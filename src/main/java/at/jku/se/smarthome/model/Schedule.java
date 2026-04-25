package at.jku.se.smarthome.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a recurring time-based device schedule.
 */
public class Schedule {
    private final String id;
    private String name;
    private final String deviceId;
    private ScheduleActionType actionType;
    private Double targetValue;
    private LocalTime executionTime;
    private Set<DayOfWeek> recurringDays;
    private LocalDate lastExecutedOn;

    /**
     * Creates a schedule definition.
     *
     * @param id the schedule id
     * @param name the display name
     * @param deviceId the target device id
     * @param actionType the action to execute
     * @param targetValue the optional numeric target value
     * @param executionTime the time of day when the schedule becomes due
     * @param recurringDays the weekdays on which the schedule repeats
     */
    public Schedule(String id, String name, String deviceId, ScheduleActionType actionType, Double targetValue,
                    LocalTime executionTime, Set<DayOfWeek> recurringDays) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Schedule id must not be empty");
        }
        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("Device id must not be empty");
        }

        this.id = id.trim();
        this.deviceId = deviceId.trim();
        applyUpdate(name, actionType, targetValue, executionTime, recurringDays);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public ScheduleActionType getActionType() {
        return actionType;
    }

    public Double getTargetValue() {
        return targetValue;
    }

    public LocalTime getExecutionTime() {
        return executionTime;
    }

    public Set<DayOfWeek> getRecurringDays() {
        return Collections.unmodifiableSet(recurringDays);
    }

    public LocalDate getLastExecutedOn() {
        return lastExecutedOn;
    }

    /**
     * Updates all editable schedule fields.
     *
     * @param newName the new schedule name
     * @param newActionType the new action type
     * @param newTargetValue the new optional target value
     * @param newExecutionTime the new execution time
     * @param newRecurringDays the new repeating weekdays
     */
    public void update(String newName, ScheduleActionType newActionType, Double newTargetValue,
                       LocalTime newExecutionTime, Set<DayOfWeek> newRecurringDays) {
        applyUpdate(newName, newActionType, newTargetValue, newExecutionTime, newRecurringDays);
    }

    private void applyUpdate(String newName, ScheduleActionType newActionType, Double newTargetValue,
                             LocalTime newExecutionTime, Set<DayOfWeek> newRecurringDays) {
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("Schedule name must not be empty");
        }
        if (newActionType == null) {
            throw new IllegalArgumentException("Schedule action type must not be null");
        }
        if (newExecutionTime == null) {
            throw new IllegalArgumentException("Execution time must not be null");
        }
        if (newRecurringDays == null || newRecurringDays.isEmpty()) {
            throw new IllegalArgumentException("At least one recurring day is required");
        }

        this.name = newName.trim();
        this.actionType = newActionType;
        this.targetValue = newActionType == ScheduleActionType.SET_VALUE ? newTargetValue : null;
        this.executionTime = newExecutionTime.withSecond(0).withNano(0);
        this.recurringDays = EnumSet.copyOf(newRecurringDays);
    }

    /**
     * Returns whether the schedule should execute at the given time.
     *
     * @param date the current date
     * @param time the current time
     * @return {@code true} if the schedule is due
     */
    public boolean isDue(LocalDate date, LocalTime time) {
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(time, "time");

        if (!recurringDays.contains(date.getDayOfWeek())) {
            return false;
        }
        if (lastExecutedOn != null && lastExecutedOn.equals(date)) {
            return false;
        }
        return !time.withSecond(0).withNano(0).isBefore(executionTime);
    }

    /**
     * Marks the schedule as executed for the given date.
     *
     * @param date the execution date
     */
    public void markExecuted(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Execution date must not be null");
        }
        this.lastExecutedOn = date;
    }
}
