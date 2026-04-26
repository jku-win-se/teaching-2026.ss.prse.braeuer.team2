package at.jku.se.smarthome.model;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import at.jku.se.smarthome.repository.HomeRepository;
import at.jku.se.smarthome.repository.InMemoryHomeRepository;
import at.jku.se.smarthome.repository.InMemoryUserRepository;
import at.jku.se.smarthome.repository.SQLiteHomeRepository;
import at.jku.se.smarthome.repository.SQLiteUserRepository;
import at.jku.se.smarthome.repository.UserRepository;
import at.jku.se.smarthome.util.PasswordHasher;

/**
 * Central smart home domain model for rooms, devices and user registration.
 */
@SuppressWarnings("PMD")
public class SmartHomeSystem {
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final String DEFAULT_DATABASE_URL = "jdbc:sqlite:" + resolveDefaultDatabasePath();
    private static SmartHomeSystem persistentSystem;

    private final List<Room> rooms;
    private final List<ActivityLogEntry> activityLog;
    private final List<Rule> rules;
    private final List<Schedule> schedules;
    private final Map<String, List<Room>> userRooms;
    private final Map<String, List<ActivityLogEntry>> userActivityLog;
    private final Map<String, List<Rule>> userRules;
    private final Map<String, List<Schedule>> userSchedules;
    private final Set<String> executingRuleIds;
    private final UserRepository userRepository;
    private final HomeRepository homeRepository;
    private final UserSession userSession;
    private final Clock clock;

    public SmartHomeSystem() {
        this(new InMemoryUserRepository(), new InMemoryHomeRepository(), Clock.systemDefaultZone());
    }

    /**
     * Creates a system with the provided user repository.
     *
     * @param userRepository the repository used to persist users
     */
    public SmartHomeSystem(UserRepository userRepository) {
        this(userRepository, resolveHomeRepository(userRepository), Clock.systemDefaultZone());
    }

    /**
     * Creates a system with the provided repositories.
     *
     * @param userRepository the repository used to persist users
     * @param homeRepository the repository used to persist rooms and devices
     */
    public SmartHomeSystem(UserRepository userRepository, HomeRepository homeRepository) {
        this(userRepository, homeRepository, Clock.systemDefaultZone());
    }

    /**
     * Creates a system with the provided repositories and clock.
     *
     * @param userRepository the repository used to persist users
     * @param homeRepository the repository used to persist rooms, devices and activity log entries
     * @param clock the clock used to timestamp state changes
     */
    public SmartHomeSystem(UserRepository userRepository, HomeRepository homeRepository, Clock clock) {
        this.rooms = new ArrayList<>();
        this.activityLog = new ArrayList<>();
        this.rules = new ArrayList<>();
        this.schedules = new ArrayList<>();
        this.userRooms = new HashMap<>();
        this.userActivityLog = new HashMap<>();
        this.userRules = new HashMap<>();
        this.userSchedules = new HashMap<>();
        this.executingRuleIds = new HashSet<>();
        this.userRepository = userRepository;
        this.homeRepository = homeRepository;
        this.userSession = new UserSession();
        this.clock = clock;
    }

    /**
     * Creates a smart home system with persistent SQLite-backed user storage.
     *
     * @return a smart home system with SQLite persistence for users
     */
    public static SmartHomeSystem createPersistentSystem() {
        if (persistentSystem == null) {
            persistentSystem = new SmartHomeSystem(
                    new SQLiteUserRepository(DEFAULT_DATABASE_URL),
                    new SQLiteHomeRepository(DEFAULT_DATABASE_URL),
                    Clock.systemDefaultZone()
            );
        }
        return persistentSystem;
    }

    public void addRoom(Room room) {
        if (room == null) {
            throw new IllegalArgumentException("Room must not be null");
        }
        getActiveRooms().add(room);
        persistRoomIfAuthenticated(room);
    }

    /**
     * Removes all currently stored rooms from the system.
     */
    public void clearRooms() {
        if (userSession.isLoggedIn()) {
            homeRepository.deleteRoomsByUserEmail(userSession.getCurrentUser().getEmail());
        }
        getActiveRooms().clear();
        getActiveRules().clear();
        getActiveSchedules().clear();
    }

    /**
     * Creates and stores a room for the currently authenticated user.
     *
     * @param roomName the display name of the room
     * @return the created room
     */
    public Room createRoom(String roomName) {
        requireAuthenticatedUser();
        Room room = new Room(UUID.randomUUID().toString(), roomName);
        getActiveRooms().add(room);
        homeRepository.saveRoom(userSession.getCurrentUser().getEmail(), room);
        return room;
    }

    /**
     * Renames an existing room of the currently authenticated user.
     *
     * @param roomId the room id
     * @param newName the new room name
     */
    public void renameRoom(String roomId, String newName) {
        requireAuthenticatedUser();
        Room room = findRoomById(roomId);
        if (room == null) {
            throw new IllegalArgumentException("Room not found");
        }
        room.rename(newName);
        homeRepository.updateRoom(room);
    }

