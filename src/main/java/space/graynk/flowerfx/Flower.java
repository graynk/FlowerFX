package space.graynk.flowerfx;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import space.graynk.flowerfx.interfaces.Flowable;
import space.graynk.flowerfx.interfaces.Swappable;
import space.graynk.flowerfx.util.NodeWrapper;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main class for operating the flow of switching between the panes.
 * @param <T> - Class of the main controller
 */
public class Flower<T extends Flowable> {
    private static final Logger logger = Logger.getLogger(Flower.class.getName());
    private final FXMLLoader loader;
    private final T mainController;
    private final Swapper swapper;
    // of course, straight up caching the nodes is ugly, it keeps the state after the user is done with them, so I
    // imply that either we __want__ to save the state, or the user will reset the node himself, using reinitialize()
    private final Map<URL, NodeWrapper<? extends Swappable>> cachedNodes = new HashMap<>();
    private final AnimatedTransition animation;
    private final Duration duration;
    private final LinkedList<NodeWrapper<? extends Swappable>> currentBranch = new LinkedList<>(); // keeps history of transitions between nodes
    private final ExecutorService exec = Executors.newSingleThreadExecutor(r -> { // used for background loading
       Thread t = new Thread(r);
       t.setDaemon(true);
       t.setName("FXML Loader Thread");
       return t;
    });

    public enum AnimatedTransition { NONE, SWIPE, FADE }

    public Flower(@NotNull Stage primaryStage, @NotNull URL mainFXML, @Nullable URL firstPane) {
        this(primaryStage, mainFXML, firstPane, AnimatedTransition.NONE, Duration.millis(400), null);
    }

    public Flower(@NotNull Stage primaryStage, @NotNull URL mainFXML, @Nullable URL firstPane,
                  @NotNull AnimatedTransition animation) {
        this(primaryStage, mainFXML, firstPane, animation, Duration.millis(400), null);
    }

