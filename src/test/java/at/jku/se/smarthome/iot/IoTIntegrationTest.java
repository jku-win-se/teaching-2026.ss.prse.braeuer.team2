package at.jku.se.smarthome.iot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import at.jku.se.smarthome.model.ActivityActorType;
import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.model.DeviceType;
import at.jku.se.smarthome.model.Room;
import at.jku.se.smarthome.model.RuleActionType;
import at.jku.se.smarthome.model.SmartHomeSystem;
import at.jku.se.smarthome.model.ThresholdOperator;
import at.jku.se.smarthome.repository.InMemoryHomeRepository;
import at.jku.se.smarthome.repository.InMemoryUserRepository;

@SuppressWarnings("PMD")
public class IoTIntegrationTest {

    @Test
    public void defaultNoOpIntegration_keepsVirtualDeviceUsable() {
        SmartHomeSystem system = new SmartHomeSystem();
        Room room = new Room("room-1", "Living Room");
        Device device = new Device("device-1", "Lamp", DeviceType.SWITCH);
        room.addDevice(device);
        system.addRoom(room);

        system.toggleDevice(device.getId());

        assertTrue(device.isOn());
        assertFalse(system.isIoTIntegrationConnected());
    }

    @Test
    public void deviceAction_publishesStateThroughIntegration() {
        FakeIoTIntegration integration = new FakeIoTIntegration();
        SmartHomeSystem system = createLoggedInSystem(integration);
        Room room = system.createRoom("Living Room");
        Device device = system.createDevice(room.getId(), "Dimmer", DeviceType.DIMMER);

        system.updateDeviceValue(device.getId(), 60);

        assertEquals(1, integration.getPublishedMessages().size());
        DeviceStateMessage message = integration.getPublishedMessages().get(0);
        assertEquals(device.getId(), message.getDeviceId());
        assertEquals(DeviceType.DIMMER, message.getDeviceType());
        assertTrue(message.isPoweredOn());
        assertEquals(60.0, message.getValue(), 0.0001);
    }

    @Test
    public void incomingStatus_updatesVirtualDeviceWithoutPublishingLoop() {
        FakeIoTIntegration integration = new FakeIoTIntegration();
        SmartHomeSystem system = createLoggedInSystem(integration);
        Room room = system.createRoom("Living Room");
        Device device = system.createDevice(room.getId(), "Lamp", DeviceType.SWITCH);

        integration.fireIncomingStatus(new DeviceStateMessage(device.getId(), DeviceType.SWITCH, true, null));

        assertTrue(device.isOn());
        assertTrue(integration.getPublishedMessages().isEmpty());
        assertEquals(1, system.getActivityLog().size());
        assertEquals(ActivityActorType.USER, system.getActivityLog().get(0).getActorType());
        assertEquals("IoT Integration", system.getActivityLog().get(0).getActorName());
    }

    @Test
    public void incomingSensorStatus_canTriggerRules() {
        FakeIoTIntegration integration = new FakeIoTIntegration();
        SmartHomeSystem system = createLoggedInSystem(integration);
        Room room = system.createRoom("Living Room");
        Device sensor = system.createDevice(room.getId(), "Temperature Sensor", DeviceType.SENSOR);
        Device lamp = system.createDevice(room.getId(), "Lamp", DeviceType.SWITCH);
        system.createThresholdRule(
                "Temperature Alert",
                sensor.getId(),
                ThresholdOperator.ABOVE,
                25.0,
                RuleActionType.SET_DEVICE_STATE,
                lamp.getId(),
                1.0
        );

        integration.fireIncomingStatus(new DeviceStateMessage(sensor.getId(), DeviceType.SENSOR, true, 28.0));

        assertTrue(lamp.isOn());
        assertEquals(1, integration.getPublishedMessages().size());
        assertEquals(lamp.getId(), integration.getPublishedMessages().get(0).getDeviceId());
    }

    @Test
    public void mqttIntegration_mapsDeviceStateToTopicsAndPayload() {
        MqttIoTIntegration integration = new MqttIoTIntegration("tcp://localhost:1883", "client-1");
        integration.connect();

        integration.publishDeviceState(new DeviceStateMessage("device-1", DeviceType.SWITCH, true, null));

        assertTrue(integration.isConnected());
        assertEquals(1, integration.getPublishedMessages().size());
        MqttIoTIntegration.PublishedMqttMessage message = integration.getPublishedMessages().get(0);
        assertEquals("smarthome/devices/device-1/command", message.getTopic());
        assertEquals("type=SWITCH;on=true;value=", message.getPayload());
    }

    @Test
    public void mqttIntegration_receivesStatusMessages() {
        MqttIoTIntegration integration = new MqttIoTIntegration("tcp://localhost:1883", "client-1");
        List<DeviceStateMessage> receivedMessages = new ArrayList<>();
        integration.setStatusListener(receivedMessages::add);

        integration.receiveStatusMessage(
                integration.buildStatusTopic("sensor-1"),
                "type=SENSOR;on=true;value=18.5"
        );

        assertEquals(1, receivedMessages.size());
        assertEquals("sensor-1", receivedMessages.get(0).getDeviceId());
        assertEquals(DeviceType.SENSOR, receivedMessages.get(0).getDeviceType());
        assertEquals(18.5, receivedMessages.get(0).getValue(), 0.0001);
    }

    private SmartHomeSystem createLoggedInSystem(IoTIntegration integration) {
        SmartHomeSystem system = new SmartHomeSystem(
                new InMemoryUserRepository(),
                new InMemoryHomeRepository(),
                Clock.fixed(Instant.parse("2026-04-25T10:15:30Z"), ZoneOffset.UTC),
                integration
        );
        system.registerUser("owner@example.com", "password123");
        system.loginUser("owner@example.com", "password123");
        return system;
    }

    private static class FakeIoTIntegration implements IoTIntegration {
        private final List<DeviceStateMessage> publishedMessages = new ArrayList<>();
        private DeviceStatusListener statusListener;
        private boolean connected;

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
            publishedMessages.add(message);
        }

        @Override
        public void setStatusListener(DeviceStatusListener listener) {
            statusListener = listener;
        }

        private List<DeviceStateMessage> getPublishedMessages() {
            return publishedMessages;
        }

        private void fireIncomingStatus(DeviceStateMessage message) {
            statusListener.onDeviceStatusReceived(message);
        }
    }
}