    /**
     * Removes an existing room of the currently authenticated user.
     *
     * @param roomId the room id
     * @return {@code true} if a room was removed, otherwise {@code false}
     */
    public boolean removeRoom(String roomId) {
        requireAuthenticatedUser();
        Room room = findRoomById(roomId);
        if (room == null) {
            return false;
        }
        removeRulesForRoom(room);
        removeSchedulesForRoom(room);
        boolean removed = getActiveRooms().remove(room);
        if (removed) {
            homeRepository.deleteRoom(roomId);
        }
        return removed;
    }

    /**
     * Returns all rooms of the active context.
     * If a user is logged in, their rooms are returned; otherwise the anonymous in-memory rooms.
     *
     * @return a defensive copy of the rooms list
     */
    public List<Room> getRooms() {
        return List.copyOf(getActiveRooms());
    }

    /**
     * Finds a room by id in the active context.
     *
     * @param roomId the room id
     * @return the matching room, or {@code null} if no room exists
     */
    public Room findRoomById(String roomId) {
        if (roomId == null || roomId.isBlank()) {
            return null;
        }

        for (Room room : getActiveRooms()) {
            if (room.getId().equals(roomId.trim())) {
                return room;
            }
        }
        return null;
    }

    /**
     * Creates a device and assigns it to a room of the authenticated user.
     *
     * @param roomId the target room id
     * @param deviceName the device name
     * @param deviceType the device type
     * @return the created device
     */
    public Device createDevice(String roomId, String deviceName, DeviceType deviceType) {
        requireAuthenticatedUser();
        Room room = findRoomById(roomId);
        if (room == null) {
            throw new IllegalArgumentException("Room not found");
        }

        Device device = new Device(UUID.randomUUID().toString(), deviceName, deviceType);
        room.addDevice(device);
        homeRepository.saveDevice(roomId, device);
        return device;
    }

    public void renameDevice(String deviceId, String newName) {
        Device device = findDeviceById(deviceId);
        if (device == null) {
            throw new IllegalArgumentException("Device not found");
        }
        device.rename(newName);
        homeRepository.updateDevice(device);
    }

    public boolean removeDevice(String deviceId) {
        for (Room room : getActiveRooms()) {
            if (room.removeDevice(deviceId)) {
                removeRulesForDevice(deviceId);
                removeSchedulesForDevice(deviceId);
                homeRepository.deleteDevice(deviceId);
                return true;
            }
        }
        return false;
    }

    /**
     * Toggles a switch device and persists the changed state.
     *
     * @param deviceId the device id
     */
    public void toggleDevice(String deviceId) {
        toggleDeviceInternal(deviceId, ActivityActorType.USER, resolveManualActorName());
    }

    /**
     * Toggles a switch device because of an automation rule and persists the changed state.
     *
     * @param deviceId the device id
     * @param ruleName the executing rule name
     */
    public void toggleDeviceByRule(String deviceId, String ruleName) {
        toggleDeviceInternal(deviceId, ActivityActorType.RULE, validateRuleName(ruleName));
    }

    /**
     * Sets a switch device to an explicit state because of an automation rule and persists the changed state.
     *
     * @param deviceId the device id
     * @param on the target switch state
     * @param ruleName the executing rule name
     */
    public void setSwitchStateByRule(String deviceId, boolean on, String ruleName) {
        setSwitchStateInternal(deviceId, on, ActivityActorType.RULE, validateRuleName(ruleName));
    }

    private void toggleDeviceInternal(String deviceId, ActivityActorType actorType, String actorName) {
        Device device = findDeviceById(deviceId);
        if (device == null) {
            throw new IllegalArgumentException("Device not found");
        }

        String previousState = describeDeviceState(device);
        device.toggle();
        homeRepository.updateDevice(device);
        String newState = describeDeviceState(device);
        logDeviceStateChange(device, actorType, actorName, previousState, newState);
        evaluateRulesAfterDeviceChange(device, previousState, newState);
    }

    /**
     * Updates a device value and persists the changed state.
     *
     * @param deviceId the device id
     * @param value the new device value
     */
    public void updateDeviceValue(String deviceId, double value) {
        updateDeviceValueInternal(deviceId, value, ActivityActorType.USER, resolveManualActorName());
    }

    /**
     * Updates a device value because of an automation rule and persists the changed state.
     *
     * @param deviceId the device id
     * @param value the new device value
     * @param ruleName the executing rule name
     */
    public void updateDeviceValueByRule(String deviceId, double value, String ruleName) {
        updateDeviceValueInternal(deviceId, value, ActivityActorType.RULE, validateRuleName(ruleName));
    }

    private void updateDeviceValueInternal(String deviceId, double value,
                                           ActivityActorType actorType, String actorName) {
        Device device = findDeviceById(deviceId);
        if (device == null) {
            throw new IllegalArgumentException("Device not found");
        }

        String previousState = describeDeviceState(device);
        device.setValue(value);
        String newState = describeDeviceState(device);
        if (previousState.equals(newState)) {
            return;
        }
        homeRepository.updateDevice(device);
        logDeviceStateChange(device, actorType, actorName, previousState, newState);
        evaluateRulesAfterDeviceChange(device, previousState, newState);
    }

