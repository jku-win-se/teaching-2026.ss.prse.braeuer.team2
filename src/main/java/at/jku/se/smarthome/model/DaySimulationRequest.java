package at.jku.se.smarthome.model;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Input data for a full-day smart home simulation.
 */
@SuppressWarnings({
        "PMD.CommentRequired",
        "PMD.DataClass",
        "PMD.UseObjectForClearerAPI"
})
public class DaySimulationRequest {
    private final LocalDateTime startDateTime;
    private final Map<String, Double> initialSensorValues;
    private final Set<String> activeRuleIds;

    public DaySimulationRequest(LocalDateTime startDateTime, Map<String, Double> initialSensorValues,
                                Set<String> activeRuleIds) {
        if (startDateTime == null) {
            throw new IllegalArgumentException("Simulation start time must not be null");
        }
        if (initialSensorValues == null) {
            throw new IllegalArgumentException("Initial sensor values must not be null");
        }
        if (activeRuleIds == null) {
            throw new IllegalArgumentException("Active rule ids must not be null");
        }

        this.startDateTime = startDateTime.withSecond(0).withNano(0);
        this.initialSensorValues = new HashMap<>(initialSensorValues);
        this.activeRuleIds = new HashSet<>(activeRuleIds);
    }

    public LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    public Map<String, Double> getInitialSensorValues() {
        return Collections.unmodifiableMap(initialSensorValues);
    }

    public Set<String> getActiveRuleIds() {
        return Collections.unmodifiableSet(activeRuleIds);
    }
}
