package at.jku.se.smarthome.model;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Calculates estimated energy consumption aggregates from device state changes.
 */
public final class EnergyConsumptionCalculator {
    private static final double SWITCH_WATTS = 60.0;
    private static final double DIMMER_MAX_WATTS = 80.0;
    private static final double THERMOSTAT_WATTS_PER_DEGREE = 50.0;
    private static final double SENSOR_WATTS = 2.0;
    private static final double PERCENT = 100.0;
    private static final double SECONDS_PER_HOUR = 3600.0;

    private final Clock clock;

    /**
     * Creates a calculator using the provided clock.
     *
     * @param clock the clock used to resolve the current reporting window
     */
    public EnergyConsumptionCalculator(Clock clock) {
        if (clock == null) {
            throw new IllegalArgumentException("Clock must not be null");
        }
        this.clock = clock;
    }

    /**
     * Calculates the estimated energy dashboard for the provided rooms and activity log.
     *
     * @param rooms the rooms to aggregate
     * @param activityLog the persisted device activity log
     * @param aggregationPeriod the selected aggregation period
     * @return the calculated dashboard aggregate
     */
    public EnergyDashboard calculate(List<Room> rooms, List<ActivityLogEntry> activityLog,
                                     EnergyAggregationPeriod aggregationPeriod) {
        List<Room> safeRooms = requireRooms(rooms);
        List<ActivityLogEntry> safeActivityLog = requireActivityLog(activityLog);
        EnergyAggregationPeriod safePeriod = requireAggregationPeriod(aggregationPeriod);

        Instant endExclusive = clock.instant();
        ZoneId zoneId = clock.getZone();
        Instant startInclusive = safePeriod.resolveStart(endExclusive, zoneId);

        List<EnergyRoomConsumption> roomConsumptions = new ArrayList<>();
        for (Room room : safeRooms) {
            roomConsumptions.add(calculateRoomConsumption(room, safeActivityLog, startInclusive, endExclusive));
        }

        roomConsumptions.sort(Comparator.comparing(EnergyRoomConsumption::getTotalConsumptionKiloWattHours).reversed()
                .thenComparing(EnergyRoomConsumption::getRoomName, String.CASE_INSENSITIVE_ORDER));
        return new EnergyDashboard(safePeriod, startInclusive, endExclusive, roomConsumptions);
    }

    private EnergyRoomConsumption calculateRoomConsumption(Room room, List<ActivityLogEntry> activityLog,
                                                           Instant startInclusive, Instant endExclusive) {
        List<EnergyDeviceConsumption> deviceConsumptions = new ArrayList<>();
        for (Device device : room.getDevices()) {
            double consumption = calculateDeviceConsumption(device, activityLog, startInclusive, endExclusive);
            deviceConsumptions.add(new EnergyDeviceConsumption(
                    room.getId(),
                    room.getName(),
                    device.getId(),
                    device.getName(),
                    device.getType(),
                    consumption
            ));
        }
        deviceConsumptions.sort(Comparator.comparing(EnergyDeviceConsumption::getConsumptionKiloWattHours).reversed()
                .thenComparing(EnergyDeviceConsumption::getDeviceName, String.CASE_INSENSITIVE_ORDER));
        return new EnergyRoomConsumption(room.getId(), room.getName(), deviceConsumptions);
    }

    private double calculateDeviceConsumption(Device device, List<ActivityLogEntry> activityLog,
                                              Instant startInclusive, Instant endExclusive) {
        List<ActivityLogEntry> deviceEntries = collectDeviceEntries(activityLog, device.getId(), endExclusive);
        String currentState = resolveInitialState(device, deviceEntries, startInclusive);
        Instant segmentStart = startInclusive;
        double wattHours = 0.0;

        for (ActivityLogEntry entry : deviceEntries) {
            Instant timestamp = entry.getTimestamp();
            if (timestamp.isBefore(startInclusive)) {
                continue;
            }
            wattHours += estimateWattHours(currentState, device.getType(), segmentStart, timestamp);
            currentState = entry.getNewState();
            segmentStart = timestamp;
        }

        wattHours += estimateWattHours(currentState, device.getType(), segmentStart, endExclusive);
        return wattHours / 1000.0;
    }

