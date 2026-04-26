package at.jku.se.smarthome.model;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Trigger definition of an automation rule.
 */
@SuppressWarnings({
        "PMD.CommentRequired",
        "PMD.ShortVariable",
        "PMD.DataClass"
})
public class RuleTrigger {
    private final RuleTriggerType triggerType;
    private final String sourceDeviceId;
    private final Double expectedValue;
    private final ThresholdOperator thresholdOperator;
    private final LocalTime triggerTime;
    private LocalDate lastTriggeredOn;

    /**
     * Creates a trigger definition.
     *
     * @param triggerType the trigger type
     * @param sourceDeviceId the source device id
     * @param expectedValue the expected device state value
     */
    public RuleTrigger(RuleTriggerType triggerType, String sourceDeviceId, Double expectedValue) {
        this(triggerType, sourceDeviceId, expectedValue, null, null, null);
    }

    /**
     * Creates a trigger definition.
     *
     * @param triggerType the trigger type
     * @param sourceDeviceId the source device id
     * @param expectedValue the expected device state value
     * @param thresholdOperator the threshold comparison operator
     * @param triggerTime the time when a time-based trigger becomes due
     * @param lastTriggeredOn the last date on which this trigger ran
     */
    public RuleTrigger(RuleTriggerType triggerType, String sourceDeviceId, Double expectedValue,
                       ThresholdOperator thresholdOperator, LocalTime triggerTime, LocalDate lastTriggeredOn) {
        if (triggerType == null) {
            throw new IllegalArgumentException("Rule trigger type must not be null");
        }
        if (triggerType != RuleTriggerType.TIME && (sourceDeviceId == null || sourceDeviceId.isBlank())) {
            throw new IllegalArgumentException("Source device id must not be empty");
        }

        this.triggerType = triggerType;
        this.sourceDeviceId = sourceDeviceId == null ? null : sourceDeviceId.trim();
        this.expectedValue = expectedValue;
        this.thresholdOperator = thresholdOperator;
        this.triggerTime = triggerTime == null ? null : triggerTime.withSecond(0).withNano(0);
        this.lastTriggeredOn = lastTriggeredOn;
    }

    public RuleTriggerType getTriggerType() {
        return triggerType;
    }

    public String getSourceDeviceId() {
        return sourceDeviceId;
    }

    public Double getExpectedValue() {
        return expectedValue;
    }

    public ThresholdOperator getThresholdOperator() {
        return thresholdOperator;
    }

    public LocalTime getTriggerTime() {
        return triggerTime;
    }

    public LocalDate getLastTriggeredOn() {
        return lastTriggeredOn;
    }

    /**
     * Returns whether this time trigger is due at the given date and time.
     *
     * @param date the current date
     * @param time the current time
     * @return {@code true} if the trigger is due
     */
    public boolean isDue(LocalDate date, LocalTime time) {
        if (triggerType != RuleTriggerType.TIME || triggerTime == null) {
            return false;
        }
        boolean alreadyTriggeredToday = lastTriggeredOn != null && lastTriggeredOn.equals(date);
        return !alreadyTriggeredToday && !time.withSecond(0).withNano(0).isBefore(triggerTime);
    }

    /**
     * Marks this trigger as executed for the given date.
     *
     * @param date the execution date
     */
    public void markTriggered(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Execution date must not be null");
        }
        this.lastTriggeredOn = date;
    }
}
