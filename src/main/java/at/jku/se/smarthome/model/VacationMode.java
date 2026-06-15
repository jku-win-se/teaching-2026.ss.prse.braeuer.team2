package at.jku.se.smarthome.model;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Defines a temporary vacation mode that lets selected schedules override normal schedules.
 */
@SuppressWarnings({"PMD.DataClass", "PMD.CommentRequired"})
public class VacationMode {
    private final Set<String> scheduleIds;
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
        this(Set.of(scheduleId), startAt, endAt, enabled);
    }

    /**
     * Creates a vacation mode configuration.
     *
     * @param scheduleIds the selected schedule ids
     * @param startAt the start timestamp
     * @param endAt the end timestamp
     * @param enabled whether the mode is enabled
     */
    public VacationMode(Set<String> scheduleIds, LocalDateTime startAt, LocalDateTime endAt, boolean enabled) {
        this.scheduleIds = validateScheduleIds(scheduleIds);
        this.startAt = normalizeDateTime(startAt, "Vacation start must not be null");
        this.endAt = normalizeDateTime(endAt, "Vacation end must not be null");
        validateDateRange(this.startAt, this.endAt);
        this.enabled = enabled;
    }

    private static Set<String> validateScheduleIds(Set<String> scheduleIds) {
        validateScheduleIdsProvided(scheduleIds);
        Set<String> normalizedScheduleIds = new LinkedHashSet<>();
        for (String scheduleId : scheduleIds) {
            normalizedScheduleIds.add(normalizeScheduleId(scheduleId));
        }
        return Collections.unmodifiableSet(normalizedScheduleIds);
    }

    private static void validateScheduleIdsProvided(Set<String> scheduleIds) {
        if (scheduleIds == null || scheduleIds.isEmpty()) {
            throw new IllegalArgumentException("Vacation schedules must not be empty");
        }
    }

    private static String normalizeScheduleId(String scheduleId) {
        if (scheduleId == null) {
            throw new IllegalArgumentException("Vacation schedule must not be empty");
        }
        String normalizedScheduleId = scheduleId.trim();
        if (normalizedScheduleId.isEmpty()) {
            throw new IllegalArgumentException("Vacation schedule must not be empty");
        }
        return normalizedScheduleId;
    }

    private static LocalDateTime normalizeDateTime(LocalDateTime dateTime, String nullMessage) {
        if (dateTime == null) {
            throw new IllegalArgumentException(nullMessage);
        }
        return dateTime.withSecond(0).withNano(0);
    }

    private static void validateDateRange(LocalDateTime startAt, LocalDateTime endAt) {
        if (!endAt.isAfter(startAt)) {
            throw new IllegalArgumentException("Vacation end must be after vacation start");
        }
    }

    public String getScheduleId() {
        return scheduleIds.iterator().next();
    }

    public Set<String> getScheduleIds() {
        return scheduleIds;
    }

    public boolean containsSchedule(String scheduleId) {
        return scheduleId != null && scheduleIds.contains(scheduleId.trim());
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
