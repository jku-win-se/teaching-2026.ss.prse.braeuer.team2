package at.jku.se.smarthome.repository;

import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.model.DeviceType;
import at.jku.se.smarthome.model.Room;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SQLite-backed repository for rooms and devices.
 */
@SuppressWarnings("PMD")
public class SQLiteHomeRepository implements HomeRepository {

    private final String databaseUrl;

    /**
     * Creates a repository backed by the given SQLite JDBC URL and initializes the schema.
     *
     * @param databaseUrl the JDBC URL of the SQLite database
     */
    public SQLiteHomeRepository(String databaseUrl) {
        if (databaseUrl == null || databaseUrl.isBlank()) {
            throw new IllegalArgumentException("Database URL must not be blank");
        }

        this.databaseUrl = databaseUrl;
        initializeSchema();
    }

    @Override
    public List<Room> findRoomsByUserEmail(String userEmail) {
        String sql = """
                SELECT r.id AS room_id, r.name AS room_name,
                       d.id AS device_id, d.name AS device_name, d.type AS device_type,
                       d.is_on AS device_is_on, d.value AS device_value
                FROM rooms r
                LEFT JOIN devices d ON d.room_id = r.id
                WHERE r.user_email = ?
                ORDER BY r.name, d.name
                """;

        Map<String, Room> roomsById = new LinkedHashMap<>();

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userEmail);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String roomId = resultSet.getString("room_id");
                    Room room = roomsById.get(roomId);
                    if (room == null) {
                        room = new Room(roomId, resultSet.getString("room_name"));
                        roomsById.put(roomId, room);
                    }

                    String deviceId = resultSet.getString("device_id");
                    if (deviceId != null) {
                        room.addDevice(mapDevice(resultSet));
                    }
                }
            }

            return new ArrayList<>(roomsById.values());
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to query rooms", exception);
        }
    }

    @Override
    public void saveRoom(String userEmail, Room room) {
        String sql = "INSERT INTO rooms(id, user_email, name) VALUES(?, ?, ?)";

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, room.getId());
            statement.setString(2, userEmail);
            statement.setString(3, room.getName());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save room", exception);
        }
    }

    @Override
    public void updateRoom(Room room) {
        String sql = "UPDATE rooms SET name = ? WHERE id = ?";

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, room.getName());
            statement.setString(2, room.getId());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to update room", exception);
        }
    }

    @Override
    public void deleteRoom(String roomId) {
        String sql = "DELETE FROM rooms WHERE id = ?";

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, roomId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to delete room", exception);
        }
    }

    @Override
    public void deleteRoomsByUserEmail(String userEmail) {
        String sql = "DELETE FROM rooms WHERE user_email = ?";

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userEmail);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to delete user rooms", exception);
        }
    }

    @Override
    public void saveDevice(String roomId, Device device) {
        String sql = "INSERT INTO devices(id, room_id, name, type, is_on, value) VALUES(?, ?, ?, ?, ?, ?)";

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, device.getId());
            statement.setString(2, roomId);
            statement.setString(3, device.getName());
            statement.setString(4, device.getType().name());
            statement.setInt(5, device.isOn() ? 1 : 0);
            statement.setDouble(6, device.getValue());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save device", exception);
        }
    }

    @Override
    public void updateDevice(Device device) {
        String sql = "UPDATE devices SET name = ?, type = ?, is_on = ?, value = ? WHERE id = ?";

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, device.getName());
            statement.setString(2, device.getType().name());
            statement.setInt(3, device.isOn() ? 1 : 0);
            statement.setDouble(4, device.getValue());
            statement.setString(5, device.getId());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to update device", exception);
        }
    }

    @Override
    public void deleteDevice(String deviceId) {
        String sql = "DELETE FROM devices WHERE id = ?";

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, deviceId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to delete device", exception);
        }
    }

    private Device mapDevice(ResultSet resultSet) throws SQLException {
        Device device = new Device(
                resultSet.getString("device_id"),
                resultSet.getString("device_name"),
                DeviceType.valueOf(resultSet.getString("device_type"))
        );

        boolean isOn = resultSet.getInt("device_is_on") == 1;
        double value = resultSet.getDouble("device_value");

        switch (device.getType()) {
            case SWITCH -> {
                if (isOn) {
                    device.toggle();
                }
            }
            case DIMMER, THERMOSTAT, BLIND, SENSOR -> device.setValue(value);
        }

        return device;
    }

    private Connection openConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(databaseUrl);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    private void initializeSchema() {
        String createRoomsSql = """
                CREATE TABLE IF NOT EXISTS rooms (
                    id TEXT PRIMARY KEY,
                    user_email TEXT NOT NULL,
                    name TEXT NOT NULL,
                    FOREIGN KEY(user_email) REFERENCES users(email) ON DELETE CASCADE
                )
                """;
        String createDevicesSql = """
                CREATE TABLE IF NOT EXISTS devices (
                    id TEXT PRIMARY KEY,
                    room_id TEXT NOT NULL,
                    name TEXT NOT NULL,
                    type TEXT NOT NULL,
                    is_on INTEGER NOT NULL,
                    value REAL NOT NULL,
                    FOREIGN KEY(room_id) REFERENCES rooms(id) ON DELETE CASCADE
                )
                """;

        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(createRoomsSql);
            statement.execute(createDevicesSql);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to initialize home schema", exception);
        }
    }
}
