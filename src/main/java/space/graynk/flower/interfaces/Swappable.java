package space.graynk.flower.interfaces;

import space.graynk.flower.util.Swapper;

/**
 * It would make way more sense as an abstract class, but controllers may extend one another, and
 * there is no multiple inheritance in Java.
 */
public interface Swappable {
    void setSwapper(Swapper swapper);
    void reinitialize();
}
