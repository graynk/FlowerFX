package space.graynk.flower.util;

import javafx.scene.Node;

/**
 * Wrapper class for caching
 */
public class NodeWrapper {
    private final Node node;
    private final Object controller;

    public NodeWrapper(Node node, Object controller) {
        this.node = node;
        this.controller = controller;
    }

    public Node getNode() {
        return node;
    }

    public Object getController() {
        return controller;
    }
}
