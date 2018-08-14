package space.graynk.flower;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import space.graynk.flower.interfaces.Flowable;
import space.graynk.flower.interfaces.Swappable;
import space.graynk.flower.util.NodeWrapper;
import space.graynk.flower.util.Swapper;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main class for operating the flow of switching between the panes.
 * @param <T> - Class of the main controller
 */
public class Flower<T extends Flowable> {
    private final FXMLLoader loader;
    private final T mainController;
    private final Stage primaryStage;
    private final Swapper swapper;
    // TODO: of course, straight up caching the nodes is ugly, it keeps the state after the user is done with them, so I
    // imply that either we __want__ to save the state, or the user will reset the node himself, but I will add the
    // flag to reload the node from scratch later. I guess that means caching the class or something like that
    private final Map<URL, NodeWrapper> cachedNodes = new HashMap<>();
    private final AnimatedTransition animation;
    private final Duration duration;
    private final LinkedList<Node> currentBranch = new LinkedList<>(); // keeps history of transitions between nodes
    private final ExecutorService exec = Executors.newSingleThreadExecutor(r -> { // used for background loading
       Thread t = new Thread(r);
       t.setDaemon(true);
       t.setName("FXML Loader Thread");
       return t;
    });

    public enum AnimatedTransition { NONE, SWIPE, FADE }

    public Flower(@NotNull Stage primaryStage, @NotNull URL mainFXML, @Nullable URL firstPane) {
        this(primaryStage, mainFXML, firstPane, AnimatedTransition.NONE, Duration.millis(700));
    }

    public Flower(@NotNull Stage primaryStage, @NotNull URL mainFXML, @Nullable URL firstPane,
                  @NotNull AnimatedTransition animation) {
        this(primaryStage, mainFXML, firstPane, animation, Duration.millis(400));
    }

    public Flower(@NotNull Stage primaryStage, @NotNull URL mainFXML, @Nullable URL firstPane,
                  @NotNull AnimatedTransition animation, @NotNull Duration duration) {
        this.primaryStage = primaryStage;
        this.animation = animation;
        this.duration = duration;
        loader = new FXMLLoader(mainFXML);
        // this is the abstract class that the controllers call to let main controller know that panes need to be switched
        swapper = new Swapper() {
            @Override
            // for some reason addPane is still slow on first call for the preloaded node - 70ms on RPI 3B+
            // 10ms on following calls. And I can't offload it to other thread, so the UI _will_ freeze
            public void changeView(URL fxml) {
                exec.submit(() -> {
                    Node newView = loadNode(fxml); // load node in the background. if it's already cached -- no biggie
                    if(newView == null) return;

                    Platform.runLater(() -> { // set panes and animate on UI thread
                        mainController.addPane(newView);
                        animateNodeIntoView(newView);
                    });
                });
            }

            @Override
            public void fuckGoBack() {
                if(currentBranch.size() <= 1) return;
                // may be called from anywhere (more likely on UI though), animate on UI thread
                if(Platform.isFxApplicationThread())
                    animateCurrentNodeOutOfView();
                else
                    Platform.runLater(() -> animateCurrentNodeOutOfView());
            }
        };
        loader.setControllerFactory(param -> {
            Object controller = null;
            try {
                controller = param.getConstructor().newInstance(); // load the controller as usual
            } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
                e.printStackTrace(); // TODO: don't just swallow the exception
            }
            // set the Swapper before initialize() gets called on the controller in case Context is used there
            if (controller instanceof Swappable) {
                ((Swappable) controller).setSwapper(swapper);
            }
            return controller;
        });
        try {
            Parent parent = loader.load(); // TODO: what if it's not Parent?
            primaryStage.setScene(new Scene(parent));
        } catch (IOException e) {
            e.printStackTrace(); // TODO: again, don't swallow the exception
        }
        mainController = loader.getController();
        if(firstPane != null) swapper.changeView(firstPane); // set the first pane
    }

    private void animateNodeIntoView(Node newView) {
        if(currentBranch.size() > 0) {
            switch (animation) { // swipe or fade previous node from the screen
                case SWIPE:
                    Node last = currentBranch.peekLast();
                    transitTranslate(last,
                            last.getLayoutX(), last.getLayoutX() -  last.getLayoutBounds().getWidth(),
                            event -> mainController.removePrevPane()).play();
                    break;
                case FADE:
                    transitFade(currentBranch.peekLast(), 1, 0,
                            event -> mainController.removePrevPane()).play();
                    break;
            }
        }
        // fade new node into view
        transitFade(newView, 0, 1, event -> currentBranch.add(newView)).play();
    }

    private void animateCurrentNodeOutOfView() {
        // remove the current node from the branch and fade it away
        transitFade(currentBranch.pollLast(), 1, 0,
                event -> mainController.removePrevPane()).play();
        Node prev = currentBranch.peekLast();
        mainController.addPane(prev); // get the previous node from history and add it to pane
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
    }

    private Node loadNode(URL fxml) {
        // if the node is cached -- return it
        NodeWrapper wrapper = cachedNodes.get(fxml);
        if (wrapper != null) return wrapper.getNode();
        // else load it and cache it
        loader.setLocation(fxml);
        loader.setRoot(null);
        loader.setController(null);
        try {
            Node node = loader.load();
            cachedNodes.put(fxml, new NodeWrapper(node, loader.getController()));
            return node;
        } catch (IOException e) {
            e.printStackTrace(); // TODO: god damn it
        }
        return null;
    }

    /**
     * Preload the following FXMLs in the specified order. This method should be called on startup of the application
     */
    public void preload(List<URL> nodes) {
        // load and cache each node from the list
        // I assume this is called on startup, so it doesn't matter which thread I use since there's no UI
        // and you would probably expect this method to block anyway
        for(URL node : nodes) loadNode(node);
    }

    /**
     * @return T Main controller
     */
    public T getMainController() {
        return mainController;
    }

    /**
     * @return Stage
     */
    public Stage getPrimaryStage() {
        return primaryStage;
    }

    /**
     * @param fxml - URL for FXML file describing the node
     * @return Node if it has been cached, null otherwise
     */
    public Node getCachedNode(URL fxml) {
        NodeWrapper wrapper = cachedNodes.get(fxml);
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