    private void setSwitchStateInternal(String deviceId, boolean on, ActivityActorType actorType, String actorName) {
        Device device = findDeviceById(deviceId);
        if (device == null) {
            throw new IllegalArgumentException("Device not found");
        }
        if (device.getType() != DeviceType.SWITCH) {
            throw new IllegalArgumentException("Device is not a switch");
        }

        String previousState = describeDeviceState(device);
        device.setPowerState(on);
        String newState = describeDeviceState(device);
        if (previousState.equals(newState)) {
            return;
        }
        homeRepository.updateDevice(device);
        logDeviceStateChange(device, actorType, actorName, previousState, newState);
        evaluateRulesAfterDeviceChange(device, previousState, newState);
    }

    /**
     * Returns the activity log of the current context.
     * If a user is logged in, their log is returned; otherwise the anonymous in-memory log is returned.
     *
     * @return a defensive copy of the activity log
     */
    public List<ActivityLogEntry> getActivityLog() {
        return List.copyOf(getActiveActivityLog());
    }

    /**
     * Creates and stores an automation rule for the authenticated user.
     *
     * @param name the rule name
     * @param triggerType the trigger type
     * @param sourceDeviceId the source device id
     * @param expectedTriggerValue the expected source device state
     * @param actionType the action type
     * @param targetDeviceId the target device id
     * @param targetActionValue the target device state
     * @return the created rule
     */
    public Rule createRule(String name, RuleTriggerType triggerType, String sourceDeviceId, Double expectedTriggerValue,
                           RuleActionType actionType, String targetDeviceId, Double targetActionValue) {
        requireAuthenticatedUser();
        RuleTrigger trigger = createValidatedRuleTrigger(triggerType, sourceDeviceId, expectedTriggerValue, null, null);
        RuleAction action = createValidatedRuleAction(actionType, targetDeviceId, targetActionValue);
        Rule rule = new Rule(UUID.randomUUID().toString(), name, trigger, action);
        getActiveRules().add(rule);
        homeRepository.saveRule(userSession.getCurrentUser().getEmail(), rule);
        return rule;
    }

    /**
     * Creates and stores a sensor threshold automation rule for the authenticated user.
     *
     * @param name the rule name
     * @param sensorDeviceId the source sensor id
     * @param thresholdOperator the threshold comparison operator
     * @param thresholdValue the threshold value
     * @param actionType the action type
     * @param targetDeviceId the target device id
     * @param targetActionValue the target device state
     * @return the created rule
     */
    public Rule createThresholdRule(String name, String sensorDeviceId, ThresholdOperator thresholdOperator,
                                    Double thresholdValue, RuleActionType actionType, String targetDeviceId,
                                    Double targetActionValue) {
        requireAuthenticatedUser();
        RuleTrigger trigger = createValidatedRuleTrigger(
                RuleTriggerType.THRESHOLD,
                sensorDeviceId,
                thresholdValue,
                thresholdOperator,
                null
        );
        RuleAction action = createValidatedRuleAction(actionType, targetDeviceId, targetActionValue);
        Rule rule = new Rule(UUID.randomUUID().toString(), name, trigger, action);
        getActiveRules().add(rule);
        homeRepository.saveRule(userSession.getCurrentUser().getEmail(), rule);
        return rule;
    }

    /**
     * Creates and stores a time-based automation rule for the authenticated user.
     *
     * @param name the rule name
     * @param triggerTime the time when the rule becomes due
     * @param actionType the action type
     * @param targetDeviceId the target device id
     * @param targetActionValue the target device state
     * @return the created rule
     */
    public Rule createTimeRule(String name, LocalTime triggerTime, RuleActionType actionType,
                               String targetDeviceId, Double targetActionValue) {
        requireAuthenticatedUser();
        RuleTrigger trigger = createValidatedRuleTrigger(RuleTriggerType.TIME, null, null, null, triggerTime);
        RuleAction action = createValidatedRuleAction(actionType, targetDeviceId, targetActionValue);
        Rule rule = new Rule(UUID.randomUUID().toString(), name, trigger, action);
        getActiveRules().add(rule);
        homeRepository.saveRule(userSession.getCurrentUser().getEmail(), rule);
        return rule;
    }

    /**
     * Updates an existing automation rule of the authenticated user.
     *
     * @param ruleId the rule id
     * @param name the rule name
     * @param triggerType the trigger type
     * @param sourceDeviceId the source device id
     * @param expectedTriggerValue the expected source device state
     * @param actionType the action type
     * @param targetDeviceId the target device id
     * @param targetActionValue the target device state
     */
    public void updateRule(String ruleId, String name, RuleTriggerType triggerType, String sourceDeviceId,
                           Double expectedTriggerValue, RuleActionType actionType, String targetDeviceId,
                           Double targetActionValue) {
        requireAuthenticatedUser();
        Rule rule = findRuleById(ruleId);
        if (rule == null) {
            throw new IllegalArgumentException("Rule not found");
        }

        RuleTrigger trigger = createValidatedRuleTrigger(triggerType, sourceDeviceId, expectedTriggerValue, null, null);
        RuleAction action = createValidatedRuleAction(actionType, targetDeviceId, targetActionValue);
        rule.update(name, trigger, action);
        homeRepository.updateRule(rule);
    }

