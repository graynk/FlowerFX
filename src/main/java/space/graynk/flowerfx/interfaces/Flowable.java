package space.graynk.flowerfx.interfaces;

import javafx.scene.Node;
import javafx.scene.layout.Pane;


/**
 * Main controller responsible for coordinating pane-switching should implement this interface.
 */
public interface Flowable extends Swappable {
    /**
     * Adds {@link Node} to root {@link javafx.scene.layout.Pane}.
     * @param node
     */
    default void addPane(Node node) {
        getMainPane().getChildren().add(node);
    }

    /**
     * Removes last meaningful node (which is passed as a parameter) from children of main {@link javafx.scene.layout.Pane}.
     * Depending on the logic of the application some pop-up nodes above this one may need to be removed as well.
     * @param node
     */
    default void removePrevPane(Node node) {
        getMainPane().getChildren().remove(node);
    }

    /**
     *
     * @return main {@link javafx.scene.layout.Pane} that houses all of the switchable panes.
     */
    Pane getMainPane();
}
