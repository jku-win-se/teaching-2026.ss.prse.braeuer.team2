package at.jku.se.smarthome.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Assert;
import org.junit.Test;

import at.jku.se.smarthome.repository.SQLiteUserRepository;

@SuppressWarnings({
        "PMD.CommentRequired",
        "PMD.AtLeastOneConstructor"
})
public class UserLoginTest {

    @Test
    public void loginUserRegisteredUserLogsInAndCreatesSession() throws IOException {
        SmartHomeSystem system = createSystemWithTempDatabase();
        User registeredUser = system.registerUser("user@example.com", "secret123");

        User loggedInUser = system.loginUser("user@example.com", "secret123");

        Assert.assertEquals("The logged-in user email should match the registered user",
                registeredUser.getEmail(), loggedInUser.getEmail());
        Assert.assertTrue("The user should be marked as logged in", system.isUserLoggedIn());
        Assert.assertSame("The current session should reference the logged-in user",
                loggedInUser, system.getLoggedInUser());
    }

    @Test
    public void loginUserWrongPasswordThrowsException() throws IOException {
        SmartHomeSystem system = createSystemWithTempDatabase();
        system.registerUser("user@example.com", "secret123");

        Assert.assertThrows(IllegalArgumentException.class,
                () -> system.loginUser("user@example.com", "wrongpass"));
        Assert.assertFalse("The user should remain logged out after a failed login attempt",
                system.isUserLoggedIn());
    }

    @Test
    public void loginUserUnknownEmailThrowsException() throws IOException {
        SmartHomeSystem system = createSystemWithTempDatabase();

        Assert.assertThrows(IllegalArgumentException.class,
                () -> system.loginUser("missing@example.com", "secret123"));
        Assert.assertFalse("Unknown users must not be logged in", system.isUserLoggedIn());
    }

    @Test
    public void logoutUserClearsSession() throws IOException {
        SmartHomeSystem system = createSystemWithTempDatabase();
        system.registerUser("user@example.com", "secret123");
        system.loginUser("user@example.com", "secret123");

        system.logoutUser();

        Assert.assertFalse("The user should be logged out after calling logout", system.isUserLoggedIn());
        Assert.assertNull("The session should no longer contain a logged-in user", system.getLoggedInUser());
    }

    private SmartHomeSystem createSystemWithTempDatabase() throws IOException {
        Path databaseFile = Files.createTempFile("smarthome-login-test", ".db");
        databaseFile.toFile().deleteOnExit();
        return new SmartHomeSystem(new SQLiteUserRepository("jdbc:sqlite:" + databaseFile));
    }
}
