package at.jku.se.smarthome.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

import at.jku.se.smarthome.repository.SQLiteHomeRepository;
import at.jku.se.smarthome.repository.SQLiteUserRepository;

@SuppressWarnings("PMD")
public class SmartHomeSystemTest {

    //Umbennene
    @Test
    public void renameDevice_changesName() {
        SmartHomeSystem system = new SmartHomeSystem();
        Room room = new Room("r1", "Living Room");
        Device device = new Device("d1", "Lamp", DeviceType.SWITCH);

        room.addDevice(device);
        system.addRoom(room);

        system.renameDevice("d1", "New Lamp");

        assertEquals("New Lamp", device.getName());
    }

    @Test
    public void renameDevice_deviceNotFound_throwsException() {
        SmartHomeSystem system = new SmartHomeSystem();

        assertThrows(IllegalArgumentException.class, () -> system.renameDevice("unknown", "New Name"));
    }

    //Entfernen
    @Test
    public void removeDevice_existingDevice_returnsTrue() {
        SmartHomeSystem system = new SmartHomeSystem();
        Room room = new Room("r1", "Living Room");
        Device device = new Device("d1", "Lamp", DeviceType.SWITCH);

        room.addDevice(device);
        system.addRoom(room);

        boolean removed = system.removeDevice("d1");

        assertTrue(removed);
        assertNull(system.findDeviceById("d1"));
    }

    @Test
    public void removeDevice_unknownDevice_returnsFalse() {
        SmartHomeSystem system = new SmartHomeSystem();
        Room room = new Room("r1", "Living Room");
        Device device = new Device("d1", "Lamp", DeviceType.SWITCH);

        room.addDevice(device);
        system.addRoom(room);

        boolean removed = system.removeDevice("unknown");

        assertFalse(removed);
        assertEquals(device, system.findDeviceById("d1"));
    }

    //Tests für Hilfsfunktionen für Rename und Remove
    @Test
    public void findDeviceById_existingDevice_returnsDevice() {
        SmartHomeSystem system = new SmartHomeSystem();
        Room room = new Room("r1", "Living Room");
        Device device = new Device("d1", "Lamp", DeviceType.SWITCH);

        room.addDevice(device);
        system.addRoom(room);

        Device foundDevice = system.findDeviceById("d1");

        assertEquals(device, foundDevice);
    }

    @Test
    public void findDeviceById_unknownDevice_returnsNull() {
        SmartHomeSystem system = new SmartHomeSystem();
        Room room = new Room("r1", "Living Room");
        Device device = new Device("d1", "Lamp", DeviceType.SWITCH);

        room.addDevice(device);
        system.addRoom(room);

        Device foundDevice = system.findDeviceById("unknown");

        assertNull(foundDevice);
    }

    @Test
    public void addRoom_nullRoom_throwsException() {
        SmartHomeSystem system = new SmartHomeSystem();

        assertThrows(IllegalArgumentException.class, () -> system.addRoom(null));
    }

    @Test
    public void createRoom_loggedInUser_addsRoomToRoomList() {
        SmartHomeSystem system = new SmartHomeSystem();
        system.registerUser("room-owner@example.com", "password123");
        system.loginUser("room-owner@example.com", "password123");

        Room room = system.createRoom("Kitchen");

        assertEquals("Kitchen", room.getName());
        assertEquals(1, system.getRooms().size());
        assertEquals(room.getId(), system.getRooms().get(0).getId());
    }

    @Test
    public void createRoom_withoutLogin_throwsException() {
        SmartHomeSystem system = new SmartHomeSystem();

        assertThrows(IllegalStateException.class, () -> system.createRoom("Kitchen"));
    }

    @Test
    public void renameRoom_existingRoom_changesName() {
        SmartHomeSystem system = new SmartHomeSystem();
        system.registerUser("room-owner@example.com", "password123");
        system.loginUser("room-owner@example.com", "password123");
        Room room = system.createRoom("Living Room");

        system.renameRoom(room.getId(), "Bedroom");

        assertEquals("Bedroom", system.findRoomById(room.getId()).getName());
    }

    @Test
    public void renameRoom_unknownRoom_throwsException() {
        SmartHomeSystem system = new SmartHomeSystem();
        system.registerUser("room-owner@example.com", "password123");
        system.loginUser("room-owner@example.com", "password123");

        assertThrows(IllegalArgumentException.class, () -> system.renameRoom("missing", "Bedroom"));
    }

