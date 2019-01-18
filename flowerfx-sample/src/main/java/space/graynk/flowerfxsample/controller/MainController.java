package space.graynk.flowerfxsample.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import space.graynk.flowerfx.interfaces.Flowable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This is the one controller to rule them all. All other FXMLs get loaded into mainPane.
 * You can surround this main pane with needed static stuff that's always on screen, like "back" button or clock.
 */
public class MainController extends BaseController implements Flowable {
    @FXML private StackPane mainPane;
    @FXML private Label timeLabel;
    @FXML private Label dateLabel;
    @FXML private Label sceneLabel;
    @FXML private Button backButton;

    public void initialize() {
        // back thread for updating clock ui
        ScheduledExecutorService scheduledExec = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread t = new Thread(runnable);
            t.setName("Datetime labels thread");
            t.setDaemon(true);
            return t;
        });

        LocalDateTime start = LocalDateTime.now();
        int delay = 59 - start.getSecond();
        LocalDateTime nextDay = LocalDate.now().plusDays(1).atStartOfDay();
        long dateDelay = start.until(nextDay, ChronoUnit.MILLIS);

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        dateLabel.setText(start.format(dateFormatter));
        timeLabel.setText(start.format(timeFormatter));

        scheduledExec.scheduleAtFixedRate(() -> Platform.runLater(() ->
                        timeLabel.setText(LocalDateTime.now().plusSeconds(1).format(timeFormatter))),
                delay, 60, TimeUnit.SECONDS);
        scheduledExec.scheduleAtFixedRate(() -> Platform.runLater(() ->
                        dateLabel.setText(LocalDate.now().format(dateFormatter))),
                dateDelay, 24*60*60*1000, TimeUnit.MILLISECONDS);
    }

    @FXML
    private void goBack() {
        swapper.fuckGoBack();
        sceneLabel.setText(swapper.getCurrentSceneName());
        if (swapper.getDepth() == 1)
            backButton.setDisable(true);
    }

    @FXML
    private void off() {
        Platform.exit();
    }

    // override addPane because we want to use sceneName and control back button
    @Override
    public void addPane(Node node) {
        String sceneName = swapper.getCurrentSceneName();
        sceneLabel.setText(sceneName);
        Instant now = Instant.now();
        mainPane.getChildren().add(node);
        System.out.println("add " + now.until(Instant.now(), ChronoUnit.MILLIS));
        backButton.setDisable(swapper.getDepth() == 0);
    }

    // removePrevPane can use default implementation

    @Override
    public Pane getMainPane() {
        return mainPane;
    }
}
