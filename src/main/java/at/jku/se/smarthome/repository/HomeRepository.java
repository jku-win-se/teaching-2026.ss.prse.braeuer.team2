package at.jku.se.smarthome.repository;

import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.model.Room;

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
     * Deletes a stored device.
     *
     * @param deviceId the device id
     */
    void deleteDevice(String deviceId);
}
