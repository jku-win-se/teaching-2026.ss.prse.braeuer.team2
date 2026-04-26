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
        this.timestamp = requireTimestamp(timestamp);
        this.ruleId = requireText(ruleId, "Rule id must not be empty");
        this.ruleName = requireText(ruleName, "Rule name must not be empty");
        this.successful = successful;
        this.message = requireText(message, "Notification message must not be empty");
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

    private static Instant requireTimestamp(Instant timestamp) {
        if (timestamp == null) {
            throw new IllegalArgumentException("Notification timestamp must not be null");
        }
        return timestamp;
    }

    private static String requireText(String value, String errorMessage) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(errorMessage);
        }
        return value.trim();
    }
}
