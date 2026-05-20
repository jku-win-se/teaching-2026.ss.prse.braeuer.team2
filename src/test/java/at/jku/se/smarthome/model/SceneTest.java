package at.jku.se.smarthome.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import at.jku.se.smarthome.repository.InMemoryHomeRepository;
import at.jku.se.smarthome.repository.InMemoryUserRepository;
import at.jku.se.smarthome.repository.SQLiteHomeRepository;
import at.jku.se.smarthome.repository.SQLiteUserRepository;

@SuppressWarnings({
        "PMD.CommentRequired",
        "PMD.AtLeastOneConstructor",
        "PMD.JUnitAssertionsShouldIncludeMessage"
})
public class SceneTest {

    @Test
    public void createSceneWithMultipleDeviceStatesStoresScene() {
        SmartHomeSystem system = createLoggedInOwnerSystem();
        Room room = system.createRoom("Living Room");
        Device lamp = system.createDevice(room.getId(), "Lamp", DeviceType.SWITCH);
        Device dimmer = system.createDevice(room.getId(), "Dimmer", DeviceType.DIMMER);

        Scene scene = system.createScene("Movie Night", List.of(
                new SceneDeviceState(lamp.getId(), 1.0),
                new SceneDeviceState(dimmer.getId(), 30.0)
        ));

        Assert.assertEquals(1, system.getScenes().size());
        Assert.assertEquals("Movie Night", scene.getName());
        Assert.assertEquals(2, scene.getDeviceStates().size());
    }

    @Test
    public void activateSceneAppliesAllConfiguredDeviceStates() {
        SmartHomeSystem system = createLoggedInOwnerSystem();
        Room room = system.createRoom("Living Room");
        Device lamp = system.createDevice(room.getId(), "Lamp", DeviceType.SWITCH);
        Device dimmer = system.createDevice(room.getId(), "Dimmer", DeviceType.DIMMER);
        system.updateDeviceValue(dimmer.getId(), 80.0);
        Scene scene = system.createScene("Movie Night", List.of(
                new SceneDeviceState(lamp.getId(), 1.0),
                new SceneDeviceState(dimmer.getId(), 30.0)
        ));

        system.activateScene(scene.getId());

        Assert.assertTrue(lamp.isOn());
        Assert.assertEquals(30.0, dimmer.getValue(), 0.0001);
        Assert.assertTrue(system.isSceneActive(scene.getId()));
        Assert.assertEquals("Scene \"Movie Night\" activated successfully.",
                system.getRuleNotifications().get(0).getMessage());
    }

    @Test
    public void deactivateSceneRestoresPreviouslyCapturedDeviceStates() {
        SmartHomeSystem system = createLoggedInOwnerSystem();
        Room room = system.createRoom("Living Room");
        Device lamp = system.createDevice(room.getId(), "Lamp", DeviceType.SWITCH);
        Device dimmer = system.createDevice(room.getId(), "Dimmer", DeviceType.DIMMER);
        system.updateDeviceValue(dimmer.getId(), 80.0);
        Scene scene = system.createScene("Movie Night", List.of(
                new SceneDeviceState(lamp.getId(), 1.0),
                new SceneDeviceState(dimmer.getId(), 30.0)
        ));

        system.activateScene(scene.getId());
        system.deactivateScene(scene.getId());

        Assert.assertFalse(lamp.isOn());
        Assert.assertEquals(80.0, dimmer.getValue(), 0.0001);
        Assert.assertFalse(system.isSceneActive(scene.getId()));
        Assert.assertEquals("Scene \"Movie Night\" deactivated successfully.",
                system.getRuleNotifications().get(1).getMessage());
    }

    @Test
    public void updateSceneReplacesNameAndDeviceStates() {
        SmartHomeSystem system = createLoggedInOwnerSystem();
        Room room = system.createRoom("Living Room");
        Device lamp = system.createDevice(room.getId(), "Lamp", DeviceType.SWITCH);
        Device dimmer = system.createDevice(room.getId(), "Dimmer", DeviceType.DIMMER);
        Scene scene = system.createScene("Movie Night", List.of(new SceneDeviceState(lamp.getId(), 1.0)));

        system.updateScene(scene.getId(), "Reading", List.of(new SceneDeviceState(dimmer.getId(), 70.0)));

        Assert.assertEquals("Reading", scene.getName());
        Assert.assertEquals(1, scene.getDeviceStates().size());
        Assert.assertEquals(dimmer.getId(), scene.getDeviceStates().get(0).getDeviceId());
    }

