package at.jku.se.smarthome.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import at.jku.se.smarthome.repository.InMemoryHomeRepository;
import at.jku.se.smarthome.repository.InMemoryUserRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.Test;

@SuppressWarnings("PMD")
public class EnergyDashboardTest {
    private static final double DELTA = 0.000_001;

    @Test
    public void dayAggregationCalculatesDeviceRoomAndHouseholdConsumption() {
        EnergyConsumptionCalculator calculator = new EnergyConsumptionCalculator(
                Clock.fixed(Instant.parse("2026-04-29T12:00:00Z"), ZoneOffset.UTC)
        );
        Room livingRoom = new Room("room-1", "Living Room");
        Device lamp = new Device("device-1", "Lamp", DeviceType.SWITCH);
        Device dimmer = new Device("device-2", "Dimmer", DeviceType.DIMMER);
        livingRoom.addDevice(lamp);
        livingRoom.addDevice(dimmer);

        List<ActivityLogEntry> activityLog = List.of(
                createEntry("2026-04-28T23:00:00Z", lamp, "Off", "On"),
                createEntry("2026-04-29T06:00:00Z", lamp, "On", "Off"),
                createEntry("2026-04-29T00:00:00Z", dimmer, "0 %", "50 %")
        );

        EnergyDashboard dashboard = calculator.calculate(List.of(livingRoom), activityLog, EnergyAggregationPeriod.DAY);

        assertEquals(EnergyAggregationPeriod.DAY, dashboard.getAggregationPeriod());
        assertEquals(Instant.parse("2026-04-29T00:00:00Z"), dashboard.getStartInclusive());
        assertEquals(Instant.parse("2026-04-29T12:00:00Z"), dashboard.getEndExclusive());
        assertEquals(0.84, dashboard.getTotalConsumptionKiloWattHours(), DELTA);
        assertEquals(0.84, dashboard.getRoomConsumptions().get(0).getTotalConsumptionKiloWattHours(), DELTA);
        assertEquals("Dimmer", dashboard.getDeviceConsumptions().get(0).getDeviceName());
        assertEquals(0.48, dashboard.getDeviceConsumptions().get(0).getConsumptionKiloWattHours(), DELTA);
        assertEquals("Lamp", dashboard.getDeviceConsumptions().get(1).getDeviceName());
        assertEquals(0.36, dashboard.getDeviceConsumptions().get(1).getConsumptionKiloWattHours(), DELTA);
    }

    @Test
    public void weekAggregationStartsOnMondayAndUsesPreviousState() {
        EnergyConsumptionCalculator calculator = new EnergyConsumptionCalculator(
                Clock.fixed(Instant.parse("2026-04-29T12:00:00Z"), ZoneOffset.UTC)
        );
        Room kitchen = new Room("room-1", "Kitchen");
        Device light = new Device("device-1", "Kitchen Light", DeviceType.SWITCH);
        kitchen.addDevice(light);
        List<ActivityLogEntry> activityLog = List.of(
                createEntry("2026-04-26T20:00:00Z", light, "Off", "On"),
                createEntry("2026-04-28T00:00:00Z", light, "On", "Off")
        );

        EnergyDashboard dashboard = calculator.calculate(List.of(kitchen), activityLog, EnergyAggregationPeriod.WEEK);

        assertEquals(Instant.parse("2026-04-27T00:00:00Z"), dashboard.getStartInclusive());
        assertEquals(1.44, dashboard.getTotalConsumptionKiloWattHours(), DELTA);
    }

