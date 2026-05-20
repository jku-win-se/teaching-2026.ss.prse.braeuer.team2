package at.jku.se.smarthome.model;

/**
 * Defines the target state of a single device inside a scene.
 */
@SuppressWarnings({
        "PMD.CommentRequired",
        "PMD.ShortVariable",
        "PMD.DataClass"
})
public class SceneDeviceState {
    private final String deviceId;
    private final double targetValue;

    /**
     * Creates a device target state for a scene.
     *
     * @param deviceId the target device id
     * @param targetValue the target device value
     */
    public SceneDeviceState(String deviceId, double targetValue) {
        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("Device id must not be empty");
        }

        this.deviceId = deviceId.trim();
        this.targetValue = targetValue;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public double getTargetValue() {
        return targetValue;
    }
}
