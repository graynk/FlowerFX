package space.graynk.flowerfxsample;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.util.Duration;
import space.graynk.flowerfx.Flower;
import space.graynk.flowerfxsample.controller.MainController;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // set everything up
        Flower<MainController> flower = new Flower<>(primaryStage, Main.class.getResource("main.fxml"),
                Main.class.getResource("menu.fxml"),
                Flower.AnimatedTransition.SWIPE, Duration.millis(400), "Menu");
        List<URL> nodes = new ArrayList<>();
        nodes.add(Main.class.getResource("controller/journalpage.fxml"));
        nodes.add(Main.class.getResource("controller/aggregationjournal.fxml"));
        nodes.add(Main.class.getResource("controller/aggregationpage.fxml"));
        ExecutorService exec = Executors.newSingleThreadExecutor(runnable -> {
            Thread t = new Thread(runnable);
            t.setName("Data loader thread");
            t.setDaemon(true);
            return t;
        });
        flower.putIntoContext("exec", exec); // share single thread executor for data loading between scenes
        flower.preload(nodes); // preload specified nodes
        primaryStage.setTitle("Sample app");
        primaryStage.show();
    }
}
