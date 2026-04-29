package at.jku.se.smarthome.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

import org.junit.Test;

@SuppressWarnings("PMD")
public class PlanningConflictTest {
    @Test
    public void conflictingSchedulesForSameDeviceTimeAndDayAreRejected() {
        SmartHomeSystem system = createLoggedInOwnerSystem();
        Device lamp = createSwitch(system);
        system.createSchedule(
                "Morning on",
                lamp.getId(),
                ScheduleActionType.SET_VALUE,
                1.0,
                LocalTime.of(7, 0),
                Set.of(DayOfWeek.MONDAY)
        );

        PlanningConflictException exception = assertThrows(PlanningConflictException.class, () -> system.createSchedule(
                "Morning off",
                lamp.getId(),
                ScheduleActionType.SET_VALUE,
                0.0,
                LocalTime.of(7, 0),
                Set.of(DayOfWeek.MONDAY)
        ));

        assertTrue(exception.getMessage().contains("Morning off"));
        assertTrue(exception.getMessage().contains("Morning on"));
        assertTrue(exception.getMessage().contains("Desk Lamp"));
        assertEquals(1, system.getSchedules().size());
    }

    @Test
    public void schedulesWithSameTargetOrDifferentDaysAreAllowed() {
        SmartHomeSystem system = createLoggedInOwnerSystem();
        Device lamp = createSwitch(system);

        system.createSchedule("Monday on", lamp.getId(), ScheduleActionType.SET_VALUE, 1.0,
                LocalTime.of(7, 0), Set.of(DayOfWeek.MONDAY));
        system.createSchedule("Also on", lamp.getId(), ScheduleActionType.SET_VALUE, 1.0,
                LocalTime.of(7, 0), Set.of(DayOfWeek.MONDAY));
        system.createSchedule("Tuesday off", lamp.getId(), ScheduleActionType.SET_VALUE, 0.0,
                LocalTime.of(7, 0), Set.of(DayOfWeek.TUESDAY));

        assertEquals(3, system.getSchedules().size());
    }

    @Test
    public void toggleScheduleConflictsWithSimultaneousExplicitSchedule() {
        SmartHomeSystem system = createLoggedInOwnerSystem();
        Device lamp = createSwitch(system);
        system.createSchedule("Toggle lamp", lamp.getId(), ScheduleActionType.TOGGLE, null,
                LocalTime.of(20, 0), Set.of(DayOfWeek.FRIDAY));

        PlanningConflictException exception = assertThrows(PlanningConflictException.class, () -> system.createSchedule(
                "Turn lamp on",
                lamp.getId(),
                ScheduleActionType.SET_VALUE,
                1.0,
                LocalTime.of(20, 0),
                Set.of(DayOfWeek.FRIDAY)
        ));

        assertTrue(exception.getMessage().contains("Toggle lamp"));
    }

    @Test
    public void timeRuleConflictsWithScheduleForSameDeviceTimeAndDifferentTarget() {
        SmartHomeSystem system = createLoggedInOwnerSystem();
        Device lamp = createSwitch(system);
        system.createSchedule("Morning on", lamp.getId(), ScheduleActionType.SET_VALUE, 1.0,
                LocalTime.of(7, 0), Set.of(DayOfWeek.MONDAY));

        PlanningConflictException exception = assertThrows(PlanningConflictException.class, () -> system.createTimeRule(
                "Morning off rule",
                LocalTime.of(7, 0),
                RuleActionType.SET_DEVICE_STATE,
                lamp.getId(),
                0.0
        ));

        assertTrue(exception.getMessage().contains("Morning off rule"));
        assertTrue(exception.getMessage().contains("Morning on"));
        assertEquals(0, system.getRules().size());
    }

    @Test
    public void timeRuleAndScheduleWithSameTargetAreAllowed() {
        SmartHomeSystem system = createLoggedInOwnerSystem();
        Device lamp = createSwitch(system);
        system.createSchedule("Morning on", lamp.getId(), ScheduleActionType.SET_VALUE, 1.0,
                LocalTime.of(7, 0), Set.of(DayOfWeek.MONDAY));

        system.createTimeRule("Morning on rule", LocalTime.of(7, 0),
                RuleActionType.SET_DEVICE_STATE, lamp.getId(), 1.0);

        assertEquals(1, system.getRules().size());
    }

    @Test
    public void conflictingTimeRulesForSameDeviceAndTimeAreRejected() {
        SmartHomeSystem system = createLoggedInOwnerSystem();
        Device lamp = createSwitch(system);
        system.createTimeRule("On rule", LocalTime.of(7, 0),
                RuleActionType.SET_DEVICE_STATE, lamp.getId(), 1.0);

        PlanningConflictException exception = assertThrows(PlanningConflictException.class, () -> system.createTimeRule(
                "Off rule",
                LocalTime.of(7, 0),
                RuleActionType.SET_DEVICE_STATE,
                lamp.getId(),
                0.0
        ));

        assertTrue(exception.getMessage().contains("On rule"));
        assertEquals(1, system.getRules().size());
    }