    @Test
    public void removeRoom_existingRoom_returnsTrueAndRemovesRoom() {
        SmartHomeSystem system = new SmartHomeSystem();
        system.registerUser("room-owner@example.com", "password123");
        system.loginUser("room-owner@example.com", "password123");
        Room room = system.createRoom("Living Room");

        boolean removed = system.removeRoom(room.getId());

        assertTrue(removed);
        assertNull(system.findRoomById(room.getId()));
        assertTrue(system.getRooms().isEmpty());
    }

    @Test
    public void removeRoom_unknownRoom_returnsFalse() {
        SmartHomeSystem system = new SmartHomeSystem();
        system.registerUser("room-owner@example.com", "password123");
        system.loginUser("room-owner@example.com", "password123");

        assertFalse(system.removeRoom("missing"));
    }

    @Test
    public void roomLists_areIsolatedPerAuthenticatedUser() {
        SmartHomeSystem system = new SmartHomeSystem();
        system.registerUser("owner1@example.com", "password123");
        system.registerUser("owner2@example.com", "password123");

        system.loginUser("owner1@example.com", "password123");
        system.createRoom("Kitchen");
        system.logoutUser();

        system.loginUser("owner2@example.com", "password123");
        assertTrue(system.getRooms().isEmpty());
    }

    @Test
    public void createDevice_validInput_addsDeviceToSelectedRoom() {
        SmartHomeSystem system = new SmartHomeSystem();
        system.registerUser("owner@example.com", "password123");
        system.loginUser("owner@example.com", "password123");
        Room room = system.createRoom("Living Room");

        Device device = system.createDevice(room.getId(), "Ceiling Light", DeviceType.SWITCH);

        assertNotNull(device);
        assertEquals("Ceiling Light", device.getName());
        assertEquals(DeviceType.SWITCH, device.getType());
        assertEquals(1, room.getDevices().size());
        assertEquals(device.getId(), room.getDevices().get(0).getId());
    }

    @Test
    public void createDevice_unknownRoom_throwsException() {
        SmartHomeSystem system = new SmartHomeSystem();
        system.registerUser("owner@example.com", "password123");
        system.loginUser("owner@example.com", "password123");

        assertThrows(IllegalArgumentException.class,
                () -> system.createDevice("missing-room", "Thermostat", DeviceType.THERMOSTAT));
    }

    @Test
    public void createDevice_withoutLogin_throwsException() {
        SmartHomeSystem system = new SmartHomeSystem();

        assertThrows(IllegalStateException.class,
                () -> system.createDevice("room-id", "Sensor", DeviceType.SENSOR));
    }

    @Test
    public void createDevice_blankName_throwsException() {
        SmartHomeSystem system = new SmartHomeSystem();
        system.registerUser("owner@example.com", "password123");
        system.loginUser("owner@example.com", "password123");
        Room room = system.createRoom("Kitchen");

        assertThrows(IllegalArgumentException.class,
                () -> system.createDevice(room.getId(), "   ", DeviceType.DIMMER));
    }

    @Test
    public void roomsAndDevices_areLoadedAgainAfterRestart() throws IOException {
        Path databaseFile = Files.createTempFile("smarthome-room-device-persistence", ".db");
        databaseFile.toFile().deleteOnExit();
        String databaseUrl = "jdbc:sqlite:" + databaseFile;

        SmartHomeSystem firstSystem = new SmartHomeSystem(new SQLiteUserRepository(databaseUrl));
        firstSystem.registerUser("owner@example.com", "password123");
        firstSystem.loginUser("owner@example.com", "password123");
        Room room = firstSystem.createRoom("Living Room");
        Device device = firstSystem.createDevice(room.getId(), "Ceiling Light", DeviceType.DIMMER);
        firstSystem.updateDeviceValue(device.getId(), 75);
        firstSystem.logoutUser();

        SmartHomeSystem secondSystem = new SmartHomeSystem(new SQLiteUserRepository(databaseUrl));
        secondSystem.loginUser("owner@example.com", "password123");

        assertEquals(1, secondSystem.getRooms().size());
        Room persistedRoom = secondSystem.getRooms().get(0);
        assertEquals("Living Room", persistedRoom.getName());
        assertEquals(1, persistedRoom.getDevices().size());

        Device persistedDevice = persistedRoom.getDevices().get(0);
        assertEquals("Ceiling Light", persistedDevice.getName());
        assertEquals(DeviceType.DIMMER, persistedDevice.getType());
        assertEquals(75.0, persistedDevice.getValue(), 0.0001);
        assertTrue(persistedDevice.isOn());
    }

