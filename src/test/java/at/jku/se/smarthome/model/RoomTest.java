package at.jku.se.smarthome.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class RoomTest {
    @Test
    public void getId_returnsCorrectId() {
        Room room = new Room("r1", "Living Room");

        assertEquals("r1", room.getId());
    }

    @Test
    public void rename_changesRoomName() {
        Room room = new Room("r1", "Living Room");

        room.rename("Kitchen");

        assertEquals("Kitchen", room.getName());
    }

    @Test
    public void rename_blankName_throwsException() {
        Room room = new Room("r1", "Living Room");

        assertThrows(IllegalArgumentException.class, () -> room.rename("  "));
    }

    @Test
    public void addDevice_validDevice_assignsDeviceToRoom() {
        Room room = new Room("r1", "Living Room");
        Device device = new Device("d1", "Lamp", DeviceType.SWITCH);

        room.addDevice(device);

        assertEquals(1, room.getDevices().size());
        assertNotNull(room.findDeviceById("d1"));
    }
}
