package at.jku.se.smarthome.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Immutable energy dashboard aggregate for one reporting window.
 */
@SuppressWarnings("PMD.DataClass")
public final class EnergyDashboard {
    /**
     * Selected reporting window.
     */
    private final EnergyAggregationPeriod aggregationPeriod;
    /**
     * Inclusive start of the reporting window.
     */
    private final Instant startInclusive;
    /**
     * Exclusive end of the reporting window.
     */
    private final Instant endExclusive;
    /**
     * Room-level consumption breakdown.
     */
    private final List<EnergyRoomConsumption> roomConsumptions;
    /**
     * Household-level consumption sum in kWh.
     */
    private final double totalConsumptionKiloWattHours;

    /**
     * Creates an immutable dashboard aggregate.
     *
     * @param aggregationPeriod the selected aggregation period
     * @param startInclusive the inclusive start instant
     * @param endExclusive the exclusive end instant
     * @param roomConsumptions the per-room energy breakdown
     */
    public EnergyDashboard(EnergyAggregationPeriod aggregationPeriod, Instant startInclusive,
                           Instant endExclusive, List<EnergyRoomConsumption> roomConsumptions) {
        this.aggregationPeriod = requirePeriod(aggregationPeriod);
        this.startInclusive = requireInstant(startInclusive, "Start instant must not be null");
        this.endExclusive = requireInstant(endExclusive, "End instant must not be null");
        this.roomConsumptions = List.copyOf(requireRooms(roomConsumptions));
        this.totalConsumptionKiloWattHours = sumRoomConsumption(this.roomConsumptions);
    }

    public EnergyAggregationPeriod getAggregationPeriod() {
        return aggregationPeriod;
    }

    public Instant getStartInclusive() {
        return startInclusive;
    }

    public Instant getEndExclusive() {
        return endExclusive;
    }

    public List<EnergyRoomConsumption> getRoomConsumptions() {
        return roomConsumptions;
    }

    public double getTotalConsumptionKiloWattHours() {
        return totalConsumptionKiloWattHours;
    }

    /**
     * Returns all device consumptions flattened across all rooms.
     *
     * @return all visible device consumptions
     */
    public List<EnergyDeviceConsumption> getDeviceConsumptions() {
        List<EnergyDeviceConsumption> devices = new ArrayList<>();
        for (EnergyRoomConsumption roomConsumption : roomConsumptions) {
            devices.addAll(roomConsumption.getDeviceConsumptions());
        }
        return List.copyOf(devices);
    }

    private static EnergyAggregationPeriod requirePeriod(EnergyAggregationPeriod candidate) {
        if (candidate == null) {
            throw new IllegalArgumentException("Aggregation period must not be null");
        }
        return candidate;
    }

    private static Instant requireInstant(Instant candidate, String message) {
        if (candidate == null) {
            throw new IllegalArgumentException(message);
        }
        return candidate;
    }

    private static List<EnergyRoomConsumption> requireRooms(List<EnergyRoomConsumption> roomConsumptions) {
        if (roomConsumptions == null) {
            throw new IllegalArgumentException("Room consumptions must not be null");
        }
        return roomConsumptions;
    }

    private static double sumRoomConsumption(List<EnergyRoomConsumption> roomConsumptions) {
        double sum = 0.0;
        for (EnergyRoomConsumption roomConsumption : roomConsumptions) {
            sum += roomConsumption.getTotalConsumptionKiloWattHours();
        }
        return sum;
    }
}
