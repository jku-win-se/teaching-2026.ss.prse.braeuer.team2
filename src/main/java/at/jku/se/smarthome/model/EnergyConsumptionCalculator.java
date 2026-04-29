package at.jku.se.smarthome.model;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Calculates estimated energy consumption aggregates from device state changes.
 */
@SuppressWarnings({
        "PMD.GodClass",
        "PMD.TooManyMethods"
})
public final class EnergyConsumptionCalculator {
    /**
     * Estimated wattage of an active switch device.
     */
    private static final double SWITCH_WATTS = 60.0;
    /**
     * Estimated wattage of a dimmer at 100 percent brightness.
     */
    private static final double DIMMER_MAX_WATTS = 80.0;
    /**
     * Estimated thermostat wattage per configured degree Celsius.
     */
    private static final double THERMOSTAT_WATTS_PER_DEGREE = 50.0;
    /**
     * Estimated continuous wattage of a sensor.
     */
    private static final double SENSOR_WATTS = 2.0;
    /**
     * Percent base used for dimmer calculations.
     */
    private static final double PERCENT = 100.0;
    /**
     * Number of seconds in one hour.
     */
    private static final double SECONDS_PER_HOUR = 3600.0;
    /**
     * Leading numeric state parser.
     */
    private static final Pattern LEADING_NUMBER_PATTERN = Pattern.compile("^-?\\d+(\\.\\d+)?");

    /**
     * Clock used for reporting window boundaries.
     */
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
        String initialState;

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
            initialState = latestBeforeStart.getNewState();
        } else if (firstWithinWindow != null) {
            initialState = firstWithinWindow.getPreviousState();
        } else {
            initialState = device.getStatusText();
        }
        return initialState;
    }

    private double estimateWattHours(String state, DeviceType deviceType,
                                     Instant startInclusive, Instant endExclusive) {
        double wattHours = 0.0;
        if (startInclusive.isBefore(endExclusive)) {
            double hours = Duration.between(startInclusive, endExclusive).toSeconds() / SECONDS_PER_HOUR;
            wattHours = estimatePowerWatts(state, deviceType) * hours;
        }
        return wattHours;
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
        double parsedValue = 0.0;

        if (value != null && !value.isBlank()) {
            String normalizedValue = value.trim().replace(',', '.');
            Matcher matcher = LEADING_NUMBER_PATTERN.matcher(normalizedValue);
            if (matcher.find()) {
                parsedValue = Double.parseDouble(matcher.group());
            }
        }

        return parsedValue;
    }

    private double clamp(double value, double minimum, double maximum) {
        double clampedValue = value;
        if (value < minimum) {
            clampedValue = minimum;
        }
        if (value > maximum) {
            clampedValue = maximum;
        }
        return clampedValue;
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