    @Test
    public void roomAndDeviceConsumptionObjectsAreImmutableAndValidateInput() {
        EnergyDeviceConsumption deviceConsumption = new EnergyDeviceConsumption(
                "room-1",
                "Living Room",
                "device-1",
                "Lamp",
                DeviceType.SWITCH,
                0.5
        );
        EnergyRoomConsumption roomConsumption = new EnergyRoomConsumption(
                "room-1",
                "Living Room",
                List.of(deviceConsumption)
        );
        EnergyDashboard dashboard = new EnergyDashboard(
                EnergyAggregationPeriod.DAY,
                Instant.parse("2026-04-29T00:00:00Z"),
                Instant.parse("2026-04-29T12:00:00Z"),
                List.of(roomConsumption)
        );

        assertEquals(0.5, roomConsumption.getTotalConsumptionKiloWattHours(), DELTA);
        assertEquals(0.5, dashboard.getTotalConsumptionKiloWattHours(), DELTA);
        assertThrows(UnsupportedOperationException.class,
                () -> dashboard.getRoomConsumptions().add(roomConsumption));
        assertThrows(IllegalArgumentException.class,
                () -> new EnergyDeviceConsumption("room", "Room", "device", "Device", null, 0.0));
        assertThrows(IllegalArgumentException.class,
                () -> new EnergyRoomConsumption("room", "Room", null));
        assertThrows(IllegalArgumentException.class,
                () -> new EnergyDashboard(null, Instant.now(), Instant.now(), List.of()));
    }

    @Test
    public void calculatorHandlesThermostatSensorBlindAndInvalidNumericStates() {
        EnergyConsumptionCalculator calculator = new EnergyConsumptionCalculator(
                Clock.fixed(Instant.parse("2026-04-29T02:00:00Z"), ZoneOffset.UTC)
        );
        Room room = new Room("room-1", "Mixed");
        Device thermostat = new Device("device-1", "Thermostat", DeviceType.THERMOSTAT);
        Device sensor = new Device("device-2", "Sensor", DeviceType.SENSOR);
        Device blind = new Device("device-3", "Blind", DeviceType.BLIND);
        Device dimmer = new Device("device-4", "Broken Dimmer", DeviceType.DIMMER);
        room.addDevice(thermostat);
        room.addDevice(sensor);
        room.addDevice(blind);
        room.addDevice(dimmer);
        List<ActivityLogEntry> activityLog = List.of(
                createEntry("2026-04-29T00:00:00Z", thermostat, "20.0 °C", "21.0 °C"),
                createEntry("2026-04-29T00:00:00Z", dimmer, "Off", "invalid")
        );

        EnergyDashboard dashboard = calculator.calculate(List.of(room), activityLog, EnergyAggregationPeriod.DAY);

        assertEquals(2.104, dashboard.getTotalConsumptionKiloWattHours(), DELTA);
        assertTrue(dashboard.getDeviceConsumptions().stream()
                .anyMatch(device -> "Blind".equals(device.getDeviceName())
                        && device.getConsumptionKiloWattHours() == 0.0));
    }

    @Test
    public void smartHomeSystemExposesEnergyDashboardForCurrentHome() {
        SmartHomeSystem system = new SmartHomeSystem(
                new InMemoryUserRepository(),
                new InMemoryHomeRepository(),
                Clock.fixed(Instant.parse("2026-04-29T12:00:00Z"), ZoneOffset.UTC)
        );
        system.registerUser("owner@example.com", "password123");
        system.loginUser("owner@example.com", "password123");
        Room room = system.createRoom("Office");
        Device lamp = system.createDevice(room.getId(), "Desk Lamp", DeviceType.SWITCH);
        lamp.setPowerState(true);

        EnergyDashboard dashboard = system.getEnergyDashboard(EnergyAggregationPeriod.DAY);

        assertEquals(0.72, dashboard.getTotalConsumptionKiloWattHours(), DELTA);
        assertEquals("Office", dashboard.getRoomConsumptions().get(0).getRoomName());
        assertEquals("Desk Lamp", dashboard.getDeviceConsumptions().get(0).getDeviceName());
    }

    @Test
    public void calculatorRejectsInvalidRequiredInput() {
        EnergyConsumptionCalculator calculator = new EnergyConsumptionCalculator(
                Clock.fixed(Instant.parse("2026-04-29T12:00:00Z"), ZoneOffset.UTC)
        );

        assertThrows(IllegalArgumentException.class, () -> new EnergyConsumptionCalculator(null));
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(null, List.of(), EnergyAggregationPeriod.DAY));
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(List.of(), null, EnergyAggregationPeriod.DAY));
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(List.of(), List.of(), null));
    }

    private ActivityLogEntry createEntry(String timestamp, Device device, String previousState, String newState) {
        return new ActivityLogEntry(
                Instant.parse(timestamp),
                device.getId(),
                device.getName(),
                ActivityActorType.USER,
                "tester",
                previousState,
                newState
        );
    }
}