    @Test
    public void roomAndDeviceChanges_areStillVisibleAfterRestart() throws IOException {
        Path databaseFile = Files.createTempFile("smarthome-room-device-update", ".db");
        databaseFile.toFile().deleteOnExit();
        String databaseUrl = "jdbc:sqlite:" + databaseFile;

        SmartHomeSystem firstSystem = new SmartHomeSystem(new SQLiteUserRepository(databaseUrl));
        firstSystem.registerUser("owner@example.com", "password123");
        firstSystem.loginUser("owner@example.com", "password123");
        Room room = firstSystem.createRoom("Living Room");
        Device device = firstSystem.createDevice(room.getId(), "Lamp", DeviceType.SWITCH);
        firstSystem.renameRoom(room.getId(), "Bedroom");
        firstSystem.renameDevice(device.getId(), "Bedside Lamp");
        firstSystem.toggleDevice(device.getId());
        firstSystem.logoutUser();

        SmartHomeSystem secondSystem = new SmartHomeSystem(new SQLiteUserRepository(databaseUrl));
        secondSystem.loginUser("owner@example.com", "password123");

        Room persistedRoom = secondSystem.getRooms().get(0);
        Device persistedDevice = persistedRoom.getDevices().get(0);
        assertEquals("Bedroom", persistedRoom.getName());
        assertEquals("Bedside Lamp", persistedDevice.getName());
        assertTrue(persistedDevice.isOn());
    }

    @Test
    public void deletedRoomsAndDevices_stayDeletedAfterRestart() throws IOException {
        Path databaseFile = Files.createTempFile("smarthome-room-device-delete", ".db");
        databaseFile.toFile().deleteOnExit();
        String databaseUrl = "jdbc:sqlite:" + databaseFile;

        SmartHomeSystem firstSystem = new SmartHomeSystem(new SQLiteUserRepository(databaseUrl));
        firstSystem.registerUser("owner@example.com", "password123");
        firstSystem.loginUser("owner@example.com", "password123");
        Room room = firstSystem.createRoom("Office");
        Device device = firstSystem.createDevice(room.getId(), "Sensor", DeviceType.SENSOR);
        firstSystem.removeDevice(device.getId());
        firstSystem.removeRoom(room.getId());
        firstSystem.logoutUser();

        SmartHomeSystem secondSystem = new SmartHomeSystem(new SQLiteUserRepository(databaseUrl));
        secondSystem.loginUser("owner@example.com", "password123");

        assertTrue(secondSystem.getRooms().isEmpty());
    }

    @Test
    public void manualStateChange_createsActivityLogEntryWithUserActor() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-04-25T10:15:30Z"), ZoneOffset.UTC);
        SmartHomeSystem system = new SmartHomeSystem(
                new at.jku.se.smarthome.repository.InMemoryUserRepository(),
                new at.jku.se.smarthome.repository.InMemoryHomeRepository(),
                fixedClock
        );
        system.registerUser("owner@example.com", "password123");
        system.loginUser("owner@example.com", "password123");
        Room room = system.createRoom("Living Room");
        Device device = system.createDevice(room.getId(), "Lamp", DeviceType.SWITCH);

        system.toggleDevice(device.getId());

