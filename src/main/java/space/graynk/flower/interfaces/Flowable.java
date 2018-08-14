package space.graynk.flower.interfaces;

import javafx.scene.Node;

public interface Flowable extends Swappable {
    void setPane(Node node);
    void addPane(Node node);
    void removePrevPane();
}
