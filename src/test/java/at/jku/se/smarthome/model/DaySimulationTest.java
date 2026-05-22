package at.jku.se.smarthome.model;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings({
        "PMD.CommentRequired",
        "PMD.AtLeastOneConstructor",
        "PMD.JUnitAssertionsShouldIncludeMessage"
})
public class DaySimulationTest {

    @Test
    public void initialSensorValuesTriggerActiveRulesWithoutChangingLiveDevices() {
        SmartHomeSystem system = createLoggedInSystem();
        Room room = system.createRoom("Living Room");
        Device temperatureSensor = system.createDevice(room.getId(), "Temperature", DeviceType.SENSOR);
        Device heater = system.createDevice(room.getId(), "Heater", DeviceType.SWITCH);
        Rule rule = system.createThresholdRule(
                "Heat when cold",
                temperatureSensor.getId(),
                ThresholdOperator.BELOW,
                18.0,
                RuleActionType.SET_DEVICE_STATE,
                heater.getId(),
                1.0
        );

        DaySimulationResult result = system.simulateDay(new DaySimulationRequest(
                LocalDateTime.of(2026, 5, 22, 6, 0),
                Map.of(temperatureSensor.getId(), 17.0),
                Set.of(rule.getId())
        ));

        Assert.assertEquals("The simulation should show the sensor update and the rule action",
                2, result.getEvents().size());
        Assert.assertFalse("The live heater must remain off after the simulation", heater.isOn());
        Assert.assertEquals("The live sensor value must remain unchanged after the simulation",
                0.0, temperatureSensor.getValue(), 0.001);
        Assert.assertTrue("The simulated heater should be on in the result",
                findSimulatedDevice(result, heater.getId()).isOn());
    }

    @Test
    public void inactiveRulesAreIgnoredDuringSimulation() {
        SmartHomeSystem system = createLoggedInSystem();
        Room room = system.createRoom("Living Room");
        Device temperatureSensor = system.createDevice(room.getId(), "Temperature", DeviceType.SENSOR);
        Device heater = system.createDevice(room.getId(), "Heater", DeviceType.SWITCH);
        system.createThresholdRule(
                "Heat when cold",
                temperatureSensor.getId(),
                ThresholdOperator.BELOW,
                18.0,
                RuleActionType.SET_DEVICE_STATE,
                heater.getId(),
                1.0
        );

        DaySimulationResult result = system.simulateDay(new DaySimulationRequest(
                LocalDateTime.of(2026, 5, 22, 6, 0),
                Map.of(temperatureSensor.getId(), 17.0),
                Set.of()
        ));

        Assert.assertEquals("Only the initial sensor event should be recorded", 1, result.getEvents().size());
        Assert.assertFalse("The simulated heater must stay off when its rule is inactive",
                findSimulatedDevice(result, heater.getId()).isOn());
    }

    @Test
    public void schedulesAndTimeRulesRunInSimulatedDay() {
        SmartHomeSystem system = createLoggedInSystem();
        Room room = system.createRoom("Living Room");
        Device dimmer = system.createDevice(room.getId(), "Dimmer", DeviceType.DIMMER);
        Device light = system.createDevice(room.getId(), "Light", DeviceType.SWITCH);
        system.createSchedule(
                "Morning dimmer",
                dimmer.getId(),
                ScheduleActionType.SET_VALUE,
                50.0,
                LocalTime.of(6, 30),
                Set.of(DayOfWeek.FRIDAY)
        );
        Rule timeRule = system.createTimeRule(
                "Morning light",
                LocalTime.of(7, 0),
                RuleActionType.SET_DEVICE_STATE,
                light.getId(),
                1.0
        );

        DaySimulationResult result = system.simulateDay(new DaySimulationRequest(
                LocalDateTime.of(2026, 5, 22, 6, 0),
                Map.of(),
                Set.of(timeRule.getId())
        ));

        Assert.assertEquals("Two simulated changes should be recorded", 2, result.getEvents().size());
        Assert.assertEquals("The simulated dimmer should follow the schedule", 50.0,
                findSimulatedDevice(result, dimmer.getId()).getValue(), 0.001);
        Assert.assertTrue("The simulated light should follow the time rule",
                findSimulatedDevice(result, light.getId()).isOn());
        Assert.assertEquals("The live dimmer must remain unchanged", 0.0, dimmer.getValue(), 0.001);
        Assert.assertFalse("The live light must remain unchanged", light.isOn());
    }

    private SmartHomeSystem createLoggedInSystem() {
        SmartHomeSystem system = new SmartHomeSystem();
        system.registerUser("owner@example.com", "password123");
        system.loginUser("owner@example.com", "password123");
        return system;
    }

    private Device findSimulatedDevice(DaySimulationResult result, String deviceId) {
        for (Room room : result.getFinalRooms()) {
            Device device = room.findDeviceById(deviceId);
            if (device != null) {
                return device;
            }
        }
        throw new AssertionError("Simulated device not found");
    }
}
