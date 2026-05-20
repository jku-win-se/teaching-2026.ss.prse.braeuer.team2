package at.jku.se.smarthome.iot;

import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.model.DeviceType;

/**
 * Immutable message that represents a device state for external IoT integrations.
 */
@SuppressWarnings("PMD.DataClass")
public class DeviceStateMessage {
    private final String deviceId;
    private final DeviceType deviceType;
    private final boolean poweredOn;
    private final Double value;

    /**
     * Creates a message for a device state.
     *
     * @param deviceId the device id
     * @param deviceType the device type
     * @param poweredOn whether the device is powered on
     * @param value the numeric value, or {@code null} if the device has no value
     */
    public DeviceStateMessage(String deviceId, DeviceType deviceType, boolean poweredOn, Double value) {
        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("Device id must not be empty");
        }
        if (deviceType == null) {
            throw new IllegalArgumentException("Device type must not be null");
        }
        this.deviceId = deviceId.trim();
        this.deviceType = deviceType;
        this.poweredOn = poweredOn;
        this.value = value;
    }

    /**
     * Creates an IoT message from a domain device.
     *
     * @param device the source device
     * @return a device state message
     */
    public static DeviceStateMessage fromDevice(Device device) {
        if (device == null) {
            throw new IllegalArgumentException("Device must not be null");
        }
        Double numericValue = device.getType() == DeviceType.SWITCH ? null : device.getValue();
        return new DeviceStateMessage(device.getId(), device.getType(), device.isOn(), numericValue);
    }

    public String getDeviceId() {
        return deviceId;
    }

    public DeviceType getDeviceType() {
        return deviceType;
    }

    public boolean isPoweredOn() {
        return poweredOn;
    }

    public Double getValue() {
        return value;
    }
}
