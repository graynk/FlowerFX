package space.graynk.flowerfx;

import javafx.stage.Stage;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * The, um, listener, that controllers call when they want to switch the panes or pass the data between them
 */
public abstract class Swapper {
    private Stage stage;
    private String sceneName;
    // this is used to pass the data into another scene
    private final static Map<String, Object> context = new HashMap<>();

    /**
     * Switches current scene to the new one, specified by the fxml URL.
     * @param fxml - path to .fxml file to load. After the first load it gets cached, but path is still needed
     * @param reinitialize - sets whether cached pane needs to be reinitialized
     * @param sceneName - name of the scene (for example to be displayed at the top of the screen). May be null if you don't need it
     */
    public void changeView(URL fxml, boolean reinitialize, String sceneName) {}
    public void changeView(URL fxml, boolean reinitialize) {}

    /**
     * Return to the previous scene
     */
    public void fuckGoBack() {}

    /**
     * Return to specific scene, specified by fxml URL. If specified scene is not in current scene graph then do nothing.
     * @param fxml
     */
    public void backToSpecific(URL fxml) {}

    /**
     * Returns depth of current scene graph.
     * @return
     */
    public int getDepth() {return 0;}

    /**
     * Save main stage
     * @param stage
     */
    void setPrimaryStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * Save scene name
     * @param sceneName
     */
    void setSceneName(String sceneName) {
        this.sceneName = sceneName;
    }

    /**
     * Returns main stage
     * @return
     */
    public Stage getPrimaryStage() { return stage; }

    /**
     * Returns name of the current scene
     * @return
     */
    public String getCurrentSceneName() { return sceneName; }

    /**
     * Save some object that needs to get passed to the next scene
     * @param key
     * @param value
     */
    public void putIntoContext(String key, Object value) {
        Swapper.context.put(key, value);
    }

    /**
     * Get saved object without removing it, keeping it for future use
     * @param key
     * @return
     */
    public Object getFromContext(String key) { return Swapper.context.get(key); }

    /**
     * Get saved object and remove it
     * @param key
     * @return
     */
    public Object removeFromContext(String key) {
        return Swapper.context.remove(key);
    }
}
