package at.jku.se.smarthome.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

import at.jku.se.smarthome.repository.SQLiteUserRepository;

public class UserLoginTest {

    @Test
    public void loginUser_registeredUser_logsInAndCreatesSession() throws IOException {
        SmartHomeSystem system = createSystemWithTempDatabase();
        User registeredUser = system.registerUser("user@example.com", "secret123");

        User loggedInUser = system.loginUser("user@example.com", "secret123");

        assertEquals(registeredUser.getEmail(), loggedInUser.getEmail());
        assertTrue(system.isUserLoggedIn());
        assertSame(loggedInUser, system.getLoggedInUser());
    }

    @Test
    public void loginUser_wrongPassword_throwsException() throws IOException {
        SmartHomeSystem system = createSystemWithTempDatabase();
        system.registerUser("user@example.com", "secret123");

        assertThrows(IllegalArgumentException.class,
                () -> system.loginUser("user@example.com", "wrongpass"));
        assertFalse(system.isUserLoggedIn());
    }

    @Test
    public void loginUser_unknownEmail_throwsException() throws IOException {
        SmartHomeSystem system = createSystemWithTempDatabase();

        assertThrows(IllegalArgumentException.class,
                () -> system.loginUser("missing@example.com", "secret123"));
        assertFalse(system.isUserLoggedIn());
    }

    @Test
    public void logoutUser_clearsSession() throws IOException {
        SmartHomeSystem system = createSystemWithTempDatabase();
        system.registerUser("user@example.com", "secret123");
        system.loginUser("user@example.com", "secret123");

        system.logoutUser();

        assertFalse(system.isUserLoggedIn());
        assertEquals(null, system.getLoggedInUser());
    }

    private SmartHomeSystem createSystemWithTempDatabase() throws IOException {
        Path databaseFile = Files.createTempFile("smarthome-login-test", ".db");
        databaseFile.toFile().deleteOnExit();
        return new SmartHomeSystem(new SQLiteUserRepository("jdbc:sqlite:" + databaseFile));
    }
}
