package at.jku.se.smarthome.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

import at.jku.se.smarthome.repository.SQLiteUserRepository;
import at.jku.se.smarthome.util.PasswordHasher;

public class UserRegistrationTest {

    @Test
    public void registerUser_validInput_createsUserWithHashedPassword() throws IOException {
        SmartHomeSystem system = createSystemWithTempDatabase();

        User user = system.registerUser("Test.User@example.com", "secret123");
        User storedUser = system.findUserByEmail("test.user@example.com");

        assertEquals("test.user@example.com", user.getEmail());
        assertEquals(1, system.getUserCount());
        assertEquals(user.getEmail(), storedUser.getEmail());
        assertEquals(user.getPasswordHash(), storedUser.getPasswordHash());
        assertNotEquals("secret123", user.getPasswordHash());
        assertTrue(PasswordHasher.verify("secret123", user.getPasswordHash()));
        assertTrue(user.getPasswordHash().startsWith("$2"));
    }

    @Test
    public void registerUser_duplicateEmail_throwsException() throws IOException {
        SmartHomeSystem system = createSystemWithTempDatabase();
        system.registerUser("user@example.com", "secret123");

        assertThrows(IllegalArgumentException.class,
                () -> system.registerUser("USER@example.com", "secret456"));
    }

    @Test
    public void registerUser_invalidEmail_throwsException() throws IOException {
        SmartHomeSystem system = createSystemWithTempDatabase();

        assertThrows(IllegalArgumentException.class,
                () -> system.registerUser("not-an-email", "secret123"));
    }

    @Test
    public void registerUser_shortPassword_throwsException() throws IOException {
        SmartHomeSystem system = createSystemWithTempDatabase();

        assertThrows(IllegalArgumentException.class,
                () -> system.registerUser("user@example.com", "short"));
    }

    private SmartHomeSystem createSystemWithTempDatabase() throws IOException {
        Path databaseFile = Files.createTempFile("smarthome-registration-test", ".db");
        databaseFile.toFile().deleteOnExit();
        return new SmartHomeSystem(new SQLiteUserRepository("jdbc:sqlite:" + databaseFile));
    }
}
