package space.graynk.flower.util;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * The, um, listener, that controllers call when they want to switch the panes or pass the data between them
 */
public abstract class Swapper {
    // this is used to pass the data into another scene
    private final static Map<String, Object> context = new HashMap<>();

    public void changeView(URL fxml) {}
    public void fuckGoBack() {}

    public void addToContext(String key, Object value) {
        Swapper.context.put(key, value);
    }

    public Object getFromContext(String key) { return Swapper.context.get(key); }

    // sorry
    public Object takeOutOfContext(String key) {
        return Swapper.context.remove(key);
    }
}