    @Test
    public void removeSceneDeletesExistingScene() {
        SmartHomeSystem system = createLoggedInOwnerSystem();
        Room room = system.createRoom("Living Room");
        Device lamp = system.createDevice(room.getId(), "Lamp", DeviceType.SWITCH);
        Scene scene = system.createScene("Movie Night", List.of(new SceneDeviceState(lamp.getId(), 1.0)));

        Assert.assertTrue(system.removeScene(scene.getId()));

        Assert.assertTrue(system.getScenes().isEmpty());
        Assert.assertFalse(system.removeScene(scene.getId()));
    }

    @Test
    public void createSceneRejectsInvalidDeviceStateValue() {
        SmartHomeSystem system = createLoggedInOwnerSystem();
        Room room = system.createRoom("Living Room");
        Device lamp = system.createDevice(room.getId(), "Lamp", DeviceType.SWITCH);

        Assert.assertThrows(
                IllegalArgumentException.class,
                () -> system.createScene("Invalid", List.of(new SceneDeviceState(lamp.getId(), 50.0)))
        );
    }

    @Test
    public void createSceneRejectsDuplicateDeviceState() {
        SmartHomeSystem system = createLoggedInOwnerSystem();
        Room room = system.createRoom("Living Room");
        Device lamp = system.createDevice(room.getId(), "Lamp", DeviceType.SWITCH);

        Assert.assertThrows(
                IllegalArgumentException.class,
                () -> system.createScene("Duplicate", List.of(
                        new SceneDeviceState(lamp.getId(), 0.0),
                        new SceneDeviceState(lamp.getId(), 1.0)
                ))
        );
    }

    @Test
    public void memberCanActivateButCannotManageScenes() throws IOException {
        Path databaseFile = Files.createTempFile("smarthome-scene-role", ".db");
        databaseFile.toFile().deleteOnExit();
        String databaseUrl = "jdbc:sqlite:" + databaseFile;
        SQLiteUserRepository userRepository = new SQLiteUserRepository(databaseUrl);
        SQLiteHomeRepository homeRepository = new SQLiteHomeRepository(databaseUrl);
        SmartHomeSystem ownerSystem = new SmartHomeSystem(userRepository, homeRepository);
        ownerSystem.registerUser("member@example.com", "password123", UserRole.MEMBER);

        Room room = new Room("room-1", "Living Room");
        Device lamp = new Device("device-1", "Lamp", DeviceType.SWITCH);
        room.addDevice(lamp);
        homeRepository.saveRoom("member@example.com", room);
        homeRepository.saveDevice(room.getId(), lamp);
        homeRepository.saveScene("member@example.com", new Scene(
                "scene-1",
                "Lights On",
                List.of(new SceneDeviceState(lamp.getId(), 1.0))
        ));

        ownerSystem.loginUser("member@example.com", "password123");
        ownerSystem.activateScene("scene-1");

        Assert.assertTrue(ownerSystem.findDeviceById(lamp.getId()).isOn());
        Assert.assertThrows(
                IllegalStateException.class,
                () -> ownerSystem.createScene("Blocked", List.of(new SceneDeviceState(lamp.getId(), 0.0)))
        );
    }

    @Test
    public void scenesAreLoadedAgainAfterRestart() throws IOException {
        Path databaseFile = Files.createTempFile("smarthome-scene", ".db");
        databaseFile.toFile().deleteOnExit();
        String databaseUrl = "jdbc:sqlite:" + databaseFile;

        SmartHomeSystem firstSystem = new SmartHomeSystem(
                new SQLiteUserRepository(databaseUrl),
                new SQLiteHomeRepository(databaseUrl)
        );
        firstSystem.registerUser("owner@example.com", "password123");
        firstSystem.loginUser("owner@example.com", "password123");
        Room room = firstSystem.createRoom("Living Room");
        Device lamp = firstSystem.createDevice(room.getId(), "Lamp", DeviceType.SWITCH);
        firstSystem.createScene("Movie Night", List.of(new SceneDeviceState(lamp.getId(), 1.0)));
        firstSystem.logoutUser();

        SmartHomeSystem secondSystem = new SmartHomeSystem(
                new SQLiteUserRepository(databaseUrl),
                new SQLiteHomeRepository(databaseUrl)
        );
        secondSystem.loginUser("owner@example.com", "password123");

        Assert.assertEquals(1, secondSystem.getScenes().size());
        Assert.assertEquals("Movie Night", secondSystem.getScenes().get(0).getName());
        Assert.assertEquals(lamp.getId(), secondSystem.getScenes().get(0).getDeviceStates().get(0).getDeviceId());
    }

    private SmartHomeSystem createLoggedInOwnerSystem() {
        SmartHomeSystem system = new SmartHomeSystem(new InMemoryUserRepository(), new InMemoryHomeRepository());
        system.registerUser("owner@example.com", "password123");
        system.loginUser("owner@example.com", "password123");
        return system;
    }
}
