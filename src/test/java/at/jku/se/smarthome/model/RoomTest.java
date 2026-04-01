package at.jku.se.smarthome.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RoomTest {
    @Test
    public void getId_returnsCorrectId() {
        Room room = new Room("r1", "Living Room");

        assertEquals("r1", room.getId());
    }
}
