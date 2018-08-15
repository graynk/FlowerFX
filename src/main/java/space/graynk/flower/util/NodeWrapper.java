package space.graynk.flower.util;

import javafx.scene.Node;
import space.graynk.flower.interfaces.Swappable;

/**
 * Wrapper class for caching
 */
public class NodeWrapper<T extends Swappable> {
    private final Node node;
    private final T controller;

    public NodeWrapper(Node node, T controller) {
        this.node = node;
        this.controller = controller;
    }

    public Node getNode() {
        return node;
    }

    public T getController() {
        return controller;
    }
}
