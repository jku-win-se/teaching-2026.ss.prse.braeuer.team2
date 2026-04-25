package at.jku.se.smarthome.model;

import java.time.Instant;

/**
 * Immutable activity log entry for a device state change.
 */
@SuppressWarnings("PMD.DataClass")
public class ActivityLogEntry {
    /**
     * Timestamp of the state change.
     */
    private final Instant timestamp;
    /**
     * Identifier of the affected device.
     */
    private final String deviceId;
    /**
     * Display name of the affected device.
     */
    private final String deviceName;
    /**
     * Category of the actor that triggered the state change.
     */
    private final ActivityActorType actorType;
    /**
     * Concrete actor name, e.g. user email or rule name.
     */
    private final String actorName;
    /**
     * Device state before the change.
     */
    private final String previousState;
    /**
     * Device state after the change.
     */
    private final String newState;

    /**
     * Creates a new activity log entry.
     *
     * @param timestamp the time of the state change
     * @param deviceId the affected device id
     * @param deviceName the affected device name
     * @param actorType the actor category
     * @param actorName the concrete user email or rule name
     * @param previousState the state before the change
     * @param newState the state after the change
     */
    public ActivityLogEntry(Instant timestamp, String deviceId, String deviceName,
                            ActivityActorType actorType, String actorName,
                            String previousState, String newState) {
        this.timestamp = requireTimestamp(timestamp);
        this.deviceId = requireNonBlank(deviceId, "Device id must not be blank");
        this.deviceName = requireNonBlank(deviceName, "Device name must not be blank");
        this.actorType = requireActorType(actorType);
        this.actorName = requireNonBlank(actorName, "Actor name must not be blank");
        this.previousState = requireNonBlank(previousState, "Previous state must not be blank");
        this.newState = requireNonBlank(newState, "New state must not be blank");
    }

    /**
     * Returns the timestamp of the state change.
     *
     * @return the timestamp of the state change
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Returns the affected device id.
     *
     * @return the affected device id
     */
    public String getDeviceId() {
        return deviceId;
    }

    /**
     * Returns the affected device name.
     *
     * @return the affected device name
     */
    public String getDeviceName() {
        return deviceName;
    }

    /**
     * Returns the actor category.
     *
     * @return the actor category
     */
    public ActivityActorType getActorType() {
        return actorType;
    }

    /**
     * Returns the concrete actor name.
     *
     * @return the actor name
     */
    public String getActorName() {
        return actorName;
    }

    /**
     * Returns the device state before the change.
     *
     * @return the previous device state
     */
    public String getPreviousState() {
        return previousState;
    }

    /**
     * Returns the device state after the change.
     *
     * @return the new device state
     */
    public String getNewState() {
        return newState;
    }

    private static Instant requireTimestamp(Instant candidate) {
        if (candidate == null) {
            throw new IllegalArgumentException("Timestamp must not be null");
        }
        return candidate;
    }

    private static ActivityActorType requireActorType(ActivityActorType candidate) {
        if (candidate == null) {
            throw new IllegalArgumentException("Actor type must not be null");
        }
        return candidate;
    }

    private static String requireNonBlank(String candidate, String message) {
        if (candidate == null || candidate.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return candidate.trim();
    }
}
