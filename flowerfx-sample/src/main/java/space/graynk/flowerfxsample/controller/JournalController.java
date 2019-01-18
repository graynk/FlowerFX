package space.graynk.flowerfxsample.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.XYChart;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadLocalRandom;

public class JournalController extends BaseController {
    @FXML private AreaChart<Number, Number> wbcChart;
    @FXML private AreaChart<Number, Number> rbcChart;
    @FXML private AreaChart<Number, Number> pltChart;

    public void initialize() {
        ExecutorService exec = (ExecutorService) swapper.getFromContext("exec");
        exec.submit(() -> { // load data on worker thread
            XYChart.Series<Number, Number> wbcSeries = new XYChart.Series<>();
            XYChart.Series<Number, Number> rbcSeries = new XYChart.Series<>();
            XYChart.Series<Number, Number> pltSeries = new XYChart.Series<>();
            for (int i = 0; i < 100; i++) { // get from "db"
                wbcSeries.getData().add(new XYChart.Data<>(i, ThreadLocalRandom.current().nextInt(0, 100)));
                rbcSeries.getData().add(new XYChart.Data<>(i, ThreadLocalRandom.current().nextInt(0, 100)));
                pltSeries.getData().add(new XYChart.Data<>(i, ThreadLocalRandom.current().nextInt(0, 100)));
            }
            Platform.runLater(() -> { // set on ui thread
                wbcChart.getData().setAll(wbcSeries);
                rbcChart.getData().setAll(rbcSeries);
                pltChart.getData().setAll(pltSeries);
            });
        });
    }
}
