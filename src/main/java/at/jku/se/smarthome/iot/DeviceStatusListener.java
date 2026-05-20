package at.jku.se.smarthome.iot;

/**
 * Listener that receives state updates from an external IoT integration.
 */
@FunctionalInterface
public interface DeviceStatusListener {

    /**
     * Handles an externally reported device state.
     *
     * @param message the incoming device state message
     */
    void onDeviceStatusReceived(DeviceStateMessage message);
}
