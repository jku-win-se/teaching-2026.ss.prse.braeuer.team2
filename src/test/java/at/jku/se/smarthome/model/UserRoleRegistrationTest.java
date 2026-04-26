package at.jku.se.smarthome.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

@SuppressWarnings({
        "PMD.CommentRequired",
        "PMD.AtLeastOneConstructor"
})
public class UserRoleRegistrationTest {

    @Test
    public void registerUserWithoutExplicitRoleCreatesOwner() throws IOException {
        SmartHomeSystem system = UserRoleTestSupport.createSystemWithTempDatabase();

        User user = system.registerUser("owner@example.com", "password123");

        assertEquals("Default registration should create an owner account", UserRole.OWNER, user.getRole());
    }

    @Test
    public void registerUserWithMemberRoleCreatesMember() throws IOException {
        SmartHomeSystem system = UserRoleTestSupport.createSystemWithTempDatabase();

        User user = system.registerUser("member@example.com", "password123", UserRole.MEMBER);

        assertEquals("Explicit member registration should store the member role", UserRole.MEMBER, user.getRole());
    }

    @Test
    public void loginUserKeepsPersistedMemberRole() throws IOException {
        SmartHomeSystem system = UserRoleTestSupport.createSystemWithTempDatabase();
        system.registerUser("member@example.com", "password123", UserRole.MEMBER);

        User loggedInUser = system.loginUser("member@example.com", "password123");

        assertEquals("The logged-in user should keep the persisted member role",
                UserRole.MEMBER, loggedInUser.getRole());
    }

    @Test
    public void loggedInMemberIsRecognizedAsMember() throws IOException {
        SmartHomeSystem system = UserRoleTestSupport.createSystemWithTempDatabase();
        system.registerUser("member@example.com", "password123", UserRole.MEMBER);

        system.loginUser("member@example.com", "password123");

        assertTrue("The session should identify the current user as a member", system.isCurrentUserMember());
    }

    @Test
    public void loggedInMemberIsNotRecognizedAsOwner() throws IOException {
        SmartHomeSystem system = UserRoleTestSupport.createSystemWithTempDatabase();
        system.registerUser("member@example.com", "password123", UserRole.MEMBER);

        system.loginUser("member@example.com", "password123");

        assertFalse("A member session must not be treated as an owner", system.isCurrentUserOwner());
    }
}