    /**
     *
     * @param primaryStage - Main stage that houses switchable panes
     * @param mainFXML - Main FXML for said stage
     * @param firstPane - First pane to load into the stage
     * @param animation - Type of animation: none, swipe left/right or fade in/out.
     * @param duration - Time the animation takes to switch scenes
     * @param sceneName - Name of the current (main) scene
     */
    public Flower(@NotNull Stage primaryStage, @NotNull URL mainFXML, @Nullable URL firstPane,
                  @NotNull AnimatedTransition animation, @NotNull Duration duration, @Nullable String sceneName) {
        this.animation = animation;
        this.duration = duration;
        loader = new FXMLLoader(mainFXML);
        // this is the abstract class that the controllers call to let main controller know that panes need to be switched
        swapper = new Swapper() {
            @Override
            public void changeView(URL fxml, boolean reinitialize) {
                changeView(fxml, reinitialize, null);
            }

            @Override
            // for some reason addPane still slow on first call for the preloaded node - 70ms on RPI 3B+
            // 10ms on following calls. And I can't offload it to other thread, so the UI _will_ freeze
            public void changeView(URL fxml, boolean reinitialize, String sceneName) {
                exec.submit(() -> {
                    Instant now = Instant.now();
                    NodeWrapper<? extends Swappable> wrapper = loadNode(fxml);
                    if (wrapper == null) return;
                    Node newView = wrapper.getNode(); // load node in the background. if it's already cached -- no biggie
                    if(newView == null) return;
                    wrapper.setName(sceneName);
                    swapper.setSceneName(sceneName);
                    System.out.println(now.until(Instant.now(), ChronoUnit.MILLIS));
                    // may be called from anywhere (more likely on UI though), set panes and animate on UI thread
                    Platform.runLater(() -> animateNodeIntoView(wrapper, reinitialize));
                });
            }

            @Override
            public void fuckGoBack() {
                if(currentBranch.size() <= 1) return;
                // may be called from anywhere (more likely on UI though), animate on UI thread
                NodeWrapper<? extends Swappable> prevWrapper = currentBranch.get(currentBranch.size() - 2); // get previous node from history
                if(Platform.isFxApplicationThread())
                    animateCurrentNodeOutOfView(prevWrapper);
                else
                    Platform.runLater(() -> animateCurrentNodeOutOfView(prevWrapper));
            }

            @Override
            public void backToSpecific(URL fxml) {
                NodeWrapper<? extends Swappable> wrapper = loadNode(fxml);
                if(currentBranch.contains(wrapper)) {
                    animateCurrentNodeOutOfView(wrapper);
                    for(int i = currentBranch.indexOf(wrapper) + 1; i < currentBranch.size(); i++) {
                        currentBranch.removeLast();
                    }
                }
            }

            @Override
            public int getDepth() { return currentBranch.size(); }
        };
        swapper.setPrimaryStage(primaryStage);
        loader.setControllerFactory(param -> {
            Object controller;
            try {
                controller = param.getConstructor().newInstance(); // load the controller as usual
            } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
                logger.log(Level.SEVERE, "Failed to instantiate main controller", e);
                return null;
            }
            // set the Swapper before initialize() gets called on the main controller in case Context is used there
            if (controller instanceof Swappable) {
                ((Swappable) controller).setSwapper(swapper);
            } else {
                throw new ClassCastException("Controller is not an instance of Swappable");
            }
            return controller;
        });
        try {
            Parent parent = loader.load();
            primaryStage.setScene(new Scene(parent));
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to load main fxml - not Parent: " + mainFXML, e);
            mainController = null;
            return;
        }
        mainController = loader.getController();
        mainController.reinitialize();
        if(firstPane != null) swapper.changeView(firstPane, false, sceneName); // set the first pane
    }

    private void animateNodeIntoView(NodeWrapper<? extends Swappable> wrapper, boolean reinitialize) {
        if(reinitialize) wrapper.getController().reinitialize();
        Node newView = wrapper.getNode();
        mainController.addPane(newView);
        //TODO: how can I avoid starting animation before node had the time to render after loading?
        if(currentBranch.size() > 0) {
            Node last = currentBranch.peekLast().getNode();
            EventHandler<ActionEvent> onFinished = null;
            if (animation != AnimatedTransition.NONE) { // set cache hint for animation only
                newView.setCacheHint(CacheHint.SPEED);
                last.setCacheHint(CacheHint.SPEED);
                onFinished = event -> {
                    mainController.removePrevPane(last);
                    last.setCacheHint(CacheHint.QUALITY);
                };
            }
            switch (animation) { // swipe or fade previous node from the screen
                case SWIPE:
                    transitTranslate(last, last.getLayoutX(),
                            last.getLayoutX() -  last.getLayoutBounds().getWidth(), onFinished).play();
                    break;
                case FADE:
                    transitFade(last, 1, 0, onFinished).play();
                    break;
                case NONE:
                    mainController.removePrevPane(last);
                    break;
            }
        }
        // fade new node into view
        newView.setTranslateX(newView.getLayoutX());
        if (animation == AnimatedTransition.NONE) currentBranch.add(wrapper);
        else transitFade(newView, 0, 1, event -> {
            currentBranch.add(wrapper);
            newView.setCacheHint(CacheHint.QUALITY);
        }).play();
    }

    private void animateCurrentNodeOutOfView(NodeWrapper<? extends Swappable> prevWrapper) {
        Node prev = prevWrapper.getNode();
        swapper.setSceneName(prevWrapper.getName()); // change the name of the scene to previous one
        mainController.addPane(prev); // add previous node to pane
        switch (animation) { // then fade or swipe it in
            case FADE:
                transitFade(prev, 0, 1, null).play();
                break;
            case SWIPE:
                transitTranslate(prev,
                        prev.getLayoutX() - prev.getLayoutBounds().getWidth(),
                        prev.getLayoutX(),
                        null).play();
                break;
        }
        // remove the current node from the branch and fade it away
        NodeWrapper<? extends Swappable> lastWrapper = currentBranch.pollLast();
        if (lastWrapper == null) return;
        Node last = lastWrapper.getNode();
        if (animation == AnimatedTransition.NONE) {
            mainController.removePrevPane(last);
        }
        else {
            transitFade(last, 1, 0,
                    event -> mainController.removePrevPane(last)).play();
        }
    }

    private NodeWrapper<? extends Swappable> loadNode(URL fxml) {
        // if the node is cached -- return it
        NodeWrapper<? extends Swappable> wrapper = cachedNodes.get(fxml);
        if (wrapper != null) return wrapper;
        // else load it and cache it
        loader.setLocation(fxml);
        loader.setRoot(null);
        loader.setController(null);
        try {
            Node node = loader.load();
            wrapper = new NodeWrapper<>(node, loader.getController());
            cachedNodes.put(fxml, wrapper);
            return wrapper;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to load fxml " + fxml, e);
        }
        return null;
    }

    /**
     * Preload the following FXMLs in the specified order. This method should be called on startup of the application
     * @param nodes List of FXML URLs
     */
    public void preload(List<URL> nodes) throws ExecutionException, InterruptedException {
        // load and cache each node from the list
        // FXMLLoader is shared, so let's use the same singlethreadexecutor for loading to avoid race conditions
        exec.submit(() -> {
            for(URL node : nodes) loadNode(node);
        }).get();
    }

    /**
     * Just redirects the call to {@link Swapper}
     * @param key
     * @param value
     */
    public void putIntoContext(String key, Object value) {
        swapper.putIntoContext(key, value);
    }

    /**
     * Just redirects the call to {@link Swapper}
     * @param key
     */
    public Object getFromContext(String key) { return swapper.getFromContext(key); }

    /**
     * Just redirects the call to {@link Swapper}
     * @param key
     */
    public Object removeFromContext(String key) {
        return swapper.removeFromContext(key);
    }

    /**
     * @return T Main controller
     */
    public T getMainController() {
        return mainController;
    }

    /**
     * @return Main stage in which everything gets loaded into
     */
    public Stage getPrimaryStage() {
        return swapper.getPrimaryStage();
    }

    /**
     * @param fxml - URL for FXML file describing the node
     * @return Node if it has been cached, null otherwise
     */
    public Node getCachedNode(URL fxml) {
        NodeWrapper<? extends Swappable> wrapper = cachedNodes.get(fxml);
        return wrapper == null ? null : wrapper.getNode(); }

    /**
     * @param fxml - URL for FXML file describing the node
     * @return Controller instance if it has been cached, null otherwise
     */
    public Object getCachedController(URL fxml) { return cachedNodes.get(fxml).getController(); }

    private FadeTransition transitFade(Node node, double from, double to,
                                       EventHandler<ActionEvent> onFinished) {
        FadeTransition transit = new FadeTransition(duration, node);
        transit.setFromValue(from);
        transit.setToValue(to);
        transit.setCycleCount(1);
        transit.setOnFinished(onFinished);
        return transit;
    }

    private TranslateTransition transitTranslate(Node node, double from, double to,
                                                 EventHandler<ActionEvent> onFinished) {
        TranslateTransition transit = new TranslateTransition(duration, node);
        transit.setFromX(from);
        transit.setToX(to);
        transit.setCycleCount(1);
        transit.setOnFinished(onFinished);
        return transit;
    }

    private ScaleTransition transitScale(Node node, double from, double to) {
        ScaleTransition transit = new ScaleTransition(duration, node);
        transit.setFromX(from);
        transit.setFromY(from);
        transit.setToX(to);
        transit.setToY(to);
        transit.setCycleCount(1);
        return transit;
    }

    private ParallelTransition transitParallel(Node node, double from, double to,
                                               EventHandler<ActionEvent> onFinished) {
        return new ParallelTransition(node,
                transitFade(node, from, to, onFinished),
                transitScale(node, from, to));
    }
}
