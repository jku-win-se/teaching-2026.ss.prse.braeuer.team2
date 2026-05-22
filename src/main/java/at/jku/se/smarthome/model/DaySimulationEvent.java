package at.jku.se.smarthome.model;

import java.time.LocalDateTime;

/**
 * A simulated device state change at a specific simulated time.
 */
@SuppressWarnings({
        "PMD.CommentRequired",
        "PMD.DataClass",
        "PMD.UseObjectForClearerAPI"
})
public class DaySimulationEvent {
    private final LocalDateTime timestamp;
    private final String deviceId;
    private final String deviceName;
    private final ActivityActorType actorType;
    private final String actorName;
    private final String previousState;
    private final String newState;

    public DaySimulationEvent(LocalDateTime timestamp, String deviceId, String deviceName,
                              ActivityActorType actorType, String actorName, String previousState, String newState) {
        this.timestamp = timestamp;
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.actorType = actorType;
        this.actorName = actorName;
        this.previousState = previousState;
        this.newState = newState;
    }

    public LocalDateTime getTimestamp() {
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
