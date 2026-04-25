package at.jku.se.smarthome.repository;

import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.model.Room;
import at.jku.se.smarthome.model.ActivityLogEntry;
import at.jku.se.smarthome.model.Schedule;

import java.util.ArrayList;
import java.util.List;

/**
 * No-op in-memory repository for rooms and devices.
 */
@SuppressWarnings("PMD")
public class InMemoryHomeRepository implements HomeRepository {
    @Override
    public List<Room> findRoomsByUserEmail(String userEmail) {
        return new ArrayList<>();
    }

    @Override
    public List<ActivityLogEntry> findActivityLogByUserEmail(String userEmail) {
        return new ArrayList<>();
    }

    @Override
    public List<Schedule> findSchedulesByUserEmail(String userEmail) {
        return new ArrayList<>();
    }

    @Override
    public void saveRoom(String userEmail, Room room) {
    }

    @Override
    public void updateRoom(Room room) {
    }

    @Override
    public void deleteRoom(String roomId) {
    }

    @Override
    public void deleteRoomsByUserEmail(String userEmail) {
    }

    @Override
    public void saveDevice(String roomId, Device device) {
    }

    @Override
    public void updateDevice(Device device) {
    }

    @Override
    public void saveSchedule(String userEmail, Schedule schedule) {
    }

    @Override
    public void updateSchedule(Schedule schedule) {
    }

    @Override
    public void saveActivityLogEntry(String userEmail, ActivityLogEntry entry) {
    }

    @Override
    public void deleteSchedule(String scheduleId) {
    }

    @Override
    public void deleteDevice(String deviceId) {
    }
}
