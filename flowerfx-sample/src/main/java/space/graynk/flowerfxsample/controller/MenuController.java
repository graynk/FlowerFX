package space.graynk.flowerfxsample.controller;

import javafx.fxml.FXML;

public class MenuController extends BaseController {

    @FXML
    private void openFirst() {
        swapper.changeView(MenuController.class.getResource("journalpage.fxml"), false, "Journal");
    }

    @FXML
    private void openSecond() {

    }

    @FXML
    private void openThird() {
        swapper.changeView(MenuController.class.getResource("aggregationjournal.fxml"), false, "Aggregation");
    }
}
