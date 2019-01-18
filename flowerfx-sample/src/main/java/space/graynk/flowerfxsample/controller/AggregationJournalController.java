package space.graynk.flowerfxsample.controller;

import javafx.fxml.FXML;

public class AggregationJournalController extends BaseController {
    @FXML
    private void open() {
        swapper.changeView(AggregationJournalController.class.getResource("aggregationpage.fxml"), true, "Patient");
    }
}
