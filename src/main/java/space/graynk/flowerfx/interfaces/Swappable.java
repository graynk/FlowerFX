package space.graynk.flowerfx.interfaces;

import space.graynk.flowerfx.Swapper;

/**
 * All sub-controllers need to implement this in order for the main controller to swap them with one another.
 * It would make way more sense as an abstract class, but controllers may extend one another, and
 * there is no multiple inheritance in Java.
 */
public interface Swappable {
    /**
     * Sets {@link Swapper} that gets called when controlled {@link javafx.scene.layout.Pane} needs to get switched.
     * @param swapper
     */
    void setSwapper(Swapper swapper);

    /**
     * This method should restore controlled {@link javafx.scene.layout.Pane} to it's default state depending on the
     * logic of the application.
     * It then may be called -- again, depending on the logic of the application -- when pane gets switched if you need
     * to "reload" the pane without actually reloading fxml.
     */
    void reinitialize();
}
