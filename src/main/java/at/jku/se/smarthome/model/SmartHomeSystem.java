package at.jku.se.smarthome.model;

import java.util.ArrayList;
import java.util.List;
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

    private final List<Room> rooms;
    private final UserRepository userRepository;

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
        this.userRepository = userRepository;
    }

    /**
     * Creates a smart home system with persistent SQLite-backed user storage.
     *
     * @return a smart home system with SQLite persistence for users
     */
    public static SmartHomeSystem createPersistentSystem() {
        return new SmartHomeSystem(new SQLiteUserRepository(DEFAULT_DATABASE_URL));
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
}
