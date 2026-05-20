package at.jku.se.smarthome.model;

import java.util.List;

/**
 * Represents a named group of device target states that can be activated together.
 */
@SuppressWarnings({
        "PMD.CommentRequired",
        "PMD.ShortVariable",
        "PMD.DataClass"
})
public class Scene {
    private final String id;
    private String name;
    private List<SceneDeviceState> deviceStates;

    /**
     * Creates a scene.
     *
     * @param id the scene id
     * @param name the scene display name
     * @param deviceStates the device target states belonging to the scene
     */
    public Scene(String id, String name, List<SceneDeviceState> deviceStates) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Scene id must not be empty");
        }

        this.id = id.trim();
        applyUpdate(name, deviceStates);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<SceneDeviceState> getDeviceStates() {
        return List.copyOf(deviceStates);
    }

    /**
     * Updates all editable scene fields.
     *
     * @param newName the new scene name
     * @param newDeviceStates the new target states
     */
    public void update(String newName, List<SceneDeviceState> newDeviceStates) {
        applyUpdate(newName, newDeviceStates);
    }

    private void applyUpdate(String newName, List<SceneDeviceState> newDeviceStates) {
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("Scene name must not be empty");
        }
        if (newDeviceStates == null || newDeviceStates.isEmpty()) {
            throw new IllegalArgumentException("Scene must contain at least one device state");
        }

        this.name = newName.trim();
        this.deviceStates = List.copyOf(newDeviceStates);
    }
}
