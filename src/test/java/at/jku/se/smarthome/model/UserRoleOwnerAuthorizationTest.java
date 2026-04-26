package at.jku.se.smarthome.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

@SuppressWarnings({
        "PMD.CommentRequired",
        "PMD.AtLeastOneConstructor"
})
public class UserRoleOwnerAuthorizationTest {

    @Test
    public void ownerCanRenameDevice() throws IOException {
        SmartHomeSystem system = createOwnerSystem();
        Room room = system.createRoom("Living Room");
        Device device = system.createDevice(room.getId(), "Lamp", DeviceType.SWITCH);

        system.renameDevice(device.getId(), "Ceiling Lamp");

        assertEquals("Owners should be allowed to rename devices",
                "Ceiling Lamp", system.findDeviceById(device.getId()).getName());
    }

    @Test
    public void ownerCanUpdateRule() throws IOException {
        SmartHomeSystem system = createOwnerSystem();
        Device device = createOwnerDevice(system);
        Rule rule = createOwnerRule(system, device);

        system.updateRule(
                rule.getId(),
                "Turn lamp off",
                RuleTriggerType.DEVICE_STATE_CHANGE,
                device.getId(),
                0.0,
                RuleActionType.SET_DEVICE_STATE,
                device.getId(),
                1.0
        );

        assertEquals("Owners should be allowed to update rules",
                "Turn lamp off", system.findRuleById(rule.getId()).getName());
    }

    @Test
    public void ownerCanRemoveRule() throws IOException {
        SmartHomeSystem system = createOwnerSystem();
        Device device = createOwnerDevice(system);
        Rule rule = createOwnerRule(system, device);

        assertTrue("Owners should be allowed to remove rules", system.removeRule(rule.getId()));
    }

    @Test
    public void ownerCanRemoveDevice() throws IOException {
        SmartHomeSystem system = createOwnerSystem();
        Room room = system.createRoom("Living Room");
        Device device = system.createDevice(room.getId(), "Lamp", DeviceType.SWITCH);

        assertTrue("Owners should be allowed to remove devices", system.removeDevice(device.getId()));
    }

    private SmartHomeSystem createOwnerSystem() throws IOException {
        SmartHomeSystem system = UserRoleTestSupport.createSystemWithTempDatabase();
        system.registerUser("owner@example.com", "password123");
        system.loginUser("owner@example.com", "password123");
        return system;
    }

    private Device createOwnerDevice(SmartHomeSystem system) {
        Room room = system.createRoom("Living Room");
        return system.createDevice(room.getId(), "Lamp", DeviceType.SWITCH);
    }

    private Rule createOwnerRule(SmartHomeSystem system, Device device) {
        return system.createRule(
                "Turn lamp on",
                RuleTriggerType.DEVICE_STATE_CHANGE,
                device.getId(),
                1.0,
                RuleActionType.SET_DEVICE_STATE,
                device.getId(),
                0.0
        );
    }
}
