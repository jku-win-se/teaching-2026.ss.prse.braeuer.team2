package at.jku.se.smarthome.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

import org.junit.Test;

@SuppressWarnings("PMD")
public class MemberInvitationTest {

    @Test
    public void ownerCanInviteFutureMemberAndMemberCanAccessHousehold() throws IOException {
        SmartHomeSystem system = UserRoleTestSupport.createSystemWithTempDatabase();
        system.registerUser("owner@example.com", "password123");
        system.loginUser("owner@example.com", "password123");
        Room room = system.createRoom("Living Room");
        Device lamp = system.createDevice(room.getId(), "Lamp", DeviceType.SWITCH);

        system.inviteMember("member@example.com");
        system.logoutUser();
        User member = system.registerUser("member@example.com", "password123");
        system.loginUser("member@example.com", "password123");

        assertEquals("Invited users should be registered as members", UserRole.MEMBER, member.getRole());
        assertTrue("The logged-in user should have member permissions", system.isCurrentUserMember());
        assertNotNull("The member should see the owner's invited household device", system.findDeviceById(lamp.getId()));
    }

    @Test
    public void invitedMemberCanControlDevicesButCannotManageOwnerResources() throws IOException {
        InvitationTestContext context = createHouseholdWithInvitedMember();
        SmartHomeSystem system = context.system();

        system.loginUser("member@example.com", "password123");
        Device lamp = system.findDeviceById(context.deviceId());
        system.toggleDevice(lamp.getId());

        assertTrue("Members should be allowed to control household devices", system.findDeviceById(context.deviceId()).isOn());
        assertThrows("Members must not create rooms", IllegalStateException.class,
                () -> system.createRoom("Kitchen"));
        assertThrows("Members must not create devices", IllegalStateException.class,
                () -> system.createDevice(context.roomId(), "Thermostat", DeviceType.THERMOSTAT));
        assertThrows("Members must not create rules", IllegalStateException.class,
                () -> system.createRule(
                        "No member rules",
                        RuleTriggerType.DEVICE_STATE_CHANGE,
                        context.deviceId(),
                        1.0,
                        RuleActionType.SET_DEVICE_STATE,
                        context.deviceId(),
                        0.0
                ));
        assertThrows("Members must not create schedules", IllegalStateException.class,
                () -> system.createSchedule(
                        "No member schedules",
                        context.deviceId(),
                        ScheduleActionType.SET_VALUE,
                        1.0,
                        LocalTime.of(7, 0),
                        Set.of(DayOfWeek.MONDAY)
                ));
    }

    @Test
    public void ownerCanRevokeMemberAccess() throws IOException {
        InvitationTestContext context = createHouseholdWithInvitedMember();
        SmartHomeSystem system = context.system();

        system.loginUser("owner@example.com", "password123");
        assertEquals("The owner should see one invited member", 1, system.getInvitedMemberEmails().size());
        system.revokeMemberAccess("member@example.com");
        assertTrue("The member list should be empty after revocation", system.getInvitedMemberEmails().isEmpty());
        system.logoutUser();

        system.loginUser("member@example.com", "password123");

        assertFalse("The revoked member must not see the owner's household rooms", system.getRooms().stream()
                .anyMatch(room -> context.roomId().equals(room.getId())));
        assertNull("The revoked member should not resolve the owner's device", system.findDeviceById(context.deviceId()));
    }

    @Test
    public void ownerCannotInviteInvalidEmailOrAnotherOwner() throws IOException {
        SmartHomeSystem system = UserRoleTestSupport.createSystemWithTempDatabase();
        system.registerUser("owner@example.com", "password123");
        system.registerUser("other-owner@example.com", "password123");
        system.loginUser("owner@example.com", "password123");

        assertThrows("Invitation e-mail addresses must be valid", IllegalArgumentException.class,
                () -> system.inviteMember("invalid-email"));
        assertThrows("Owner accounts must not be invited as members", IllegalArgumentException.class,
                () -> system.inviteMember("other-owner@example.com"));
        assertThrows("Owners must not invite themselves", IllegalArgumentException.class,
                () -> system.inviteMember("owner@example.com"));
    }

    private InvitationTestContext createHouseholdWithInvitedMember() throws IOException {
        SmartHomeSystem system = UserRoleTestSupport.createSystemWithTempDatabase();
        system.registerUser("owner@example.com", "password123");
        system.loginUser("owner@example.com", "password123");
        Room room = system.createRoom("Living Room");
        Device device = system.createDevice(room.getId(), "Lamp", DeviceType.SWITCH);
        system.inviteMember("member@example.com");
        system.logoutUser();
        system.registerUser("member@example.com", "password123");
        return new InvitationTestContext(system, room.getId(), device.getId());
    }

    private record InvitationTestContext(SmartHomeSystem system, String roomId, String deviceId) {
    }
}
