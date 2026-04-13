package at.jku.se.smarthome.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import at.jku.se.smarthome.repository.InMemoryUserRepository;
import at.jku.se.smarthome.repository.SQLiteUserRepository;
import at.jku.se.smarthome.repository.UserRepository;
import at.jku.se.smarthome.util.PasswordHasher;

/**
 * Central smart home domain model for rooms, devices and user registration.
 */
public class SmartHomeSystem {
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final String DEFAULT_DATABASE_URL = "jdbc:sqlite:smarthome.db";
    private static final SmartHomeSystem PERSISTENT_SYSTEM =
            new SmartHomeSystem(new SQLiteUserRepository(DEFAULT_DATABASE_URL));

    private final List<Room> rooms;
    private final Map<String, List<Room>> userRooms;
    private final UserRepository userRepository;
    private final UserSession userSession;

    public SmartHomeSystem() {
        this(new InMemoryUserRepository());
    }

    /**
     * Creates a system with the provided user repository.
     *
     * @param userRepository the repository used to persist users
     */
    public SmartHomeSystem(UserRepository userRepository) {
        this.rooms = new ArrayList<>();
        this.userRooms = new HashMap<>();
        this.userRepository = userRepository;
        this.userSession = new UserSession();
    }

    /**
     * Creates a smart home system with persistent SQLite-backed user storage.
     *
     * @return a smart home system with SQLite persistence for users
     */
    public static SmartHomeSystem createPersistentSystem() {
        return PERSISTENT_SYSTEM;
    }

    public void addRoom(Room room) {
        if (room == null) {
            throw new IllegalArgumentException("Room must not be null");
        }
        getActiveRooms().add(room);
    }

    /**
     * Removes all currently stored rooms from the system.
     */
    public void clearRooms() {
        getActiveRooms().clear();
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
        return getActiveRooms().remove(room);
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

    public void renameDevice(String deviceId, String newName) {
        Device device = findDeviceById(deviceId);
        if (device == null) {
            throw new IllegalArgumentException("Device not found");
        }
        device.rename(newName);
    }

    public boolean removeDevice(String deviceId) {
        for (Room room : getActiveRooms()) {
            if (room.removeDevice(deviceId)) {
                return true;
            }
        }
        return false;
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
        return userRooms.computeIfAbsent(userEmail, key -> new ArrayList<>());
    }

    private void requireAuthenticatedUser() {
        if (!userSession.isLoggedIn()) {
            throw new IllegalStateException("User must be logged in");
        }
    }
}
