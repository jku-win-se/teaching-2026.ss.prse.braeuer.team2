package at.jku.se.smarthome.model;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

@SuppressWarnings({
        "PMD.CommentRequired",
        "PMD.AtLeastOneConstructor"
})
public class UserRoleMemberDeviceAuthorizationTest {

    @Test
    public void memberCanControlExistingDevice() throws IOException {
        UserRoleTestSupport.RoleTestContext context = UserRoleTestSupport.createMemberContextWithDevice();

        context.system().loginUser("member@example.com", "password123");
        Device device = context.system().findDeviceById("device-1");
        context.system().toggleDevice(device.getId());

        assertTrue("Members should be allowed to control devices",
                context.system().findDeviceById(device.getId()).isOn());
    }

    @Test
    public void memberCannotCreateRoom() throws IOException {
        UserRoleTestSupport.RoleTestContext context = UserRoleTestSupport.createMemberContextWithDevice();

        context.system().loginUser("member@example.com", "password123");

        assertThrows("Members must not create rooms", IllegalStateException.class,
                () -> context.system().createRoom("Kitchen"));
    }

    @Test
    public void memberCannotCreateDevice() throws IOException {
        UserRoleTestSupport.RoleTestContext context = UserRoleTestSupport.createMemberContextWithDevice();

        context.system().loginUser("member@example.com", "password123");

        assertThrows("Members must not create devices", IllegalStateException.class,
                () -> context.system().createDevice("room-1", "Thermostat", DeviceType.THERMOSTAT));
    }

    @Test
    public void memberCannotRenameDevice() throws IOException {
        UserRoleTestSupport.RoleTestContext context = UserRoleTestSupport.createMemberContextWithDevice();

        context.system().loginUser("member@example.com", "password123");

        assertThrows("Members must not rename devices", IllegalStateException.class,
                () -> context.system().renameDevice("device-1", "New Lamp"));
    }

    @Test
    public void memberCannotRemoveDevice() throws IOException {
        UserRoleTestSupport.RoleTestContext context = UserRoleTestSupport.createMemberContextWithDevice();

        context.system().loginUser("member@example.com", "password123");

        assertThrows("Members must not remove devices", IllegalStateException.class,
                () -> context.system().removeDevice("device-1"));
    }
}
