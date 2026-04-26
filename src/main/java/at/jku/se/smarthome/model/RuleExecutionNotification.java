package at.jku.se.smarthome.model;

import java.time.Instant;

/**
 * In-app notification describing the result of an automation rule execution.
 */
@SuppressWarnings({
        "PMD.CommentRequired",
        "PMD.DataClass"
})
public class RuleExecutionNotification {
    private final Instant timestamp;
    private final String ruleId;
    private final String ruleName;
    private final boolean successful;
    private final String message;

    /**
     * Creates a notification for a rule execution result.
     *
     * @param timestamp the time at which the notification was created
     * @param ruleId the affected rule id
     * @param ruleName the affected rule name
     * @param successful whether the rule execution succeeded
     * @param message the user-facing notification text
     */
    public RuleExecutionNotification(Instant timestamp, String ruleId, String ruleName,
                                     boolean successful, String message) {
        if (timestamp == null) {
            throw new IllegalArgumentException("Notification timestamp must not be null");
        }
        if (ruleId == null || ruleId.isBlank()) {
            throw new IllegalArgumentException("Rule id must not be empty");
        }
        if (ruleName == null || ruleName.isBlank()) {
            throw new IllegalArgumentException("Rule name must not be empty");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Notification message must not be empty");
        }

        this.timestamp = timestamp;
        this.ruleId = ruleId.trim();
        this.ruleName = ruleName.trim();
        this.successful = successful;
        this.message = message.trim();
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public String getMessage() {
        return message;
    }
}
