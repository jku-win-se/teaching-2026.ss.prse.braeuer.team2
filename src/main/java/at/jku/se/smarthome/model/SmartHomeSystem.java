package at.jku.se.smarthome.model;

import java.util.ArrayList;
import java.util.List;

public class SmartHomeSystem {
    private final List<Room> rooms;

    public SmartHomeSystem() {
        this.rooms = new ArrayList<>();
    }

    public void addRoom(Room room) {
        if (room == null) {
            throw new IllegalArgumentException("Room must not be null");
        }
        rooms.add(room);
    }

    public void renameDevice(String deviceId, String newName) {
        Device device = findDeviceById(deviceId);
        if (device == null) {
            throw new IllegalArgumentException("Device not found");
        }
        device.rename(newName);
    }

    public boolean removeDevice(String deviceId) {
        for (Room room : rooms) {
            if (room.removeDevice(deviceId)) {
                return true;
            }
        }
        return false;
    }

    public Device findDeviceById(String deviceId) {
        for (Room room : rooms) {
            Device device = room.findDeviceById(deviceId);
            if (device != null) {
                return device;
            }
        }
        return null;
    }
}
