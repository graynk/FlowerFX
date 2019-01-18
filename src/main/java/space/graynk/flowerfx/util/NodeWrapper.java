package space.graynk.flowerfx.util;

import javafx.scene.CacheHint;
import javafx.scene.Node;
import space.graynk.flowerfx.interfaces.Swappable;

/**
 * Wrapper class for caching loaded nodes along with their controllers
 */
public class NodeWrapper<T extends Swappable> {
    private String name;
    private final Node node;
    private final T controller;

    public NodeWrapper(Node node, T controller) {
        this.node = node;
        this.controller = controller;
        node.setCache(true);
        node.setCacheHint(CacheHint.QUALITY);
    }

    public Node getNode() {
        return node;
    }

    public T getController() {
        return controller;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
