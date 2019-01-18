package space.graynk.flowerfxsample.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadLocalRandom;

public class AggregationController extends BaseController {
    @FXML private LineChart<Number, Number> wbcChart;
    @FXML private LineChart<Number, Number> pltChart;
    private ExecutorService exec;

    public void initialize() {
        exec = (ExecutorService) swapper.getFromContext("exec");
    }

    @Override
    public void reinitialize() { // assume this scene need to reload latest patient every time, since user can switch between patients on this pane
        exec.submit(() -> { // load data on worker thread
            XYChart.Series<Number, Number> wbcSeries = new XYChart.Series<>();
            XYChart.Series<Number, Number> pltSeries = new XYChart.Series<>();
            for (int i = 0; i < 200; i++) { // get from "db"
                wbcSeries.getData().add(new XYChart.Data<>(i, ThreadLocalRandom.current().nextInt()));
                pltSeries.getData().add(new XYChart.Data<>(i, ThreadLocalRandom.current().nextInt()));
            }
            Platform.runLater(() -> { // add on ui thread
                wbcChart.getData().setAll(wbcSeries);
                pltChart.getData().setAll(pltSeries);
            });
        });
    }

    // in real app this would allow to switch between users sorted by some value. here for convenience I just generate new random data
    @FXML private void left() {reinitialize();}
    @FXML private void right() {reinitialize();}

    // Load specific previous scene
    @FXML private void openMenuAgain() {
        swapper.backToSpecific(AggregationController.class.getResource("/space/graynk/flowerfxsample/menu.fxml"));
    }

    // Go back as usual, but from this pane, not from main controller
    @FXML private void openJournalAgain() {
        swapper.fuckGoBack();
    }


}