    /**
     * Updates an existing sensor threshold automation rule.
     *
     * @param ruleId the rule id
     * @param name the rule name
     * @param sensorDeviceId the source sensor id
     * @param thresholdOperator the threshold comparison operator
     * @param thresholdValue the threshold value
     * @param actionType the action type
     * @param targetDeviceId the target device id
     * @param targetActionValue the target device state
     */
    public void updateThresholdRule(String ruleId, String name, String sensorDeviceId,
                                    ThresholdOperator thresholdOperator, Double thresholdValue,
                                    RuleActionType actionType, String targetDeviceId, Double targetActionValue) {
        requireAuthenticatedUser();
        Rule rule = requireRule(ruleId);
        RuleTrigger trigger = createValidatedRuleTrigger(
                RuleTriggerType.THRESHOLD,
                sensorDeviceId,
                thresholdValue,
                thresholdOperator,
                null
        );
        RuleAction action = createValidatedRuleAction(actionType, targetDeviceId, targetActionValue);
        rule.update(name, trigger, action);
        homeRepository.updateRule(rule);
    }

    /**
     * Updates an existing time-based automation rule.
     *
     * @param ruleId the rule id
     * @param name the rule name
     * @param triggerTime the trigger time
     * @param actionType the action type
     * @param targetDeviceId the target device id
     * @param targetActionValue the target device state
     */
    public void updateTimeRule(String ruleId, String name, LocalTime triggerTime, RuleActionType actionType,
                               String targetDeviceId, Double targetActionValue) {
        requireAuthenticatedUser();
        Rule rule = requireRule(ruleId);
        RuleTrigger trigger = createValidatedRuleTrigger(RuleTriggerType.TIME, null, null, null, triggerTime);
        RuleAction action = createValidatedRuleAction(actionType, targetDeviceId, targetActionValue);
        rule.update(name, trigger, action);
        homeRepository.updateRule(rule);
    }

    /**
     * Removes an automation rule of the authenticated user.
     *
     * @param ruleId the rule id
     * @return {@code true} if the rule was removed
     */
    public boolean removeRule(String ruleId) {
        requireAuthenticatedUser();
        Rule rule = findRuleById(ruleId);
        if (rule == null) {
            return false;
        }

        boolean removed = getActiveRules().remove(rule);
        if (removed) {
            homeRepository.deleteRule(ruleId);
        }
        return removed;
    }

    /**
     * Returns all rules of the active context.
     *
     * @return a defensive copy of the rules list
     */
    public List<Rule> getRules() {
        return List.copyOf(getActiveRules());
    }

    /**
     * Finds a rule by id in the active context.
     *
     * @param ruleId the rule id
     * @return the matching rule, or {@code null} if none exists
     */
    public Rule findRuleById(String ruleId) {
        if (ruleId == null || ruleId.isBlank()) {
            return null;
        }

        for (Rule rule : getActiveRules()) {
            if (rule.getId().equals(ruleId.trim())) {
                return rule;
            }
        }
        return null;
    }

    private Rule requireRule(String ruleId) {
        Rule rule = findRuleById(ruleId);
        if (rule == null) {
            throw new IllegalArgumentException("Rule not found");
        }
        return rule;
    }

    /**
     * Creates and stores a recurring time-based schedule for the authenticated user.
     *
     * @param name the schedule name
     * @param deviceId the target device id
     * @param actionType the action to execute
     * @param targetValue the optional numeric target value
     * @param executionTime the time of day
     * @param recurringDays the repeating weekdays
     * @return the created schedule
     */
    public Schedule createSchedule(String name, String deviceId, ScheduleActionType actionType, Double targetValue,
                                   LocalTime executionTime, Set<DayOfWeek> recurringDays) {
        requireAuthenticatedUser();
        validateScheduleTarget(deviceId, actionType, targetValue);

        Schedule schedule = new Schedule(
                UUID.randomUUID().toString(),
                name,
                deviceId,
                actionType,
                targetValue,
                executionTime,
                recurringDays
        );
        getActiveSchedules().add(schedule);
        homeRepository.saveSchedule(userSession.getCurrentUser().getEmail(), schedule);
        return schedule;
    }

    /**
     * Updates an existing schedule of the authenticated user.
     *
     * @param scheduleId the schedule id
     * @param name the schedule name
     * @param actionType the action to execute
     * @param targetValue the optional numeric target value
     * @param executionTime the time of day
     * @param recurringDays the repeating weekdays
     */
    public void updateSchedule(String scheduleId, String name, ScheduleActionType actionType, Double targetValue,
                               LocalTime executionTime, Set<DayOfWeek> recurringDays) {
        requireAuthenticatedUser();
        Schedule schedule = findScheduleById(scheduleId);
        if (schedule == null) {
            throw new IllegalArgumentException("Schedule not found");
        }

        validateScheduleTarget(schedule.getDeviceId(), actionType, targetValue);
        schedule.update(name, actionType, targetValue, executionTime, recurringDays);
        homeRepository.updateSchedule(schedule);
    }

