package at.jku.se.smarthome.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

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
}
