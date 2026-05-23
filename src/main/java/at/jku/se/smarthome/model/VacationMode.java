package at.jku.se.smarthome.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Defines a temporary vacation mode that lets one schedule override normal schedules.
 */
@SuppressWarnings({"PMD.DataClass", "PMD.CommentRequired"})
public class VacationMode {
    private final String scheduleId;
    private final LocalDateTime startAt;
    private final LocalDateTime endAt;
    private final boolean enabled;

    /**
     * Creates a vacation mode configuration.
     *
     * @param scheduleId the selected schedule id
     * @param startAt the start timestamp
     * @param endAt the end timestamp
     * @param enabled whether the mode is enabled
     */
    public VacationMode(String scheduleId, LocalDateTime startAt, LocalDateTime endAt, boolean enabled) {
        if (scheduleId == null || scheduleId.isBlank()) {
            throw new IllegalArgumentException("Vacation schedule must not be empty");
        }
        if (startAt == null) {
            throw new IllegalArgumentException("Vacation start must not be null");
        }
        if (endAt == null) {
            throw new IllegalArgumentException("Vacation end must not be null");
        }
        if (!endAt.isAfter(startAt)) {
            throw new IllegalArgumentException("Vacation end must be after vacation start");
        }

        this.scheduleId = scheduleId.trim();
        this.startAt = startAt.withSecond(0).withNano(0);
        this.endAt = endAt.withSecond(0).withNano(0);
        this.enabled = enabled;
    }

    public String getScheduleId() {
        return scheduleId;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isActiveAt(LocalDateTime timestamp) {
        Objects.requireNonNull(timestamp, "timestamp");
        LocalDateTime normalizedTimestamp = timestamp.withSecond(0).withNano(0);
        return enabled && !normalizedTimestamp.isBefore(startAt) && normalizedTimestamp.isBefore(endAt);
    }

    public boolean isExpiredAt(LocalDateTime timestamp) {
        Objects.requireNonNull(timestamp, "timestamp");
        return enabled && !timestamp.withSecond(0).withNano(0).isBefore(endAt);
    }
}
