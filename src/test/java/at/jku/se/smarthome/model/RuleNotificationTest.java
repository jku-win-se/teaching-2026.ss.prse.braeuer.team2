package at.jku.se.smarthome.model;

import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings({
        "PMD.CommentRequired",
        "PMD.AtLeastOneConstructor",
        "PMD.JUnitAssertionsShouldIncludeMessage"
})
public class RuleNotificationTest {

    @Test
    public void successfulRuleExecutionCreatesNotification() {
        SmartHomeSystem system = createSystemWithMotionRule();

        system.updateDeviceValue(findDevice(system, "Motion Sensor").getId(), 1.0);

        Assert.assertEquals("Exactly one rule notification should be shown", 1,
                system.getRuleNotifications().size());
        RuleExecutionNotification notification = system.getRuleNotifications().get(0);
        Assert.assertTrue("The notification should report a successful execution", notification.isSuccessful());
        Assert.assertEquals("The notification should name the affected rule",
                "Motion turns light on", notification.getRuleName());
        Assert.assertTrue("The notification text should include the affected rule",
                notification.getMessage().contains("Motion turns light on"));
    }

    @Test
    public void failedRuleExecutionCreatesErrorNotification() {
        SmartHomeSystem system = createSystemWithMotionRule();
        Room room = system.getRooms().get(0);
        Device light = findDevice(system, "Hallway Light");
        room.removeDevice(light.getId());

        system.updateDeviceValue(findDevice(system, "Motion Sensor").getId(), 1.0);

        Assert.assertEquals("Exactly one rule notification should be shown", 1,
                system.getRuleNotifications().size());
        RuleExecutionNotification notification = system.getRuleNotifications().get(0);
        Assert.assertFalse("The notification should report a failed execution", notification.isSuccessful());
        Assert.assertEquals("The notification should name the affected rule",
                "Motion turns light on", notification.getRuleName());
        Assert.assertTrue("The notification text should include the failure reason",
                notification.getMessage().contains("Device not found"));
    }

    @Test
    public void dismissRuleNotificationRemovesVisibleNotification() {
        SmartHomeSystem system = createSystemWithMotionRule();
        system.updateDeviceValue(findDevice(system, "Motion Sensor").getId(), 1.0);
        RuleExecutionNotification notification = system.getRuleNotifications().get(0);

        boolean removed = system.dismissRuleNotification(notification);

        Assert.assertTrue("The selected notification should be removed", removed);
        Assert.assertTrue("No rule notifications should remain", system.getRuleNotifications().isEmpty());
    }

    private SmartHomeSystem createSystemWithMotionRule() {
        SmartHomeSystem system = new SmartHomeSystem();
        system.registerUser("owner@example.com", "password123");
        system.loginUser("owner@example.com", "password123");
        Room room = system.createRoom("Hallway");
        Device sensor = system.createDevice(room.getId(), "Motion Sensor", DeviceType.SENSOR);
        Device light = system.createDevice(room.getId(), "Hallway Light", DeviceType.SWITCH);
        system.createRule(
                "Motion turns light on",
                RuleTriggerType.DEVICE_STATE_CHANGE,
                sensor.getId(),
                1.0,
                RuleActionType.SET_DEVICE_STATE,
                light.getId(),
                1.0
        );
        return system;
    }

    private Device findDevice(SmartHomeSystem system, String name) {
        return system.getRooms().stream()
                .flatMap(room -> room.getDevices().stream())
                .filter(device -> device.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }
}