    @Test
    public void conflictingEventRulesForSameTriggerAreRejected() {
        SmartHomeSystem system = createLoggedInOwnerSystem();
        Room room = system.createRoom("Office");
        Device sensor = system.createDevice(room.getId(), "Motion", DeviceType.SENSOR);
        Device lamp = system.createDevice(room.getId(), "Desk Lamp", DeviceType.SWITCH);
        system.createRule("Motion on", RuleTriggerType.DEVICE_STATE_CHANGE, sensor.getId(), 1.0,
                RuleActionType.SET_DEVICE_STATE, lamp.getId(), 1.0);

        PlanningConflictException exception = assertThrows(PlanningConflictException.class, () -> system.createRule(
                "Motion off",
                RuleTriggerType.DEVICE_STATE_CHANGE,
                sensor.getId(),
                1.0,
                RuleActionType.SET_DEVICE_STATE,
                lamp.getId(),
                0.0
        ));

        assertTrue(exception.getMessage().contains("Motion off"));
        assertTrue(exception.getMessage().contains("Motion on"));
    }

    @Test
    public void updatingScheduleToConflictIsRejectedAndKeepsPreviousConfiguration() {
        SmartHomeSystem system = createLoggedInOwnerSystem();
        Device lamp = createSwitch(system);
        system.createSchedule("Morning on", lamp.getId(), ScheduleActionType.SET_VALUE, 1.0,
                LocalTime.of(7, 0), Set.of(DayOfWeek.MONDAY));
        Schedule eveningOff = system.createSchedule("Evening off", lamp.getId(), ScheduleActionType.SET_VALUE, 0.0,
                LocalTime.of(20, 0), Set.of(DayOfWeek.MONDAY));

        assertThrows(PlanningConflictException.class, () -> system.updateSchedule(
                eveningOff.getId(),
                "Morning off",
                ScheduleActionType.SET_VALUE,
                0.0,
                LocalTime.of(7, 0),
                Set.of(DayOfWeek.MONDAY)
        ));

        assertEquals(LocalTime.of(20, 0), eveningOff.getExecutionTime());
        assertEquals("Evening off", eveningOff.getName());
    }

    @Test
    public void updatingTimeRuleToConflictIsRejectedAndKeepsPreviousConfiguration() {
        SmartHomeSystem system = createLoggedInOwnerSystem();
        Device lamp = createSwitch(system);
        system.createTimeRule("Morning on", LocalTime.of(7, 0),
                RuleActionType.SET_DEVICE_STATE, lamp.getId(), 1.0);
        Rule eveningOff = system.createTimeRule("Evening off", LocalTime.of(20, 0),
                RuleActionType.SET_DEVICE_STATE, lamp.getId(), 0.0);

        assertThrows(PlanningConflictException.class, () -> system.updateTimeRule(
                eveningOff.getId(),
                "Morning off",
                LocalTime.of(7, 0),
                RuleActionType.SET_DEVICE_STATE,
                lamp.getId(),
                0.0
        ));

        assertEquals("Evening off", eveningOff.getName());
        assertEquals(LocalTime.of(20, 0), eveningOff.getTrigger().getTriggerTime());
    }

    @Test
    public void updatingTimeRuleToNonConflictIsAllowed() {
        SmartHomeSystem system = createLoggedInOwnerSystem();
        Device lamp = createSwitch(system);
        Rule rule = system.createTimeRule("Morning on", LocalTime.of(7, 0),
                RuleActionType.SET_DEVICE_STATE, lamp.getId(), 1.0);

        system.updateTimeRule(rule.getId(), "Evening off", LocalTime.of(20, 0),
                RuleActionType.SET_DEVICE_STATE, lamp.getId(), 0.0);

        assertEquals("Evening off", rule.getName());
        assertEquals(LocalTime.of(20, 0), rule.getTrigger().getTriggerTime());
        assertEquals(0.0, rule.getAction().getTargetValue(), 0.0);
    }

    private SmartHomeSystem createLoggedInOwnerSystem() {
        SmartHomeSystem system = new SmartHomeSystem();
        system.registerUser("owner@example.com", "password123");
        system.loginUser("owner@example.com", "password123");
        return system;
    }

    private Device createSwitch(SmartHomeSystem system) {
        Room room = system.createRoom("Office");
        return system.createDevice(room.getId(), "Desk Lamp", DeviceType.SWITCH);
    }
}
