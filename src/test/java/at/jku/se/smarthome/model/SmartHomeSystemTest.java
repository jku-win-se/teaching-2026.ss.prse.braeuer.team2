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
}