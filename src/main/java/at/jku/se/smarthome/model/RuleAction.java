package at.jku.se.smarthome.model;

/**
 * Action definition of an automation rule.
 */
@SuppressWarnings({
        "PMD.CommentRequired",
        "PMD.ShortVariable",
        "PMD.DataClass"
})
public class RuleAction {
    private final RuleActionType actionType;
    private final String targetDeviceId;
    private final Double targetValue;

    /**
     * Creates an action definition.
     *
     * @param actionType the action type
     * @param targetDeviceId the target device id
     * @param targetValue the target device state value
     */
    public RuleAction(RuleActionType actionType, String targetDeviceId, Double targetValue) {
        if (actionType == null) {
            throw new IllegalArgumentException("Rule action type must not be null");
        }
        if (targetDeviceId == null || targetDeviceId.isBlank()) {
            throw new IllegalArgumentException("Target device id must not be empty");
        }

        this.actionType = actionType;
        this.targetDeviceId = targetDeviceId.trim();
        this.targetValue = targetValue;
    }

    public RuleActionType getActionType() {
        return actionType;
    }

    public String getTargetDeviceId() {
        return targetDeviceId;
    }

    public Double getTargetValue() {
        return targetValue;
    }
}
