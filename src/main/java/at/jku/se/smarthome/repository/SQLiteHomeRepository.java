package at.jku.se.smarthome.repository;

import at.jku.se.smarthome.model.ActivityActorType;
import at.jku.se.smarthome.model.ActivityLogEntry;
import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.model.DeviceType;
import at.jku.se.smarthome.model.Room;
import at.jku.se.smarthome.model.Rule;
import at.jku.se.smarthome.model.RuleAction;
import at.jku.se.smarthome.model.RuleActionType;
import at.jku.se.smarthome.model.RuleTrigger;
import at.jku.se.smarthome.model.RuleTriggerType;
import at.jku.se.smarthome.model.Schedule;
import at.jku.se.smarthome.model.ScheduleActionType;
import at.jku.se.smarthome.model.ThresholdOperator;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    public List<ActivityLogEntry> findActivityLogByUserEmail(String userEmail) {
        String sql = """
                SELECT occurred_at, device_id, device_name, actor_type, actor_name
                       , previous_state, new_state
                FROM activity_log
                WHERE user_email = ?
                ORDER BY occurred_at, id
                """;

        List<ActivityLogEntry> activityLog = new ArrayList<>();

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userEmail);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    activityLog.add(new ActivityLogEntry(
                            Instant.parse(resultSet.getString("occurred_at")),
                            resultSet.getString("device_id"),
                            resultSet.getString("device_name"),
                            ActivityActorType.valueOf(resultSet.getString("actor_type")),
                            resultSet.getString("actor_name"),
                            resultSet.getString("previous_state"),
                            resultSet.getString("new_state")
                    ));
                }
            }

            return activityLog;
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to query activity log", exception);
        }
    }

    @Override
    public List<Schedule> findSchedulesByUserEmail(String userEmail) {
        String sql = """
                SELECT id, name, device_id, action_type, target_value, execution_time, recurring_days, last_executed_on
                FROM schedules
                WHERE user_email = ?
                ORDER BY name, id
                """;

        List<Schedule> schedules = new ArrayList<>();

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userEmail);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    schedules.add(mapSchedule(resultSet));
                }
            }

            return schedules;
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to query schedules", exception);
        }
    }

    @Override
    public List<Rule> findRulesByUserEmail(String userEmail) {
        String sql = """
                SELECT id, name, trigger_type, trigger_device_id, trigger_expected_value,
                       threshold_operator, trigger_time, last_triggered_on,
                       action_type, action_device_id, action_target_value
                FROM rules
                WHERE user_email = ?
                ORDER BY name, id
                """;

        List<Rule> rules = new ArrayList<>();

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userEmail);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    rules.add(mapRule(resultSet));
                }
            }

            return rules;
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to query rules", exception);
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
    public void saveSchedule(String userEmail, Schedule schedule) {
        String sql = """
                INSERT INTO schedules(
                    id, user_email, name, device_id, action_type, target_value, execution_time, recurring_days, last_executed_on
                )
                VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindSchedule(statement, userEmail, schedule);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save schedule", exception);
        }
    }

    @Override
    public void updateSchedule(Schedule schedule) {
        String sql = """
                UPDATE schedules
                SET name = ?, action_type = ?, target_value = ?, execution_time = ?, recurring_days = ?, last_executed_on = ?
                WHERE id = ?
                """;

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, schedule.getName());
            statement.setString(2, schedule.getActionType().name());
            setNullableDouble(statement, 3, schedule.getTargetValue());
            statement.setString(4, schedule.getExecutionTime().toString());
            statement.setString(5, serializeRecurringDays(schedule.getRecurringDays()));
            setNullableString(statement, 6, schedule.getLastExecutedOn() == null ? null : schedule.getLastExecutedOn().toString());
            statement.setString(7, schedule.getId());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to update schedule", exception);
        }
    }

    @Override
    public void saveRule(String userEmail, Rule rule) {
        String sql = """
                INSERT INTO rules(
                    id, user_email, name, trigger_type, trigger_device_id, trigger_expected_value,
                    threshold_operator, trigger_time, last_triggered_on, action_type, action_device_id,
                    action_target_value
                )
                VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindRule(statement, userEmail, rule);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save rule", exception);
        }
    }

    @Override
    public void updateRule(Rule rule) {
        String sql = """
                UPDATE rules
                SET name = ?, trigger_type = ?, trigger_device_id = ?, trigger_expected_value = ?,
                    threshold_operator = ?, trigger_time = ?, last_triggered_on = ?,
                    action_type = ?, action_device_id = ?, action_target_value = ?
                WHERE id = ?
                """;

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, rule.getName());
            statement.setString(2, rule.getTrigger().getTriggerType().name());
            setNullableString(statement, 3, storageTriggerDeviceId(rule));
            setNullableDouble(statement, 4, rule.getTrigger().getExpectedValue());
            setNullableString(statement, 5, serializeThresholdOperator(rule.getTrigger().getThresholdOperator()));
            setNullableString(statement, 6, serializeLocalTime(rule.getTrigger().getTriggerTime()));
            setNullableString(statement, 7, serializeLocalDate(rule.getTrigger().getLastTriggeredOn()));
            statement.setString(8, rule.getAction().getActionType().name());
            statement.setString(9, rule.getAction().getTargetDeviceId());
            setNullableDouble(statement, 10, rule.getAction().getTargetValue());
            statement.setString(11, rule.getId());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to update rule", exception);
        }
    }

    @Override
    public void saveActivityLogEntry(String userEmail, ActivityLogEntry entry) {
        String sql = """
                INSERT INTO activity_log(
                    user_email, occurred_at, device_id, device_name, actor_type, actor_name, previous_state, new_state
                )
                VALUES(?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userEmail);
            statement.setString(2, entry.getTimestamp().toString());
            statement.setString(3, entry.getDeviceId());
            statement.setString(4, entry.getDeviceName());
            statement.setString(5, entry.getActorType().name());
            statement.setString(6, entry.getActorName());
            statement.setString(7, entry.getPreviousState());
            statement.setString(8, entry.getNewState());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save activity log entry", exception);
        }
    }

    @Override
    public void deleteSchedule(String scheduleId) {
        String sql = "DELETE FROM schedules WHERE id = ?";

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, scheduleId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to delete schedule", exception);
        }
    }

    @Override
    public void deleteRule(String ruleId) {
        String sql = "DELETE FROM rules WHERE id = ?";

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, ruleId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to delete rule", exception);
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

    private Schedule mapSchedule(ResultSet resultSet) throws SQLException {
        Schedule schedule = new Schedule(
                resultSet.getString("id"),
                resultSet.getString("name"),
                resultSet.getString("device_id"),
                ScheduleActionType.valueOf(resultSet.getString("action_type")),
                readNullableDouble(resultSet, "target_value"),
                LocalTime.parse(resultSet.getString("execution_time")),
                parseRecurringDays(resultSet.getString("recurring_days"))
        );

        String lastExecutedOn = resultSet.getString("last_executed_on");
        if (lastExecutedOn != null && !lastExecutedOn.isBlank()) {
            schedule.markExecuted(LocalDate.parse(lastExecutedOn));
        }
        return schedule;
    }

    private Rule mapRule(ResultSet resultSet) throws SQLException {
        RuleTrigger trigger = new RuleTrigger(
                RuleTriggerType.valueOf(resultSet.getString("trigger_type")),
                resultSet.getString("trigger_device_id"),
                readNullableDouble(resultSet, "trigger_expected_value"),
                parseThresholdOperator(resultSet.getString("threshold_operator")),
                parseLocalTime(resultSet.getString("trigger_time")),
                parseLocalDate(resultSet.getString("last_triggered_on"))
        );
        RuleAction action = new RuleAction(
                RuleActionType.valueOf(resultSet.getString("action_type")),
                resultSet.getString("action_device_id"),
                readNullableDouble(resultSet, "action_target_value")
        );
        return new Rule(
                resultSet.getString("id"),
                resultSet.getString("name"),
                trigger,
                action
        );
    }

    private void bindSchedule(PreparedStatement statement, String userEmail, Schedule schedule) throws SQLException {
        statement.setString(1, schedule.getId());
        statement.setString(2, userEmail);
        statement.setString(3, schedule.getName());
        statement.setString(4, schedule.getDeviceId());
        statement.setString(5, schedule.getActionType().name());
        setNullableDouble(statement, 6, schedule.getTargetValue());
        statement.setString(7, schedule.getExecutionTime().toString());
        statement.setString(8, serializeRecurringDays(schedule.getRecurringDays()));
        setNullableString(statement, 9, schedule.getLastExecutedOn() == null ? null : schedule.getLastExecutedOn().toString());
    }

    private void bindRule(PreparedStatement statement, String userEmail, Rule rule) throws SQLException {
        statement.setString(1, rule.getId());
        statement.setString(2, userEmail);
        statement.setString(3, rule.getName());
        statement.setString(4, rule.getTrigger().getTriggerType().name());
        setNullableString(statement, 5, storageTriggerDeviceId(rule));
        setNullableDouble(statement, 6, rule.getTrigger().getExpectedValue());
        setNullableString(statement, 7, serializeThresholdOperator(rule.getTrigger().getThresholdOperator()));
        setNullableString(statement, 8, serializeLocalTime(rule.getTrigger().getTriggerTime()));
        setNullableString(statement, 9, serializeLocalDate(rule.getTrigger().getLastTriggeredOn()));
        statement.setString(10, rule.getAction().getActionType().name());
        statement.setString(11, rule.getAction().getTargetDeviceId());
        setNullableDouble(statement, 12, rule.getAction().getTargetValue());
    }

    private String storageTriggerDeviceId(Rule rule) {
        if (rule.getTrigger().getSourceDeviceId() != null) {
            return rule.getTrigger().getSourceDeviceId();
        }
        return rule.getAction().getTargetDeviceId();
    }

    private String serializeThresholdOperator(ThresholdOperator thresholdOperator) {
        return thresholdOperator == null ? null : thresholdOperator.name();
    }

    private String serializeLocalTime(LocalTime localTime) {
        return localTime == null ? null : localTime.toString();
    }

    private String serializeLocalDate(LocalDate localDate) {
        return localDate == null ? null : localDate.toString();
    }

    private ThresholdOperator parseThresholdOperator(String thresholdOperator) {
        if (thresholdOperator == null || thresholdOperator.isBlank()) {
            return null;
        }
        return ThresholdOperator.valueOf(thresholdOperator);
    }

    private LocalTime parseLocalTime(String localTime) {
        if (localTime == null || localTime.isBlank()) {
            return null;
        }
        return LocalTime.parse(localTime);
    }

    private LocalDate parseLocalDate(String localDate) {
        if (localDate == null || localDate.isBlank()) {
            return null;
        }
        return LocalDate.parse(localDate);
    }

    private void setNullableDouble(PreparedStatement statement, int index, Double value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.REAL);
            return;
        }
        statement.setDouble(index, value);
    }

    private void setNullableString(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.VARCHAR);
            return;
        }
        statement.setString(index, value);
    }

    private Double readNullableDouble(ResultSet resultSet, String columnName) throws SQLException {
        double value = resultSet.getDouble(columnName);
        if (resultSet.wasNull()) {
            return null;
        }
        return value;
    }

    private String serializeRecurringDays(Set<DayOfWeek> recurringDays) {
        List<String> dayNames = new ArrayList<>();
        for (DayOfWeek dayOfWeek : recurringDays) {
            dayNames.add(dayOfWeek.name());
        }
        return String.join(",", dayNames);
    }

    private Set<DayOfWeek> parseRecurringDays(String recurringDays) {
        EnumSet<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        for (String token : recurringDays.split(",")) {
            days.add(DayOfWeek.valueOf(token.trim()));
        }
        return days;
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
        String createActivityLogSql = """
                CREATE TABLE IF NOT EXISTS activity_log (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_email TEXT NOT NULL,
                    occurred_at TEXT NOT NULL,
                    device_id TEXT NOT NULL,
                    device_name TEXT NOT NULL,
                    actor_type TEXT NOT NULL,
                    actor_name TEXT NOT NULL,
                    previous_state TEXT NOT NULL,
                    new_state TEXT NOT NULL,
                    FOREIGN KEY(user_email) REFERENCES users(email) ON DELETE CASCADE
                )
                """;
        String createSchedulesSql = """
                CREATE TABLE IF NOT EXISTS schedules (
                    id TEXT PRIMARY KEY,
                    user_email TEXT NOT NULL,
                    name TEXT NOT NULL,
                    device_id TEXT NOT NULL,
                    action_type TEXT NOT NULL,
                    target_value REAL,
                    execution_time TEXT NOT NULL,
                    recurring_days TEXT NOT NULL,
                    last_executed_on TEXT,
                    FOREIGN KEY(user_email) REFERENCES users(email) ON DELETE CASCADE,
                    FOREIGN KEY(device_id) REFERENCES devices(id) ON DELETE CASCADE
                )
                """;
        String createRulesSql = """
                CREATE TABLE IF NOT EXISTS rules (
                    id TEXT PRIMARY KEY,
                    user_email TEXT NOT NULL,
                    name TEXT NOT NULL,
                    trigger_type TEXT NOT NULL,
                    trigger_device_id TEXT NOT NULL,
                    trigger_expected_value REAL,
                    threshold_operator TEXT,
                    trigger_time TEXT,
                    last_triggered_on TEXT,
                    action_type TEXT NOT NULL,
                    action_device_id TEXT NOT NULL,
                    action_target_value REAL,
                    FOREIGN KEY(user_email) REFERENCES users(email) ON DELETE CASCADE,
                    FOREIGN KEY(trigger_device_id) REFERENCES devices(id) ON DELETE CASCADE,
                    FOREIGN KEY(action_device_id) REFERENCES devices(id) ON DELETE CASCADE
                )
                """;

        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(createRoomsSql);
            statement.execute(createDevicesSql);
            statement.execute(createActivityLogSql);
            statement.execute(createSchedulesSql);
            statement.execute(createRulesSql);
            ensureActivityLogColumns(connection);
            ensureRuleColumns(connection);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to initialize home schema", exception);
        }
    }

    private void ensureActivityLogColumns(Connection connection) throws SQLException {
        List<String> activityLogColumns = new ArrayList<>();

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("PRAGMA table_info(activity_log)")) {
            while (resultSet.next()) {
                activityLogColumns.add(resultSet.getString("name"));
            }
        }

        try (Statement statement = connection.createStatement()) {
            if (!activityLogColumns.contains("previous_state")) {
                statement.execute("ALTER TABLE activity_log ADD COLUMN previous_state TEXT NOT NULL DEFAULT 'Unknown'");
            }
            if (!activityLogColumns.contains("new_state")) {
                statement.execute("ALTER TABLE activity_log ADD COLUMN new_state TEXT NOT NULL DEFAULT 'Unknown'");
            }
            statement.executeUpdate(
                    "UPDATE activity_log SET previous_state = 'Unknown' WHERE TRIM(COALESCE(previous_state, '')) = ''"
            );
            statement.executeUpdate(
                    "UPDATE activity_log SET new_state = 'Unknown' WHERE TRIM(COALESCE(new_state, '')) = ''"
            );
        }
    }

    private void ensureRuleColumns(Connection connection) throws SQLException {
        List<String> ruleColumns = new ArrayList<>();

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("PRAGMA table_info(rules)")) {
            while (resultSet.next()) {
                ruleColumns.add(resultSet.getString("name"));
            }
        }

        try (Statement statement = connection.createStatement()) {
            if (!ruleColumns.contains("threshold_operator")) {
                statement.execute("ALTER TABLE rules ADD COLUMN threshold_operator TEXT");
            }
            if (!ruleColumns.contains("trigger_time")) {
                statement.execute("ALTER TABLE rules ADD COLUMN trigger_time TEXT");
            }
            if (!ruleColumns.contains("last_triggered_on")) {
                statement.execute("ALTER TABLE rules ADD COLUMN last_triggered_on TEXT");
            }
        }
    }
}
