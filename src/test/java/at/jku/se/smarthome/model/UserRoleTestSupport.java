package at.jku.se.smarthome.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import at.jku.se.smarthome.repository.SQLiteHomeRepository;
import at.jku.se.smarthome.repository.SQLiteUserRepository;

@SuppressWarnings("PMD")
final class UserRoleTestSupport {

    private UserRoleTestSupport() {
    }

    static SmartHomeSystem createSystemWithTempDatabase() throws IOException {
        Path databaseFile = Files.createTempFile("smarthome-role-auth-test", ".db");
        databaseFile.toFile().deleteOnExit();
        String databaseUrl = "jdbc:sqlite:" + databaseFile;
        return new SmartHomeSystem(new SQLiteUserRepository(databaseUrl), new SQLiteHomeRepository(databaseUrl));
    }

    static RoleTestContext createMemberContextWithDevice() throws IOException {
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

        return new RoleTestContext(system, homeRepository);
    }

    static RoleTestContext createMemberContextWithRule() throws IOException {
        RoleTestContext context = createMemberContextWithDevice();
        Rule rule = new Rule(
                "rule-1",
                "Existing rule",
                new RuleTrigger(RuleTriggerType.DEVICE_STATE_CHANGE, "device-1", 1.0),
                new RuleAction(RuleActionType.SET_DEVICE_STATE, "device-1", 0.0)
        );
        context.homeRepository().saveRule("member@example.com", rule);
        return context;
    }

    record RoleTestContext(SmartHomeSystem system, SQLiteHomeRepository homeRepository) {
    }
}
