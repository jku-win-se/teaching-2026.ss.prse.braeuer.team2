package at.jku.se.smarthome.model;

import java.util.ArrayList;
import java.util.List;

public class Room {

    private final String id;
    private String name;
    private final List<Device> devices;

    public Room(String id, String name) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Room id must not be empty");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Room name must not be empty");
        }

        this.id = id.trim();
        this.name = name.trim();
        this.devices = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void addDevice(Device device) {
        if (device == null) {
            throw new IllegalArgumentException("Device must not be null");
        }
        devices.add(device);
    }

    public Device findDeviceById(String deviceId) {
        for (Device device : devices) {
            if (device.getId().equals(deviceId)) {
                return device;
            }
        }
        return null;
    }

    public boolean removeDevice(String deviceId) {
        Device device = findDeviceById(deviceId);
        if (device == null) {
            return false;
        }
        return devices.remove(device);
    }

}
