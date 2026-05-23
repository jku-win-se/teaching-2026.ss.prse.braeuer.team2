package at.jku.se.smarthome.repository;

import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.model.Room;
import at.jku.se.smarthome.model.ActivityLogEntry;
import at.jku.se.smarthome.model.Rule;
import at.jku.se.smarthome.model.Schedule;
import at.jku.se.smarthome.model.Scene;
import at.jku.se.smarthome.model.VacationMode;

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
    public List<Rule> findRulesByUserEmail(String userEmail) {
        return new ArrayList<>();
    }

    @Override
    public List<Scene> findScenesByUserEmail(String userEmail) {
        return new ArrayList<>();
    }

    @Override
    public VacationMode findVacationModeByUserEmail(String userEmail) {
        return null;
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
    public void saveRule(String userEmail, Rule rule) {
    }

    @Override
    public void saveScene(String userEmail, Scene scene) {
    }

    @Override
    public void saveVacationMode(String userEmail, VacationMode vacationMode) {
    }

    @Override
    public void updateRule(Rule rule) {
    }

    @Override
    public void updateScene(Scene scene) {
    }

    @Override
    public void saveActivityLogEntry(String userEmail, ActivityLogEntry entry) {
    }

    @Override
    public void deleteSchedule(String scheduleId) {
    }

    @Override
    public void deleteRule(String ruleId) {
    }

    @Override
    public void deleteScene(String sceneId) {
    }

    @Override
    public void deleteScenesByUserEmail(String userEmail) {
    }

    @Override
    public void deleteVacationModeByUserEmail(String userEmail) {
    }

    @Override
    public void deleteDevice(String deviceId) {
    }
}
