package at.jku.se.smarthome.model;

/**
 * Immutable estimated energy consumption for one device in one aggregation period.
 */
@SuppressWarnings("PMD.DataClass")
public final class EnergyDeviceConsumption {
    /**
     * Smallest allowed consumption value.
     */
    private static final double MINIMUM_CONSUMPTION_KILO_WATT_HOURS = 0.0;

    /**
     * Identifier of the room that owns the device.
     */
    private final String roomId;
    /**
     * Display name of the room that owns the device.
     */
    private final String roomName;
    /**
     * Identifier of the device.
     */
    private final String deviceId;
    /**
     * Display name of the device.
     */
    private final String deviceName;
    /**
     * Type of the device.
     */
    private final DeviceType deviceType;
    /**
     * Estimated consumption in kWh.
     */
    private final double consumptionKiloWattHours;

    /**
     * Creates a device energy aggregate.
     *
     * @param roomId the owning room id
     * @param roomName the owning room name
     * @param deviceId the device id
     * @param deviceName the device name
     * @param deviceType the device type
     * @param consumptionKiloWattHours the estimated energy consumption
     */
    public EnergyDeviceConsumption(String roomId, String roomName, String deviceId,
                                   String deviceName, DeviceType deviceType,
                                   double consumptionKiloWattHours) {
        this.roomId = requireNonBlank(roomId, "Room id must not be blank");
        this.roomName = requireNonBlank(roomName, "Room name must not be blank");
        this.deviceId = requireNonBlank(deviceId, "Device id must not be blank");
        this.deviceName = requireNonBlank(deviceName, "Device name must not be blank");
        this.deviceType = requireDeviceType(deviceType);
        this.consumptionKiloWattHours = requireNonNegative(consumptionKiloWattHours);
    }

    public String getRoomId() {
        return roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public DeviceType getDeviceType() {
        return deviceType;
    }

    public double getConsumptionKiloWattHours() {
        return consumptionKiloWattHours;
    }

    private static DeviceType requireDeviceType(DeviceType candidate) {
        if (candidate == null) {
            throw new IllegalArgumentException("Device type must not be null");
        }
        return candidate;
    }

    private static double requireNonNegative(double candidate) {
        if (candidate < MINIMUM_CONSUMPTION_KILO_WATT_HOURS) {
            throw new IllegalArgumentException("Consumption must not be negative");
        }
        return candidate;
    }

    private static String requireNonBlank(String candidate, String message) {
        if (candidate == null || candidate.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return candidate.trim();
    }
}
