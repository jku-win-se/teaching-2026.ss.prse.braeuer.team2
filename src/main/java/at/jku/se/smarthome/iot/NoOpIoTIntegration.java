package at.jku.se.smarthome.iot;

/**
 * Default integration that keeps the smart home fully virtual.
 */
@SuppressWarnings("PMD.AtLeastOneConstructor")
public class NoOpIoTIntegration implements IoTIntegration {

    @Override
    public void connect() {
        // Intentionally empty: the virtual-only mode has no external connection.
    }

    @Override
    public void disconnect() {
        // Intentionally empty: the virtual-only mode has no external connection.
    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public void publishDeviceState(DeviceStateMessage message) {
        // Intentionally empty: virtual device changes do not need a physical protocol.
    }

    @Override
    public void setStatusListener(DeviceStatusListener listener) {
        // Intentionally empty: there is no external source for incoming messages.
    }
}
