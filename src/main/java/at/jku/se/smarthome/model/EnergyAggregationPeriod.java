package at.jku.se.smarthome.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Supported aggregation windows for the energy dashboard.
 */
public enum EnergyAggregationPeriod {
    DAY("Today"),
    WEEK("This week");

    /**
     * Human-readable label used by the JavaFX choice box.
     */
    private final String displayName;

    EnergyAggregationPeriod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Resolves the inclusive start instant for this aggregation period.
     *
     * @param referenceInstant the instant that marks the end of the report
     * @param zoneId the zone used for calendar boundaries
     * @return the inclusive start instant
     */
    public Instant resolveStart(Instant referenceInstant, ZoneId zoneId) {
        LocalDate referenceDate = LocalDate.ofInstant(referenceInstant, zoneId);
        LocalDate startDate = switch (this) {
            case DAY -> referenceDate;
            case WEEK -> referenceDate.minusDays(referenceDate.getDayOfWeek().getValue() - 1L);
        };
        return startDate.atStartOfDay(zoneId).toInstant();
    }
}
