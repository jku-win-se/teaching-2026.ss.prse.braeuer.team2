package at.jku.se.smarthome.model;

/**
 * Represents a persisted automation rule with trigger and action.
 */
@SuppressWarnings({
        "PMD.CommentRequired",
        "PMD.ShortVariable",
        "PMD.DataClass",
        "PMD.ShortClassName"
})
public class Rule {
    private final String id;
    private String name;
    private RuleTrigger trigger;
    private RuleAction action;

    /**
     * Creates an automation rule definition.
     *
     * @param id the rule id
     * @param name the display name
     * @param trigger the trigger definition
     * @param action the action definition
     */
    public Rule(String id, String name, RuleTrigger trigger, RuleAction action) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Rule id must not be empty");
        }

        this.id = id.trim();
        applyUpdate(name, trigger, action);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public RuleTrigger getTrigger() {
        return trigger;
    }

    public RuleAction getAction() {
        return action;
    }

    /**
     * Updates all editable rule fields.
     *
     * @param newName the new rule name
     * @param newTrigger the new trigger definition
     * @param newAction the new action definition
     */
    public void update(String newName, RuleTrigger newTrigger, RuleAction newAction) {
        applyUpdate(newName, newTrigger, newAction);
    }

    private void applyUpdate(String newName, RuleTrigger newTrigger, RuleAction newAction) {
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("Rule name must not be empty");
        }
        if (newTrigger == null) {
            throw new IllegalArgumentException("Rule trigger must not be null");
        }
        if (newAction == null) {
            throw new IllegalArgumentException("Rule action must not be null");
        }

        this.name = newName.trim();
        this.trigger = newTrigger;
        this.action = newAction;
    }
}
