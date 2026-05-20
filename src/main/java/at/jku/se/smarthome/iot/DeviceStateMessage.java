package at.jku.se.smarthome.iot;

import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.model.DeviceType;

/**
 * Immutable message that represents a device state for external IoT integrations.
 */
@SuppressWarnings("PMD.DataClass")
public class DeviceStateMessage {
    /**
     * Identifier of the affected smart home device.
     */
    private final String deviceId;

    /**
     * Type of the affected smart home device.
     */
    private final DeviceType deviceType;

    /**
     * Power state reported for the affected device.
     */
    private final boolean poweredOn;

    /**
     * Numeric device value, if the device type supports one.
     */
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

    /**
     * Returns the identifier of the affected smart home device.
     *
     * @return the device id
     */
    public String getDeviceId() {
        return deviceId;
    }

    /**
     * Returns the type of the affected smart home device.
     *
     * @return the device type
     */
    public DeviceType getDeviceType() {
        return deviceType;
    }

    /**
     * Returns whether the affected smart home device is powered on.
     *
     * @return {@code true} if the device is powered on
     */
    public boolean isPoweredOn() {
        return poweredOn;
    }

    /**
     * Returns the numeric device value, if available.
     *
     * @return the numeric value, or {@code null}
     */
    public Double getValue() {
        return value;
    }
}