    private List<ActivityLogEntry> collectDeviceEntries(List<ActivityLogEntry> activityLog, String deviceId,
                                                        Instant endExclusive) {
        List<ActivityLogEntry> deviceEntries = new ArrayList<>();
        for (ActivityLogEntry entry : activityLog) {
            if (!deviceId.equals(entry.getDeviceId()) || !entry.getTimestamp().isBefore(endExclusive)) {
                continue;
            }
            deviceEntries.add(entry);
        }
        deviceEntries.sort(Comparator.comparing(ActivityLogEntry::getTimestamp));
        return deviceEntries;
    }

    private String resolveInitialState(Device device, List<ActivityLogEntry> deviceEntries, Instant startInclusive) {
        ActivityLogEntry latestBeforeStart = null;
        ActivityLogEntry firstWithinWindow = null;

        for (ActivityLogEntry entry : deviceEntries) {
            if (entry.getTimestamp().isBefore(startInclusive)) {
                latestBeforeStart = entry;
                continue;
            }
            if (firstWithinWindow == null) {
                firstWithinWindow = entry;
            }
        }

        if (latestBeforeStart != null) {
            return latestBeforeStart.getNewState();
        }
        if (firstWithinWindow != null) {
            return firstWithinWindow.getPreviousState();
        }
        return device.getStatusText();
    }

    private double estimateWattHours(String state, DeviceType deviceType,
                                     Instant startInclusive, Instant endExclusive) {
        if (!startInclusive.isBefore(endExclusive)) {
            return 0.0;
        }

        double hours = Duration.between(startInclusive, endExclusive).toSeconds() / SECONDS_PER_HOUR;
        return estimatePowerWatts(state, deviceType) * hours;
    }

    private double estimatePowerWatts(String state, DeviceType deviceType) {
        return switch (deviceType) {
            case SWITCH -> "On".equalsIgnoreCase(state) ? SWITCH_WATTS : 0.0;
            case DIMMER -> DIMMER_MAX_WATTS * parsePercentage(state) / PERCENT;
            case THERMOSTAT -> THERMOSTAT_WATTS_PER_DEGREE * parseLeadingNumber(state);
            case BLIND -> 0.0;
            case SENSOR -> SENSOR_WATTS;
        };
    }

    private double parsePercentage(String value) {
        return clamp(parseLeadingNumber(value), 0.0, PERCENT);
    }

    private double parseLeadingNumber(String value) {
        if (value == null || value.isBlank()) {
            return 0.0;
        }

        StringBuilder numberBuilder = new StringBuilder();
        String normalizedValue = value.trim().replace(',', '.');
        for (int index = 0; index < normalizedValue.length(); index++) {
            char currentCharacter = normalizedValue.charAt(index);
            if (Character.isDigit(currentCharacter) || currentCharacter == '.' || currentCharacter == '-') {
                numberBuilder.append(currentCharacter);
                continue;
            }
            if (numberBuilder.length() > 0) {
                break;
            }
        }

        if (numberBuilder.length() == 0) {
            return 0.0;
        }

        try {
            return Double.parseDouble(numberBuilder.toString());
        } catch (NumberFormatException exception) {
            return 0.0;
        }
    }

    private double clamp(double value, double minimum, double maximum) {
        if (value < minimum) {
            return minimum;
        }
        if (value > maximum) {
            return maximum;
        }
        return value;
    }

    private static List<Room> requireRooms(List<Room> rooms) {
        if (rooms == null) {
            throw new IllegalArgumentException("Rooms must not be null");
        }
        return rooms;
    }

    private static List<ActivityLogEntry> requireActivityLog(List<ActivityLogEntry> activityLog) {
        if (activityLog == null) {
            throw new IllegalArgumentException("Activity log must not be null");
        }
        return activityLog;
    }

    private static EnergyAggregationPeriod requireAggregationPeriod(EnergyAggregationPeriod aggregationPeriod) {
        if (aggregationPeriod == null) {
            throw new IllegalArgumentException("Aggregation period must not be null");
        }
        return aggregationPeriod;
    }
}
