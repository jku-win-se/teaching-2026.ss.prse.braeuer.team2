package at.jku.se.smarthome.repository;

import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.model.Room;
import at.jku.se.smarthome.model.ActivityLogEntry;

import java.util.List;

/**
 * Defines persistence operations for rooms and devices owned by a user.
 */
@SuppressWarnings("PMD")
public interface HomeRepository {
    /**
     * Loads all rooms including their devices for the given user.
     *
     * @param userEmail the owning user's email address
     * @return the stored rooms for the user
     */
    List<Room> findRoomsByUserEmail(String userEmail);

    /**
     * Loads all activity log entries for the given user.
     *
     * @param userEmail the owning user's email address
     * @return the stored activity log entries for the user
     */
    List<ActivityLogEntry> findActivityLogByUserEmail(String userEmail);

    /**
     * Stores a room for the given user.
     *
     * @param userEmail the owning user's email address
     * @param room the room to store
     */
    void saveRoom(String userEmail, Room room);

    /**
     * Updates a stored room.
     *
     * @param room the room to update
     */
    void updateRoom(Room room);

    /**
     * Deletes a stored room and its devices.
     *
     * @param roomId the room id
     */
    void deleteRoom(String roomId);

    /**
     * Deletes all stored rooms for the given user.
     *
     * @param userEmail the owning user's email address
     */
    void deleteRoomsByUserEmail(String userEmail);

    /**
     * Stores a device in the given room.
     *
     * @param roomId the target room id
     * @param device the device to store
     */
    void saveDevice(String roomId, Device device);

    /**
     * Updates a stored device.
     *
     * @param device the device to update
     */
    void updateDevice(Device device);

    /**
     * Stores an activity log entry for the given user.
     *
     * @param userEmail the owning user's email address
     * @param entry the log entry to store
     */
    void saveActivityLogEntry(String userEmail, ActivityLogEntry entry);

    /**
     * Deletes a stored device.
     *
     * @param deviceId the device id
     */
    void deleteDevice(String deviceId);
}
