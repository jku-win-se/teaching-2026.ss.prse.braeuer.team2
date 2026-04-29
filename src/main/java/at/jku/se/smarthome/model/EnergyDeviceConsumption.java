package at.jku.se.smarthome.model;

/**
 * Immutable estimated energy consumption for one device in one aggregation period.
 */
@SuppressWarnings("PMD.DataClass")
public final class EnergyDeviceConsumption {
    private final String roomId;
    private final String roomName;
    private final String deviceId;
    private final String deviceName;
    private final DeviceType deviceType;
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
        if (candidate < 0.0) {
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
