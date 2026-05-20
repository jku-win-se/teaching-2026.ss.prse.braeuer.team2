package at.jku.se.smarthome.iot;

import java.util.ArrayList;
import java.util.List;

import at.jku.se.smarthome.model.DeviceType;

/**
 * Prepared MQTT integration that maps device states to MQTT-style topics and payloads.
 */
@SuppressWarnings("PMD")
public class MqttIoTIntegration implements IoTIntegration {
    private static final String COMMAND_TOPIC = "smarthome/devices/%s/command";
    private static final String STATUS_TOPIC = "smarthome/devices/%s/state";

    private final String brokerUrl;
    private final String clientId;
    private final List<PublishedMqttMessage> publishedMessages;
    private DeviceStatusListener statusListener;
    private boolean connected;

    /**
     * Creates a prepared MQTT integration.
     *
     * @param brokerUrl the MQTT broker URL
     * @param clientId the MQTT client id
     */
    public MqttIoTIntegration(String brokerUrl, String clientId) {
        if (brokerUrl == null || brokerUrl.isBlank()) {
            throw new IllegalArgumentException("Broker URL must not be empty");
        }
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("Client id must not be empty");
        }
        this.brokerUrl = brokerUrl.trim();
        this.clientId = clientId.trim();
        this.publishedMessages = new ArrayList<>();
    }

    @Override
    public void connect() {
        connected = true;
    }

    @Override
    public void disconnect() {
        connected = false;
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public void publishDeviceState(DeviceStateMessage message) {
        if (message == null || !connected) {
            return;
        }
        publishedMessages.add(new PublishedMqttMessage(
                buildCommandTopic(message.getDeviceId()),
                toPayload(message)
        ));
    }

    @Override
    public void setStatusListener(DeviceStatusListener listener) {
        this.statusListener = listener;
    }

    /**
     * Simulates a received MQTT status message for tests or demo wiring.
     *
     * @param topic the MQTT topic
     * @param payload the payload
     */
    public void receiveStatusMessage(String topic, String payload) {
        if (statusListener == null || topic == null || payload == null) {
            return;
        }
        String deviceId = extractDeviceId(topic);
        if (deviceId == null) {
            return;
        }
        statusListener.onDeviceStatusReceived(fromPayload(deviceId, payload));
    }

    /**
     * Returns the configured broker URL.
     *
     * @return the broker URL
     */
    public String getBrokerUrl() {
        return brokerUrl;
    }

    /**
     * Returns the configured MQTT client id.
     *
     * @return the client id
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Returns all messages published by this prepared integration.
     *
     * @return a defensive copy of published messages
     */
    public List<PublishedMqttMessage> getPublishedMessages() {
        return List.copyOf(publishedMessages);
    }

    /**
     * Builds the MQTT command topic for a device.
     *
     * @param deviceId the device id
     * @return the command topic
     */
    public String buildCommandTopic(String deviceId) {
        return String.format(COMMAND_TOPIC, deviceId);
    }

    /**
     * Builds the MQTT status topic for a device.
     *
     * @param deviceId the device id
     * @return the status topic
     */
    public String buildStatusTopic(String deviceId) {
        return String.format(STATUS_TOPIC, deviceId);
    }

    private String toPayload(DeviceStateMessage message) {
        String numericValue = message.getValue() == null ? "" : message.getValue().toString();
        return "type=" + message.getDeviceType().name()
                + ";on=" + message.isPoweredOn()
                + ";value=" + numericValue;
    }

    private DeviceStateMessage fromPayload(String deviceId, String payload) {
        DeviceType deviceType = DeviceType.valueOf(readPayloadValue(payload, "type"));
        boolean poweredOn = Boolean.parseBoolean(readPayloadValue(payload, "on"));
        String valueText = readPayloadValue(payload, "value");
        Double numericValue = valueText.isBlank() ? null : Double.valueOf(valueText);
        return new DeviceStateMessage(deviceId, deviceType, poweredOn, numericValue);
    }

    private String readPayloadValue(String payload, String key) {
        String prefix = key + "=";
        for (String part : payload.split(";")) {
            if (part.startsWith(prefix)) {
                return part.substring(prefix.length());
            }
        }
        throw new IllegalArgumentException("Payload field missing: " + key);
    }

    private String extractDeviceId(String topic) {
        String[] topicParts = topic.split("/");
        if (topicParts.length != 4 || !"smarthome".equals(topicParts[0]) || !"devices".equals(topicParts[1])
                || !"state".equals(topicParts[3])) {
            return null;
        }
        return topicParts[2];
    }

    /**
     * Published MQTT-style message.
     */
    @SuppressWarnings("PMD.DataClass")
    public static class PublishedMqttMessage {
        private final String topic;
        private final String payload;

        /**
         * Creates a published MQTT-style message.
         *
         * @param topic the MQTT topic
         * @param payload the MQTT payload
         */
        public PublishedMqttMessage(String topic, String payload) {
            this.topic = topic;
            this.payload = payload;
        }

        public String getTopic() {
            return topic;
        }

        public String getPayload() {
            return payload;
        }
    }
}
