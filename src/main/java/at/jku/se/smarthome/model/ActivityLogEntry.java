package at.jku.se.smarthome.model;

import java.time.Instant;

/**
 * Immutable activity log entry for a device state change.
 */
public class ActivityLogEntry {
    private final Instant timestamp;
    private final String deviceId;
    private final String deviceName;
    private final ActivityActorType actorType;
    private final String actorName;
    private final String previousState;
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
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp must not be null");
        }
        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("Device id must not be blank");
        }
        if (deviceName == null || deviceName.isBlank()) {
            throw new IllegalArgumentException("Device name must not be blank");
        }
        if (actorType == null) {
            throw new IllegalArgumentException("Actor type must not be null");
        }
        if (actorName == null || actorName.isBlank()) {
            throw new IllegalArgumentException("Actor name must not be blank");
        }
        if (previousState == null || previousState.isBlank()) {
            throw new IllegalArgumentException("Previous state must not be blank");
        }
        if (newState == null || newState.isBlank()) {
            throw new IllegalArgumentException("New state must not be blank");
        }

        this.timestamp = timestamp;
        this.deviceId = deviceId.trim();
        this.deviceName = deviceName.trim();
        this.actorType = actorType;
        this.actorName = actorName.trim();
        this.previousState = previousState.trim();
        this.newState = newState.trim();
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public ActivityActorType getActorType() {
        return actorType;
    }

    public String getActorName() {
        return actorName;
    }

    public String getPreviousState() {
        return previousState;
    }

    public String getNewState() {
        return newState;
    }
}
