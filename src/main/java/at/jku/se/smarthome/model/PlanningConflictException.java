package at.jku.se.smarthome.model;

/**
 * Signals that a rule or schedule would create a contradictory planning configuration.
 */
public class PlanningConflictException extends IllegalStateException {
    /**
     * Serialization version for exception transport.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Creates a planning conflict exception.
     *
     * @param message the user-facing conflict description
     */
    public PlanningConflictException(String message) {
        super(message);
    }
}
