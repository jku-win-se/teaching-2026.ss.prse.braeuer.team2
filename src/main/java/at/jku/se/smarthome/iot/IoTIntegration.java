package at.jku.se.smarthome.iot;

/**
 * Optional integration point for physical IoT protocols such as MQTT.
 */
public interface IoTIntegration {

    /**
     * Opens the connection to the external IoT protocol.
     */
    void connect();

    /**
     * Closes the connection to the external IoT protocol.
     */
    void disconnect();

    /**
     * Returns whether the integration is currently connected.
     *
     * @return {@code true} if connected, otherwise {@code false}
     */
    boolean isConnected();

    /**
     * Publishes a device state change to the external IoT protocol.
     *
     * @param message the state message to publish
     */
    void publishDeviceState(DeviceStateMessage message);

    /**
     * Registers a listener for incoming device state messages.
     *
     * @param listener the listener, or {@code null} to disable callbacks
     */
    void setStatusListener(DeviceStatusListener listener);
}
