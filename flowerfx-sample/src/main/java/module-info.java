module space.graynk.flowerfxsample {
    requires space.graynk.flowerfx;
    requires javafx.graphics;
    requires javafx.fxml;
    requires javafx.controls;
    exports space.graynk.flowerfxsample.controller to space.graynk.flowerfx;
    exports space.graynk.flowerfxsample to javafx.graphics;
    opens space.graynk.flowerfxsample to javafx.fxml;
    opens space.graynk.flowerfxsample.controller to javafx.fxml;
}