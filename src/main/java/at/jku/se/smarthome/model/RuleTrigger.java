package at.jku.se.smarthome.model;

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

    /**
     * Creates a trigger definition.
     *
     * @param triggerType the trigger type
     * @param sourceDeviceId the source device id
     * @param expectedValue the expected device state value
     */
    public RuleTrigger(RuleTriggerType triggerType, String sourceDeviceId, Double expectedValue) {
        if (triggerType == null) {
            throw new IllegalArgumentException("Rule trigger type must not be null");
        }
        if (sourceDeviceId == null || sourceDeviceId.isBlank()) {
            throw new IllegalArgumentException("Source device id must not be empty");
        }

        this.triggerType = triggerType;
        this.sourceDeviceId = sourceDeviceId.trim();
        this.expectedValue = expectedValue;
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
}
