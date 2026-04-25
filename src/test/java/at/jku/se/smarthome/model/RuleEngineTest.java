package at.jku.se.smarthome.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Assert;
import org.junit.Test;

import at.jku.se.smarthome.repository.SQLiteHomeRepository;
import at.jku.se.smarthome.repository.SQLiteUserRepository;

@SuppressWarnings({
        "PMD.CommentRequired",
        "PMD.AtLeastOneConstructor",
        "PMD.JUnitAssertionsShouldIncludeMessage"
})
public class RuleEngineTest {

    @Test
    public void createRuleStoresRuleInSystem() {
        SmartHomeSystem system = new SmartHomeSystem();
        system.registerUser("owner@example.com", "password123");
        system.loginUser("owner@example.com", "password123");
        Room room = system.createRoom("Hallway");
        Device sensor = system.createDevice(room.getId(), "Motion Sensor", DeviceType.SENSOR);
        Device light = system.createDevice(room.getId(), "Hallway Light", DeviceType.SWITCH);

        Rule rule = system.createRule(
                "Motion turns light on",
                RuleTriggerType.DEVICE_STATE_CHANGE,
                sensor.getId(),
                1.0,
                RuleActionType.SET_DEVICE_STATE,
                light.getId(),
                1.0
        );

        Assert.assertEquals("Exactly one rule should be stored", 1, system.getRules().size());
        Assert.assertEquals("The created rule should keep its configured name", "Motion turns light on", rule.getName());
        Assert.assertEquals("The trigger source should reference the sensor", sensor.getId(),
                rule.getTrigger().getSourceDeviceId());
        Assert.assertEquals("The action target should reference the light", light.getId(),
                rule.getAction().getTargetDeviceId());
    }

    @Test
    public void deviceStateTriggerExecutesConfiguredActionAutomatically() {
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

        system.updateDeviceValue(sensor.getId(), 1.0);

        Assert.assertTrue("The configured target action should turn the light on", light.isOn());
        Assert.assertEquals("Two activity entries are expected: one manual sensor update and one rule action",
                2, system.getActivityLog().size());
        Assert.assertEquals("The second activity entry should be caused by a rule",
                ActivityActorType.RULE, system.getActivityLog().get(1).getActorType());
        Assert.assertEquals("The executing rule name should be recorded in the activity log",
                "Motion turns light on", system.getActivityLog().get(1).getActorName());
    }

    @Test
    public void nonMatchingDeviceStateDoesNotExecuteRuleAction() {
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

        system.updateDeviceValue(sensor.getId(), 2.0);

        Assert.assertFalse("The light should stay off when the trigger value does not match", light.isOn());
        Assert.assertEquals("Only the manual sensor update should be logged", 1, system.getActivityLog().size());
    }

    @Test
    public void rulesAreLoadedAgainAfterRestart() throws IOException {
        Path databaseFile = Files.createTempFile("smarthome-rules", ".db");
        databaseFile.toFile().deleteOnExit();
        String databaseUrl = "jdbc:sqlite:" + databaseFile;

        SmartHomeSystem firstSystem = new SmartHomeSystem(
                new SQLiteUserRepository(databaseUrl),
                new SQLiteHomeRepository(databaseUrl)
        );
        firstSystem.registerUser("owner@example.com", "password123");
        firstSystem.loginUser("owner@example.com", "password123");
        Room room = firstSystem.createRoom("Hallway");
        Device sensor = firstSystem.createDevice(room.getId(), "Motion Sensor", DeviceType.SENSOR);
        Device light = firstSystem.createDevice(room.getId(), "Hallway Light", DeviceType.SWITCH);
        firstSystem.createRule(
                "Motion turns light on",
                RuleTriggerType.DEVICE_STATE_CHANGE,
                sensor.getId(),
                1.0,
                RuleActionType.SET_DEVICE_STATE,
                light.getId(),
                1.0
        );
        firstSystem.logoutUser();

        SmartHomeSystem secondSystem = new SmartHomeSystem(
                new SQLiteUserRepository(databaseUrl),
                new SQLiteHomeRepository(databaseUrl)
        );
        secondSystem.loginUser("owner@example.com", "password123");

        Assert.assertEquals("The persisted rule should be loaded again after restart", 1, secondSystem.getRules().size());
        Assert.assertEquals("Motion turns light on", secondSystem.getRules().get(0).getName());
    }

    @Test
    public void removingDeviceAlsoRemovesAssociatedRules() {
        SmartHomeSystem system = new SmartHomeSystem();
        system.registerUser("owner@example.com", "password123");
        system.loginUser("owner@example.com", "password123");
        Room room = system.createRoom("Hallway");
        Device sensor = system.createDevice(room.getId(), "Motion Sensor", DeviceType.SENSOR);
        Device light = system.createDevice(room.getId(), "Hallway Light", DeviceType.SWITCH);
        Rule rule = system.createRule(
                "Motion turns light on",
                RuleTriggerType.DEVICE_STATE_CHANGE,
                sensor.getId(),
                1.0,
                RuleActionType.SET_DEVICE_STATE,
                light.getId(),
                1.0
        );

        system.removeDevice(sensor.getId());

        Assert.assertTrue("Removing the trigger device should also remove associated rules", system.getRules().isEmpty());
        Assert.assertFalse("The removed rule should no longer be removable a second time", system.removeRule(rule.getId()));
    }
}
