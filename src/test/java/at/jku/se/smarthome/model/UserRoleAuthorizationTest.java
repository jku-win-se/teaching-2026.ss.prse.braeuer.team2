package at.jku.se.smarthome.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

import at.jku.se.smarthome.repository.SQLiteHomeRepository;
import at.jku.se.smarthome.repository.SQLiteUserRepository;

@SuppressWarnings({
        "PMD.CommentRequired",
        "PMD.AtLeastOneConstructor"
})
public class UserRoleAuthorizationTest {

    @Test
    public void registerUser_withoutExplicitRole_createsOwner() throws IOException {
        SmartHomeSystem system = createSystemWithTempDatabase();

        User user = system.registerUser("owner@example.com", "password123");

        assertEquals(UserRole.OWNER, user.getRole());
    }

    @Test
    public void registerUser_withMemberRole_createsMember() throws IOException {
        SmartHomeSystem system = createSystemWithTempDatabase();

        User user = system.registerUser("member@example.com", "password123", UserRole.MEMBER);

        assertEquals(UserRole.MEMBER, user.getRole());
    }

    @Test
    public void loginUser_persistsUserRole() throws IOException {
        SmartHomeSystem system = createSystemWithTempDatabase();
        system.registerUser("member@example.com", "password123", UserRole.MEMBER);

        User loggedInUser = system.loginUser("member@example.com", "password123");

        assertEquals(UserRole.MEMBER, loggedInUser.getRole());
        assertTrue(system.isCurrentUserMember());
        assertFalse(system.isCurrentUserOwner());
    }

    @Test
    public void owner_canManageDevicesAndRules() throws IOException {
        SmartHomeSystem system = createSystemWithTempDatabase();
        system.registerUser("owner@example.com", "password123");
        system.loginUser("owner@example.com", "password123");

        Room room = system.createRoom("Living Room");
        Device device = system.createDevice(room.getId(), "Lamp", DeviceType.SWITCH);
        Rule rule = system.createRule(
                "Turn lamp on",
                RuleTriggerType.DEVICE_STATE_CHANGE,
                device.getId(),
                1.0,
                RuleActionType.SET_DEVICE_STATE,
                device.getId(),
                0.0
        );

        system.renameDevice(device.getId(), "Ceiling Lamp");
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

        assertEquals("Ceiling Lamp", system.findDeviceById(device.getId()).getName());
        assertEquals("Turn lamp off", system.findRuleById(rule.getId()).getName());
        assertTrue(system.removeRule(rule.getId()));
        assertTrue(system.removeDevice(device.getId()));
    }

    @Test
    public void member_canControlExistingDevice() throws IOException {
        TestContext context = createMemberContextWithDevice();

        context.system.loginUser("member@example.com", "password123");
        Device device = context.system.findDeviceById("device-1");
        context.system.toggleDevice(device.getId());

        assertTrue(context.system.findDeviceById(device.getId()).isOn());
    }

    @Test
    public void member_cannotManageRoomsOrDevices() throws IOException {
        TestContext context = createMemberContextWithDevice();

        context.system.loginUser("member@example.com", "password123");

        assertThrows(IllegalStateException.class, () -> context.system.createRoom("Kitchen"));
        assertThrows(IllegalStateException.class,
                () -> context.system.createDevice("room-1", "Thermostat", DeviceType.THERMOSTAT));
        assertThrows(IllegalStateException.class, () -> context.system.renameDevice("device-1", "New Lamp"));
        assertThrows(IllegalStateException.class, () -> context.system.removeDevice("device-1"));
    }

    @Test
    public void member_cannotManageRules() throws IOException {
        TestContext context = createMemberContextWithDevice();
        Rule rule = new Rule(
                "rule-1",
                "Existing rule",
                new RuleTrigger(RuleTriggerType.DEVICE_STATE_CHANGE, "device-1", 1.0),
                new RuleAction(RuleActionType.SET_DEVICE_STATE, "device-1", 0.0)
        );
        context.homeRepository.saveRule("member@example.com", rule);

        context.system.loginUser("member@example.com", "password123");

        assertThrows(IllegalStateException.class, () -> context.system.createRule(
                "New rule",
                RuleTriggerType.DEVICE_STATE_CHANGE,
                "device-1",
                1.0,
                RuleActionType.SET_DEVICE_STATE,
                "device-1",
                0.0
        ));
        assertThrows(IllegalStateException.class, () -> context.system.updateRule(
                "rule-1",
                "Changed rule",
                RuleTriggerType.DEVICE_STATE_CHANGE,
                "device-1",
                0.0,
                RuleActionType.SET_DEVICE_STATE,
                "device-1",
                1.0
        ));
        assertThrows(IllegalStateException.class, () -> context.system.removeRule("rule-1"));
    }

    private SmartHomeSystem createSystemWithTempDatabase() throws IOException {
        Path databaseFile = Files.createTempFile("smarthome-role-auth-test", ".db");
        databaseFile.toFile().deleteOnExit();
        String databaseUrl = "jdbc:sqlite:" + databaseFile;
        return new SmartHomeSystem(new SQLiteUserRepository(databaseUrl), new SQLiteHomeRepository(databaseUrl));
    }

    private TestContext createMemberContextWithDevice() throws IOException {
        Path databaseFile = Files.createTempFile("smarthome-member-auth-test", ".db");
        databaseFile.toFile().deleteOnExit();
        String databaseUrl = "jdbc:sqlite:" + databaseFile;
        SQLiteUserRepository userRepository = new SQLiteUserRepository(databaseUrl);
        SQLiteHomeRepository homeRepository = new SQLiteHomeRepository(databaseUrl);
        SmartHomeSystem system = new SmartHomeSystem(userRepository, homeRepository);

        system.registerUser("member@example.com", "password123", UserRole.MEMBER);
        Room room = new Room("room-1", "Living Room");
        Device device = new Device("device-1", "Lamp", DeviceType.SWITCH);
        room.addDevice(device);
        homeRepository.saveRoom("member@example.com", room);
        homeRepository.saveDevice(room.getId(), device);

        return new TestContext(system, homeRepository);
    }

    private record TestContext(SmartHomeSystem system, SQLiteHomeRepository homeRepository) {
    }
}