        assertEquals(1, system.getActivityLog().size());
        ActivityLogEntry entry = system.getActivityLog().get(0);
        assertEquals(Instant.parse("2026-04-25T10:15:30Z"), entry.getTimestamp());
        assertEquals(device.getId(), entry.getDeviceId());
        assertEquals("Lamp", entry.getDeviceName());
        assertEquals(ActivityActorType.USER, entry.getActorType());
        assertEquals("owner@example.com", entry.getActorName());
        assertEquals("Off", entry.getPreviousState());
        assertEquals("On", entry.getNewState());
    }

    @Test
    public void automatedStateChange_createsActivityLogEntryWithRuleActor() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-04-25T11:00:00Z"), ZoneOffset.UTC);
        SmartHomeSystem system = new SmartHomeSystem(
                new at.jku.se.smarthome.repository.InMemoryUserRepository(),
                new at.jku.se.smarthome.repository.InMemoryHomeRepository(),
                fixedClock
        );
        system.registerUser("owner@example.com", "password123");
        system.loginUser("owner@example.com", "password123");
        Room room = system.createRoom("Living Room");
        Device device = system.createDevice(room.getId(), "Thermostat", DeviceType.THERMOSTAT);

        system.updateDeviceValueByRule(device.getId(), 23.5, "Morning Heating Rule");

        assertEquals(1, system.getActivityLog().size());
        ActivityLogEntry entry = system.getActivityLog().get(0);
        assertEquals(Instant.parse("2026-04-25T11:00:00Z"), entry.getTimestamp());
        assertEquals(device.getId(), entry.getDeviceId());
        assertEquals("Thermostat", entry.getDeviceName());
        assertEquals(ActivityActorType.RULE, entry.getActorType());
        assertEquals("Morning Heating Rule", entry.getActorName());
        assertEquals("20 °C", entry.getPreviousState());
        assertEquals("23.5 °C", entry.getNewState());
    }

    @Test
    public void manualAndAutomatedStateChanges_areBothRecorded() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-04-25T12:00:00Z"), ZoneOffset.UTC);
        SmartHomeSystem system = new SmartHomeSystem(
                new at.jku.se.smarthome.repository.InMemoryUserRepository(),
                new at.jku.se.smarthome.repository.InMemoryHomeRepository(),
                fixedClock
        );
        system.registerUser("owner@example.com", "password123");
        system.loginUser("owner@example.com", "password123");
        Room room = system.createRoom("Living Room");
        Device device = system.createDevice(room.getId(), "Blind", DeviceType.BLIND);

        system.updateDeviceValue(device.getId(), 100);
        system.updateDeviceValueByRule(device.getId(), 0, "Sunset Rule");

        assertEquals(2, system.getActivityLog().size());
        assertEquals(ActivityActorType.USER, system.getActivityLog().get(0).getActorType());
        assertEquals(ActivityActorType.RULE, system.getActivityLog().get(1).getActorType());
        assertEquals("Closed", system.getActivityLog().get(0).getPreviousState());
        assertEquals("Open", system.getActivityLog().get(0).getNewState());
        assertEquals("Open", system.getActivityLog().get(1).getPreviousState());
        assertEquals("Closed", system.getActivityLog().get(1).getNewState());
    }

    @Test
    public void activityLogEntries_areLoadedAgainAfterRestart() throws IOException {
        Path databaseFile = Files.createTempFile("smarthome-activity-log", ".db");
        databaseFile.toFile().deleteOnExit();
        String databaseUrl = "jdbc:sqlite:" + databaseFile;
        Clock fixedClock = Clock.fixed(Instant.parse("2026-04-25T13:00:00Z"), ZoneOffset.UTC);

        SmartHomeSystem firstSystem = new SmartHomeSystem(
                new SQLiteUserRepository(databaseUrl),
                new SQLiteHomeRepository(databaseUrl),
                fixedClock
        );
        firstSystem.registerUser("owner@example.com", "password123");
        firstSystem.loginUser("owner@example.com", "password123");
        Room room = firstSystem.createRoom("Living Room");
        Device device = firstSystem.createDevice(room.getId(), "Lamp", DeviceType.SWITCH);
        firstSystem.toggleDevice(device.getId());
        firstSystem.toggleDeviceByRule(device.getId(), "Night Rule");
        firstSystem.logoutUser();

        SmartHomeSystem secondSystem = new SmartHomeSystem(
                new SQLiteUserRepository(databaseUrl),
                new SQLiteHomeRepository(databaseUrl),
                Clock.systemUTC()
        );
        secondSystem.loginUser("owner@example.com", "password123");

        assertEquals(2, secondSystem.getActivityLog().size());
        assertEquals("owner@example.com", secondSystem.getActivityLog().get(0).getActorName());
        assertEquals(ActivityActorType.USER, secondSystem.getActivityLog().get(0).getActorType());
        assertEquals("Off", secondSystem.getActivityLog().get(0).getPreviousState());
        assertEquals("On", secondSystem.getActivityLog().get(0).getNewState());
        assertEquals("Night Rule", secondSystem.getActivityLog().get(1).getActorName());
        assertEquals(ActivityActorType.RULE, secondSystem.getActivityLog().get(1).getActorType());
        assertEquals("On", secondSystem.getActivityLog().get(1).getPreviousState());
        assertEquals("Off", secondSystem.getActivityLog().get(1).getNewState());
    }
}
