package at.jku.se.smarthome.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Result of a full-day smart home simulation.
 */
@SuppressWarnings({
        "PMD.CommentRequired",
        "PMD.DataClass"
})
public class DaySimulationResult {
    private final LocalDateTime startDateTime;
    private final LocalDateTime endDateTime;
    private final List<Room> finalRooms;
    private final List<DaySimulationEvent> events;

    public DaySimulationResult(LocalDateTime startDateTime, LocalDateTime endDateTime, List<Room> finalRooms,
                               List<DaySimulationEvent> events) {
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.finalRooms = List.copyOf(finalRooms);
        this.events = List.copyOf(events);
    }

    public LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    public LocalDateTime getEndDateTime() {
        return endDateTime;
    }

    public List<Room> getFinalRooms() {
        return finalRooms;
    }

    public List<DaySimulationEvent> getEvents() {
        return events;
    }
}