    /**
     * Removes a schedule of the authenticated user.
     *
     * @param scheduleId the schedule id
     * @return {@code true} if the schedule was removed
     */
    public boolean removeSchedule(String scheduleId) {
        requireAuthenticatedUser();
        Schedule schedule = findScheduleById(scheduleId);
        if (schedule == null) {
            return false;
        }

        boolean removed = getActiveSchedules().remove(schedule);
        if (removed) {
            homeRepository.deleteSchedule(scheduleId);
        }
        return removed;
    }

    /**
     * Returns all schedules of the active context.
     *
     * @return a defensive copy of the schedules list
     */
    public List<Schedule> getSchedules() {
        return List.copyOf(getActiveSchedules());
    }

    /**
     * Finds a schedule by id in the active context.
     *
     * @param scheduleId the schedule id
     * @return the matching schedule, or {@code null} if none exists
     */
    public Schedule findScheduleById(String scheduleId) {
        if (scheduleId == null || scheduleId.isBlank()) {
            return null;
        }

        for (Schedule schedule : getActiveSchedules()) {
            if (schedule.getId().equals(scheduleId.trim())) {
                return schedule;
            }
        }
        return null;
    }

    /**
     * Executes all schedules that are due at the current clock time.
     *
     * @return the number of executed schedules
     */
    public int executeDueSchedules() {
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), clock.getZone());
        int executedCount = 0;

        for (Schedule schedule : getActiveSchedules()) {
            if (!schedule.isDue(now.toLocalDate(), now.toLocalTime())) {
                continue;
            }

            executeSchedule(schedule, now.toLocalDate());
            executedCount++;
        }
        return executedCount;
    }

    /**
     * Executes all time-based rules that are due at the current clock time.
     *
     * @return the number of executed rules
     */
    public int executeDueRules() {
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), clock.getZone());
        int executedCount = 0;

        for (Rule rule : getActiveRules()) {
            RuleTrigger trigger = rule.getTrigger();
            if (trigger.getTriggerType() != RuleTriggerType.TIME || !trigger.isDue(now.toLocalDate(), now.toLocalTime())) {
                continue;
            }

            executeRule(rule);
            trigger.markTriggered(now.toLocalDate());
            if (userSession.isLoggedIn()) {
                homeRepository.updateRule(rule);
            }
            executedCount++;
        }
        return executedCount;
    }

    public Device findDeviceById(String deviceId) {
        for (Room room : getActiveRooms()) {
            Device device = room.findDeviceById(deviceId);
            if (device != null) {
                return device;
            }
        }
        return null;
    }

    public User registerUser(String email, String password) {
        String normalizedEmail = validateEmail(email);
        validatePassword(password);

        if (userRepository.findByEmail(normalizedEmail) != null) {
            throw new IllegalArgumentException("An account with this email already exists");
        }

        User user = new User(normalizedEmail, PasswordHasher.hash(password));
        return userRepository.save(user);
    }

    /**
     * Logs in a registered user with email address and password.
     *
     * @param email the email address of the user
     * @param password the plain text password of the user
     * @return the authenticated user
     */
    public User loginUser(String email, String password) {
        String normalizedEmail = validateEmail(email);

        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password must not be blank");
        }

        User user = userRepository.findByEmail(normalizedEmail);
        if (user == null || !PasswordHasher.verify(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        userSession.login(user);
        return user;
    }

    /**
     * Ends the current authenticated user session.
     */
    public void logoutUser() {
        userSession.logout();
    }

    /**
     * Returns whether a user is currently logged in.
     *
     * @return {@code true} if a user is logged in, otherwise {@code false}
     */
    public boolean isUserLoggedIn() {
        return userSession.isLoggedIn();
    }

    /**
     * Returns the currently logged in user.
     *
     * @return the authenticated user, or {@code null} if no user is logged in
     */
    public User getLoggedInUser() {
        return userSession.getCurrentUser();
    }

    /**
     * Finds a registered user by email address.
     *
     * @param email the email address to search for
     * @return the matching user, or {@code null} if none exists
     */
    public User findUserByEmail(String email) {
        if (email == null) {
            return null;
        }
        return userRepository.findByEmail(email.trim().toLowerCase());
    }

    /**
     * Returns the number of registered users.
     *
     * @return the number of users
     */
    public int getUserCount() {
        return userRepository.count();
    }

    private String validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email must not be blank");
        }

        String normalizedEmail = email.trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            throw new IllegalArgumentException("Please enter a valid email address");
        }

        return normalizedEmail;
    }

    private void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password must not be blank");
        }
        if (password.length() < 8) {
            throw new IllegalArgumentException("Password must contain at least 8 characters");
        }
    }

    private List<Room> getActiveRooms() {
        if (!userSession.isLoggedIn()) {
            return rooms;
        }

        String userEmail = userSession.getCurrentUser().getEmail();
        return userRooms.computeIfAbsent(userEmail, homeRepository::findRoomsByUserEmail);
    }

    private List<ActivityLogEntry> getActiveActivityLog() {
        if (!userSession.isLoggedIn()) {
            return activityLog;
        }

        String userEmail = userSession.getCurrentUser().getEmail();
        return userActivityLog.computeIfAbsent(userEmail, homeRepository::findActivityLogByUserEmail);
    }

    private List<Schedule> getActiveSchedules() {
        if (!userSession.isLoggedIn()) {
            return schedules;
        }

        String userEmail = userSession.getCurrentUser().getEmail();
        return userSchedules.computeIfAbsent(userEmail, homeRepository::findSchedulesByUserEmail);
    }

    private List<Rule> getActiveRules() {
        if (!userSession.isLoggedIn()) {
            return rules;
        }

        String userEmail = userSession.getCurrentUser().getEmail();
        return userRules.computeIfAbsent(userEmail, homeRepository::findRulesByUserEmail);
    }

    private void requireAuthenticatedUser() {
        if (!userSession.isLoggedIn()) {
            throw new IllegalStateException("User must be logged in");
        }
    }

    private void logDeviceStateChange(Device device, ActivityActorType actorType, String actorName,
                                      String previousState, String newState) {
        ActivityLogEntry entry = new ActivityLogEntry(
                Instant.now(clock),
                device.getId(),
                device.getName(),
                actorType,
                actorName,
                previousState,
                newState
        );
        getActiveActivityLog().add(entry);
        if (userSession.isLoggedIn()) {
            homeRepository.saveActivityLogEntry(userSession.getCurrentUser().getEmail(), entry);
        }
    }

    private String resolveManualActorName() {
        if (userSession.isLoggedIn()) {
            return userSession.getCurrentUser().getEmail();
        }
        return "anonymous";
    }

    private String validateRuleName(String ruleName) {
        if (ruleName == null || ruleName.isBlank()) {
            throw new IllegalArgumentException("Rule name must not be blank");
        }
        return ruleName.trim();
    }

    private RuleTrigger createValidatedRuleTrigger(RuleTriggerType triggerType, String sourceDeviceId,
                                                   Double expectedTriggerValue,
                                                   ThresholdOperator thresholdOperator, LocalTime triggerTime) {
        if (triggerType == null) {
            throw new IllegalArgumentException("Rule trigger type must not be null");
        }

        return switch (triggerType) {
            case DEVICE_STATE_CHANGE -> {
                validateAutomationValue(sourceDeviceId, expectedTriggerValue, "trigger");
                yield new RuleTrigger(triggerType, sourceDeviceId, expectedTriggerValue);
            }
            case THRESHOLD -> createValidatedThresholdTrigger(sourceDeviceId, thresholdOperator, expectedTriggerValue);
            case TIME -> createValidatedTimeTrigger(triggerTime);
        };
    }

    private RuleTrigger createValidatedThresholdTrigger(String sourceDeviceId,
                                                        ThresholdOperator thresholdOperator,
                                                        Double thresholdValue) {
        Device device = findDeviceById(sourceDeviceId);
        if (device == null) {
            throw new IllegalArgumentException("Device not found");
        }
        if (device.getType() != DeviceType.SENSOR) {
            throw new IllegalArgumentException("Threshold triggers require a sensor device");
        }
        if (thresholdOperator == null) {
            throw new IllegalArgumentException("Threshold operator must not be null");
        }
        if (thresholdValue == null) {
            throw new IllegalArgumentException("A threshold value is required");
        }
        return new RuleTrigger(RuleTriggerType.THRESHOLD, sourceDeviceId, thresholdValue, thresholdOperator, null, null);
    }

    private RuleTrigger createValidatedTimeTrigger(LocalTime triggerTime) {
        if (triggerTime == null) {
            throw new IllegalArgumentException("Trigger time must not be null");
        }
        return new RuleTrigger(RuleTriggerType.TIME, null, null, null, triggerTime, null);
    }

    private RuleAction createValidatedRuleAction(RuleActionType actionType, String targetDeviceId,
                                                 Double targetActionValue) {
        if (actionType != RuleActionType.SET_DEVICE_STATE) {
            throw new IllegalArgumentException("Only device state actions are supported");
        }
        validateAutomationValue(targetDeviceId, targetActionValue, "action");
        return new RuleAction(actionType, targetDeviceId, targetActionValue);
    }

    private void validateAutomationValue(String deviceId, Double value, String context) {
        Device device = findDeviceById(deviceId);
        if (device == null) {
            throw new IllegalArgumentException("Device not found");
        }
        if (value == null) {
            throw new IllegalArgumentException("A " + context + " value is required");
        }

        switch (device.getType()) {
            case SWITCH -> {
                if (value != 0.0 && value != 1.0) {
                    throw new IllegalArgumentException("Switch " + context + " must be On or Off");
                }
            }
            case BLIND -> {
                if (value != 0.0 && value != 100.0) {
                    throw new IllegalArgumentException("Blind " + context + " must be Open or Closed");
                }
            }
            case DIMMER, THERMOSTAT, SENSOR -> validateScheduledValue(device.getType(), value);
            default -> throw new IllegalStateException("Unexpected device type: " + device.getType());
        }
    }

    private void validateScheduleTarget(String deviceId, ScheduleActionType actionType, Double targetValue) {
        Device device = findDeviceById(deviceId);
        if (device == null) {
            throw new IllegalArgumentException("Device not found");
        }
        if (actionType == null) {
            throw new IllegalArgumentException("Schedule action type must not be null");
        }

        if (device.getType() == DeviceType.SWITCH) {
            if (actionType == ScheduleActionType.TOGGLE) {
                return;
            }
            if (actionType != ScheduleActionType.SET_VALUE) {
                throw new IllegalArgumentException("Switch schedules must define a target state");
            }
            if (targetValue == null || (targetValue != 0.0 && targetValue != 1.0)) {
                throw new IllegalArgumentException("Switch schedules must target On or Off");
            }
            return;
        }

        if (actionType != ScheduleActionType.SET_VALUE) {
            throw new IllegalArgumentException("This device type requires a target value");
        }
        if (targetValue == null) {
            throw new IllegalArgumentException("A target value is required");
        }
        validateScheduledValue(device.getType(), targetValue);
    }

    private void validateScheduledValue(DeviceType deviceType, double targetValue) {
        Device probe = new Device("schedule-validation", "Schedule Validation", deviceType);
        probe.setValue(targetValue);
    }

    private void executeSchedule(Schedule schedule, LocalDate executionDate) {
        Device device = findDeviceById(schedule.getDeviceId());
        if (device == null) {
            throw new IllegalArgumentException("Device not found");
        }

        if (schedule.getActionType() == ScheduleActionType.TOGGLE) {
            toggleDeviceByRule(schedule.getDeviceId(), schedule.getName());
        } else if (device.getType() == DeviceType.SWITCH) {
            setSwitchStateByRule(schedule.getDeviceId(), schedule.getTargetValue() == 1.0, schedule.getName());
        } else {
            updateDeviceValueByRule(schedule.getDeviceId(), schedule.getTargetValue(), schedule.getName());
        }
        schedule.markExecuted(executionDate);
        if (userSession.isLoggedIn()) {
            homeRepository.updateSchedule(schedule);
        }
    }

    private void evaluateRulesAfterDeviceChange(Device changedDevice, String previousState, String newState) {
        if (previousState.equals(newState)) {
            return;
        }

        for (Rule rule : getActiveRules()) {
            RuleTrigger trigger = rule.getTrigger();
            if (trigger.getSourceDeviceId() == null || !trigger.getSourceDeviceId().equals(changedDevice.getId())) {
                continue;
            }
            if (!ruleMatchesChangedDevice(rule, changedDevice)) {
                continue;
            }
            executeRule(rule);
        }
    }

    private boolean ruleMatchesChangedDevice(Rule rule, Device device) {
        return switch (rule.getTrigger().getTriggerType()) {
            case DEVICE_STATE_CHANGE -> ruleMatchesDeviceState(rule, device);
            case THRESHOLD -> ruleMatchesThreshold(rule, device);
            case TIME -> false;
        };
    }

    private boolean ruleMatchesDeviceState(Rule rule, Device device) {
        Double expectedValue = rule.getTrigger().getExpectedValue();
        return switch (device.getType()) {
            case SWITCH -> (expectedValue != null && expectedValue == 1.0) == device.isOn();
            case BLIND -> expectedValue != null && Double.compare(device.getValue(), expectedValue) == 0;
            case DIMMER, THERMOSTAT, SENSOR ->
                    expectedValue != null && Double.compare(device.getValue(), expectedValue) == 0;
        };
    }

    private boolean ruleMatchesThreshold(Rule rule, Device device) {
        RuleTrigger trigger = rule.getTrigger();
        if (device.getType() != DeviceType.SENSOR || trigger.getExpectedValue() == null
                || trigger.getThresholdOperator() == null) {
            return false;
        }
        return switch (trigger.getThresholdOperator()) {
            case ABOVE -> device.getValue() > trigger.getExpectedValue();
            case BELOW -> device.getValue() < trigger.getExpectedValue();
        };
    }

    private void executeRule(Rule rule) {
        if (executingRuleIds.contains(rule.getId())) {
            return;
        }

        executingRuleIds.add(rule.getId());
        try {
            RuleAction action = rule.getAction();
            Device targetDevice = findDeviceById(action.getTargetDeviceId());
            if (targetDevice == null) {
                throw new IllegalArgumentException("Device not found");
            }

            if (targetDevice.getType() == DeviceType.SWITCH) {
                setSwitchStateByRule(targetDevice.getId(), action.getTargetValue() == 1.0, rule.getName());
            } else {
                updateDeviceValueByRule(targetDevice.getId(), action.getTargetValue(), rule.getName());
            }
        } finally {
            executingRuleIds.remove(rule.getId());
        }
    }

    private void removeRulesForDevice(String deviceId) {
        List<Rule> rulesToRemove = new ArrayList<>();
        for (Rule rule : getActiveRules()) {
            if (deviceId.equals(rule.getTrigger().getSourceDeviceId())
                    || rule.getAction().getTargetDeviceId().equals(deviceId)) {
                rulesToRemove.add(rule);
            }
        }
        deleteRules(rulesToRemove);
    }

    private void removeRulesForRoom(Room room) {
        List<Rule> rulesToRemove = new ArrayList<>();
        for (Rule rule : getActiveRules()) {
            Room triggerRoom = rule.getTrigger().getSourceDeviceId() == null
                    ? null
                    : findRoomContainingDevice(rule.getTrigger().getSourceDeviceId());
            Room actionRoom = findRoomContainingDevice(rule.getAction().getTargetDeviceId());
            if ((triggerRoom != null && triggerRoom.getId().equals(room.getId()))
                    || (actionRoom != null && actionRoom.getId().equals(room.getId()))) {
                rulesToRemove.add(rule);
            }
        }
        deleteRules(rulesToRemove);
    }

    private void deleteRules(List<Rule> rulesToRemove) {
        for (Rule rule : rulesToRemove) {
            getActiveRules().remove(rule);
            if (userSession.isLoggedIn()) {
                homeRepository.deleteRule(rule.getId());
            }
        }
    }

    private void removeSchedulesForRoom(Room room) {
        List<Schedule> schedulesToRemove = new ArrayList<>();
        for (Schedule schedule : getActiveSchedules()) {
            Device device = findDeviceById(schedule.getDeviceId());
            if (device == null) {
                continue;
            }
            Room owningRoom = findRoomContainingDevice(device.getId());
            if (owningRoom != null && owningRoom.getId().equals(room.getId())) {
                schedulesToRemove.add(schedule);
            }
        }
        deleteSchedules(schedulesToRemove);
    }

    private void removeSchedulesForDevice(String deviceId) {
        List<Schedule> schedulesToRemove = new ArrayList<>();
        for (Schedule schedule : getActiveSchedules()) {
            if (schedule.getDeviceId().equals(deviceId)) {
                schedulesToRemove.add(schedule);
            }
        }
        deleteSchedules(schedulesToRemove);
    }

    private void deleteSchedules(List<Schedule> schedulesToRemove) {
        for (Schedule schedule : schedulesToRemove) {
            getActiveSchedules().remove(schedule);
            if (userSession.isLoggedIn()) {
                homeRepository.deleteSchedule(schedule.getId());
            }
        }
    }

    private Room findRoomContainingDevice(String deviceId) {
        for (Room room : getActiveRooms()) {
            if (room.findDeviceById(deviceId) != null) {
                return room;
            }
        }
        return null;
    }

    private String describeDeviceState(Device device) {
        return switch (device.getType()) {
            case SWITCH -> device.isOn() ? "On" : "Off";
            case DIMMER -> formatNumericState(device.getValue(), "%");
            case THERMOSTAT -> formatNumericState(device.getValue(), "°C");
            case BLIND -> device.isOn() ? "Open" : "Closed";
            case SENSOR -> formatNumericState(device.getValue(), device.getUnit());
        };
    }

    private String formatNumericState(double value, String unit) {
        String formattedValue = value == Math.rint(value)
                ? String.format(Locale.ROOT, "%.0f", value)
                : String.format(Locale.ROOT, "%.1f", value);
        if (unit == null || unit.isBlank()) {
            return formattedValue;
        }
        return formattedValue + " " + unit;
    }

    private void persistRoomIfAuthenticated(Room room) {
        if (!userSession.isLoggedIn()) {
            return;
        }

        String userEmail = userSession.getCurrentUser().getEmail();
        homeRepository.saveRoom(userEmail, room);
        for (Device device : room.getDevices()) {
            homeRepository.saveDevice(room.getId(), device);
        }
    }

    private static HomeRepository resolveHomeRepository(UserRepository userRepository) {
        if (userRepository instanceof SQLiteUserRepository sqliteUserRepository) {
            return new SQLiteHomeRepository(sqliteUserRepository.getDatabaseUrl());
        }
        return new InMemoryHomeRepository();
    }

    private static String resolveDefaultDatabasePath() {
        Path projectPath = findProjectRoot(Paths.get("").toAbsolutePath());
        if (projectPath == null) {
            projectPath = findProjectRoot(resolveCodeSourcePath());
        }

        if (projectPath != null) {
            return projectPath.resolve("smarthome.db").toAbsolutePath().toString();
        }

        return Paths.get(System.getProperty("user.home"), ".smarthome-orchestrator", "smarthome.db")
                .toAbsolutePath()
                .toString();
    }

    private static Path findProjectRoot(Path startPath) {
        if (startPath == null) {
            return null;
        }

        Path currentPath = Files.isDirectory(startPath) ? startPath : startPath.getParent();
        while (currentPath != null) {
            if (Files.exists(currentPath.resolve("pom.xml")) || Files.exists(currentPath.resolve(".git"))) {
                return currentPath;
            }
            currentPath = currentPath.getParent();
        }

        return null;
    }

    private static Path resolveCodeSourcePath() {
        try {
            return Paths.get(SmartHomeSystem.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (Exception exception) {
            return null;
        }
    }
}
