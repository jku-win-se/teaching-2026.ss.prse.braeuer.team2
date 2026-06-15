package at.jku.se.smarthome.model;

import java.io.IOException;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import at.jku.se.smarthome.repository.InMemoryHomeRepository;
import at.jku.se.smarthome.repository.InMemoryUserRepository;
import at.jku.se.smarthome.repository.SQLiteHomeRepository;
import at.jku.se.smarthome.repository.SQLiteUserRepository;

@SuppressWarnings({
        "PMD.CommentRequired",
        "PMD.AtLeastOneConstructor",
        "PMD.JUnitAssertionsShouldIncludeMessage",
        "PMD.TooManyMethods"
})
public class ScheduleTest {

    @Test
    public void createScheduleValidSwitchScheduleIsStored() {
        SmartHomeSystem system = new SmartHomeSystem();
        system.registerUser("owner@example.com", "password123");
        system.loginUser("owner@example.com", "password123");
        Room room = system.createRoom("Living Room");
        Device device = system.createDevice(room.getId(), "Lamp", DeviceType.SWITCH);

        Schedule schedule = system.createSchedule(
                "Morning Lamp",
                device.getId(),
                ScheduleActionType.SET_VALUE,
                1.0,
                LocalTime.of(7, 30),
                Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)
        );

        Assert.assertEquals(1, system.getSchedules().size());
        Assert.assertEquals("Morning Lamp", schedule.getName());
        Assert.assertEquals(device.getId(), schedule.getDeviceId());
        Assert.assertEquals(ScheduleActionType.SET_VALUE, schedule.getActionType());
        Assert.assertEquals(1.0, schedule.getTargetValue(), 0.0001);
    }

    @Test
    public void createScheduleSwitchWithInvalidTargetRejectsInvalidAction() {
        SmartHomeSystem system = new SmartHomeSystem();
        system.registerUser("owner@example.com", "password123");
        system.loginUser("owner@example.com", "password123");
        Room room = system.createRoom("Living Room");
        Device device = system.createDevice(room.getId(), "Lamp", DeviceType.SWITCH);

        Assert.assertThrows(
                IllegalArgumentException.class,
                () -> system.createSchedule(
                        "Invalid Lamp Schedule",
                        device.getId(),
                        ScheduleActionType.SET_VALUE,
                        50.0,
                        LocalTime.of(8, 0),
                        Set.of(DayOfWeek.MONDAY)
                )
        );
    }

    @Test
    public void createScheduleDimmerWithoutTargetValueRejectsInvalidAction() {
        SmartHomeSystem system = new SmartHomeSystem();
        system.registerUser("owner@example.com", "password123");
        system.loginUser("owner@example.com", "password123");
        Room room = system.createRoom("Living Room");
        Device device = system.createDevice(room.getId(), "Dimmer", DeviceType.DIMMER);

        Assert.assertThrows(
                IllegalArgumentException.class,
                () -> system.createSchedule(
                        "Invalid Dimmer Schedule",
                        device.getId(),
                        ScheduleActionType.SET_VALUE,
                        null,
                        LocalTime.of(8, 0),
                        Set.of(DayOfWeek.MONDAY)
                )
        );
    }

    @Test
    public void executeDueSchedulesDueRecurringScheduleUpdatesTargetDeviceOncePerDay() {
        MutableClock clock = new MutableClock(Instant.parse("2026-04-27T05:59:00Z"), ZoneOffset.UTC);
        SmartHomeSystem system = new SmartHomeSystem(
                new InMemoryUserRepository(),
                new InMemoryHomeRepository(),
                clock
        );
        system.registerUser("owner@example.com", "password123");
        system.loginUser("owner@example.com", "password123");
        Room room = system.createRoom("Living Room");
        Device device = system.createDevice(room.getId(), "Lamp", DeviceType.SWITCH);
        system.createSchedule(
                "Morning Lamp",
                device.getId(),
                ScheduleActionType.SET_VALUE,
                1.0,
                LocalTime.of(6, 0),
                Set.of(DayOfWeek.MONDAY)
        );

        Assert.assertEquals(0, system.executeDueSchedules());
        Assert.assertFalse(device.isOn());

        clock.setInstant(Instant.parse("2026-04-27T06:00:00Z"));
        Assert.assertEquals(1, system.executeDueSchedules());
        Assert.assertTrue(device.isOn());

        clock.setInstant(Instant.parse("2026-04-27T06:30:00Z"));
        Assert.assertEquals(0, system.executeDueSchedules());
        Assert.assertTrue(device.isOn());

        clock.setInstant(Instant.parse("2026-05-04T06:00:00Z"));
        Assert.assertEquals(1, system.executeDueSchedules());
        Assert.assertTrue(device.isOn());
    }

    @Test
    public void executeDueSchedulesValueScheduleAppliesConfiguredValue() {
        MutableClock clock = new MutableClock(Instant.parse("2026-04-28T18:30:00Z"), ZoneOffset.UTC);
        SmartHomeSystem system = new SmartHomeSystem(
                new InMemoryUserRepository(),
                new InMemoryHomeRepository(),
                clock
        );
        system.registerUser("owner@example.com", "password123");
        system.loginUser("owner@example.com", "password123");
        Room room = system.createRoom("Bedroom");
        Device device = system.createDevice(room.getId(), "Dimmer", DeviceType.DIMMER);
        system.createSchedule(
                "Evening Dimmer",
                device.getId(),
                ScheduleActionType.SET_VALUE,
                40.0,
                LocalTime.of(18, 30),
                Set.of(DayOfWeek.TUESDAY)
        );

        Assert.assertEquals(1, system.executeDueSchedules());
        Assert.assertEquals(40.0, device.getValue(), 0.0001);
        Assert.assertTrue(device.isOn());
        Assert.assertEquals(ActivityActorType.RULE, system.getActivityLog().get(0).getActorType());
        Assert.assertEquals("Evening Dimmer", system.getActivityLog().get(0).getActorName());
    }

    @Test
    public void removeDeviceAlsoRemovesAssociatedSchedules() {
        SmartHomeSystem system = new SmartHomeSystem();
        system.registerUser("owner@example.com", "password123");
        system.loginUser("owner@example.com", "password123");
        Room room = system.createRoom("Living Room");
        Device device = system.createDevice(room.getId(), "Lamp", DeviceType.SWITCH);
        Schedule schedule = system.createSchedule(
                "Morning Lamp",
                device.getId(),
                ScheduleActionType.SET_VALUE,
                1.0,
                LocalTime.of(7, 30),
                Set.of(DayOfWeek.MONDAY)
        );

        system.removeDevice(device.getId());

        Assert.assertTrue(system.getSchedules().isEmpty());
        Assert.assertFalse(system.removeSchedule(schedule.getId()));
    }

    @Test
    public void schedulesAreLoadedAgainAfterRestart() throws IOException {
        java.nio.file.Path databaseFile = java.nio.file.Files.createTempFile("smarthome-schedule", ".db");
        databaseFile.toFile().deleteOnExit();
        String databaseUrl = "jdbc:sqlite:" + databaseFile;

        SmartHomeSystem firstSystem = new SmartHomeSystem(
                new SQLiteUserRepository(databaseUrl),
                new SQLiteHomeRepository(databaseUrl),
                Clock.fixed(Instant.parse("2026-04-29T07:00:00Z"), ZoneOffset.UTC)
        );
        firstSystem.registerUser("owner@example.com", "password123");
        firstSystem.loginUser("owner@example.com", "password123");
        Room room = firstSystem.createRoom("Living Room");
        Device device = firstSystem.createDevice(room.getId(), "Lamp", DeviceType.SWITCH);
        firstSystem.createSchedule(
                "Morning Lamp",
                device.getId(),
                ScheduleActionType.SET_VALUE,
                1.0,
                LocalTime.of(7, 0),
                Set.of(DayOfWeek.WEDNESDAY)
        );
        firstSystem.executeDueSchedules();
        firstSystem.logoutUser();

        SmartHomeSystem secondSystem = new SmartHomeSystem(
                new SQLiteUserRepository(databaseUrl),
                new SQLiteHomeRepository(databaseUrl),
                Clock.systemUTC()
        );
        secondSystem.loginUser("owner@example.com", "password123");

        Assert.assertEquals(1, secondSystem.getSchedules().size());
        Schedule persistedSchedule = secondSystem.getSchedules().get(0);
        Assert.assertEquals("Morning Lamp", persistedSchedule.getName());
        Assert.assertEquals(LocalTime.of(7, 0), persistedSchedule.getExecutionTime());
        Assert.assertEquals(Set.of(DayOfWeek.WEDNESDAY), persistedSchedule.getRecurringDays());
        Assert.assertEquals(LocalDate.of(2026, 4, 29), persistedSchedule.getLastExecutedOn());
    }

    @Test
    public void vacationModeOverridesNormalDueSchedulesWithSelectedSchedule() {
        MutableClock clock = new MutableClock(Instant.parse("2026-04-27T06:00:00Z"), ZoneOffset.UTC);
        SmartHomeSystem system = new SmartHomeSystem(
                new InMemoryUserRepository(),
                new InMemoryHomeRepository(),
                clock
        );
        system.registerUser("owner@example.com", "password123");
        system.loginUser("owner@example.com", "password123");
        Room room = system.createRoom("Living Room");
        Device lamp = system.createDevice(room.getId(), "Lamp", DeviceType.SWITCH);
        Schedule normalSchedule = system.createSchedule(
                "Normal Morning",
                lamp.getId(),
                ScheduleActionType.SET_VALUE,
                1.0,
                LocalTime.of(6, 0),
                Set.of(DayOfWeek.MONDAY)
        );
        Schedule vacationSchedule = system.createVacationSchedule(
                "Vacation Morning",
                lamp.getId(),
                ScheduleActionType.SET_VALUE,
                0.0,
                LocalTime.of(6, 0),
                Set.of(DayOfWeek.MONDAY)
        );

        system.activateVacationMode(
                vacationSchedule.getId(),
                LocalDateTime.of(2026, 4, 27, 0, 0),
                LocalDateTime.of(2026, 4, 28, 0, 0)
        );

        Assert.assertTrue(system.isVacationModeActive());
        Assert.assertEquals(1, system.executeDueSchedules());
        Assert.assertFalse(lamp.isOn());
        Assert.assertEquals(LocalDate.of(2026, 4, 27), normalSchedule.getLastExecutedOn());
        Assert.assertEquals(LocalDate.of(2026, 4, 27), vacationSchedule.getLastExecutedOn());
    }

    @Test
    public void vacationModeCanApplyMultipleVacationSchedules() {
        MutableClock clock = new MutableClock(Instant.parse("2026-04-27T06:00:00Z"), ZoneOffset.UTC);
        SmartHomeSystem system = new SmartHomeSystem(
                new InMemoryUserRepository(),
                new InMemoryHomeRepository(),
                clock
        );
        system.registerUser("owner@example.com", "password123");
        system.loginUser("owner@example.com", "password123");
        Room room = system.createRoom("Living Room");
        Device lamp = system.createDevice(room.getId(), "Lamp", DeviceType.SWITCH);
        Device dimmer = system.createDevice(room.getId(), "Dimmer", DeviceType.DIMMER);
        Device thermostat = system.createDevice(room.getId(), "Thermostat", DeviceType.THERMOSTAT);
        Schedule normalSchedule = system.createSchedule(
                "Normal Morning",
                lamp.getId(),
                ScheduleActionType.SET_VALUE,
                1.0,
                LocalTime.of(6, 0),
                Set.of(DayOfWeek.MONDAY)
        );
        Schedule vacationDimmer = system.createVacationSchedule(
                "Vacation Dimmer",
                dimmer.getId(),
                ScheduleActionType.SET_VALUE,
                30.0,
                LocalTime.of(6, 0),
                Set.of(DayOfWeek.MONDAY)
        );
        Schedule vacationThermostat = system.createVacationSchedule(
                "Vacation Thermostat",
                thermostat.getId(),
                ScheduleActionType.SET_VALUE,
                17.0,
                LocalTime.of(6, 0),
                Set.of(DayOfWeek.MONDAY)
        );

        system.activateVacationMode(
                Set.of(vacationDimmer.getId(), vacationThermostat.getId()),
                LocalDateTime.of(2026, 4, 27, 0, 0),
                LocalDateTime.of(2026, 4, 28, 0, 0)
        );

        Assert.assertTrue(system.isVacationModeActive());
        Assert.assertEquals(2, system.executeDueSchedules());
        Assert.assertFalse(lamp.isOn());
        Assert.assertEquals(30.0, dimmer.getValue(), 0.0001);
        Assert.assertEquals(17.0, thermostat.getValue(), 0.0001);
        Assert.assertEquals(LocalDate.of(2026, 4, 27), normalSchedule.getLastExecutedOn());
    }

    @Test
    public void normalSchedulesAreUsedAgainAfterVacationModeEnds() {
        MutableClock clock = new MutableClock(Instant.parse("2026-04-27T06:00:00Z"), ZoneOffset.UTC);
        SmartHomeSystem system = new SmartHomeSystem(
                new InMemoryUserRepository(),
                new InMemoryHomeRepository(),
                clock
        );
        system.registerUser("owner@example.com", "password123");
        system.loginUser("owner@example.com", "password123");
        Room room = system.createRoom("Living Room");
        Device lamp = system.createDevice(room.getId(), "Lamp", DeviceType.SWITCH);
        Device dimmer = system.createDevice(room.getId(), "Dimmer", DeviceType.DIMMER);
        system.createSchedule(
                "Normal Morning",
                lamp.getId(),
                ScheduleActionType.SET_VALUE,
                1.0,
                LocalTime.of(6, 0),
                Set.of(DayOfWeek.MONDAY)
        );
        Schedule vacationSchedule = system.createVacationSchedule(
                "Vacation Morning",
                dimmer.getId(),
                ScheduleActionType.SET_VALUE,
                30.0,
                LocalTime.of(6, 0),
                Set.of(DayOfWeek.MONDAY)
        );
        system.activateVacationMode(
                vacationSchedule.getId(),
                LocalDateTime.of(2026, 4, 27, 0, 0),
                LocalDateTime.of(2026, 4, 28, 0, 0)
        );
        system.executeDueSchedules();

        clock.setInstant(Instant.parse("2026-05-04T06:00:00Z"));

        Assert.assertFalse(system.isVacationModeActive());
        Assert.assertFalse(system.isVacationModeExpired());
        Assert.assertNull(system.getVacationMode());
        Assert.assertEquals(1, system.executeDueSchedules());
        Assert.assertTrue(lamp.isOn());
        Assert.assertEquals(30.0, dimmer.getValue(), 0.0001);
    }

    @Test
    public void scheduledVacationModeDoesNotOverrideSchedulesBeforeStart() {
        MutableClock clock = new MutableClock(Instant.parse("2026-04-27T06:00:00Z"), ZoneOffset.UTC);
        SmartHomeSystem system = new SmartHomeSystem(
                new InMemoryUserRepository(),
                new InMemoryHomeRepository(),
                clock
        );
        system.registerUser("owner@example.com", "password123");
        system.loginUser("owner@example.com", "password123");
        Room room = system.createRoom("Living Room");
        Device lamp = system.createDevice(room.getId(), "Lamp", DeviceType.SWITCH);
        Device dimmer = system.createDevice(room.getId(), "Dimmer", DeviceType.DIMMER);
        system.createSchedule(
                "Normal Morning",
                lamp.getId(),
                ScheduleActionType.SET_VALUE,
                1.0,
                LocalTime.of(6, 0),
                Set.of(DayOfWeek.MONDAY)
        );
        Schedule vacationSchedule = system.createVacationSchedule(
                "Vacation Morning",
                dimmer.getId(),
                ScheduleActionType.SET_VALUE,
                30.0,
                LocalTime.of(6, 0),
                Set.of(DayOfWeek.MONDAY)
        );
        system.activateVacationMode(
                vacationSchedule.getId(),
                LocalDateTime.of(2026, 4, 28, 0, 0),
                LocalDateTime.of(2026, 4, 30, 0, 0)
        );

        Assert.assertFalse(system.isVacationModeActive());
        Assert.assertEquals(1, system.executeDueSchedules());
        Assert.assertTrue(lamp.isOn());
        Assert.assertEquals(0.0, dimmer.getValue(), 0.0001);
    }

    @Test
    public void simulationIgnoresVacationSchedules() {
        SmartHomeSystem system = new SmartHomeSystem();
        system.registerUser("owner@example.com", "password123");
        system.loginUser("owner@example.com", "password123");
        Room room = system.createRoom("Living Room");
        Device lamp = system.createDevice(room.getId(), "Lamp", DeviceType.SWITCH);
        system.createVacationSchedule(
                "Vacation Morning",
                lamp.getId(),
                ScheduleActionType.SET_VALUE,
                1.0,
                LocalTime.of(6, 0),
                Set.of(DayOfWeek.MONDAY)
        );

        DaySimulationResult result = system.simulateDay(new DaySimulationRequest(
                LocalDateTime.of(2026, 4, 27, 0, 0),
                java.util.Map.of(),
                Set.of()
        ));

        Assert.assertTrue(result.getEvents().isEmpty());
    }

    @Test
    public void memberCannotActivateOrDeactivateVacationMode() {
        SmartHomeSystem system = new SmartHomeSystem(
                new InMemoryUserRepository(),
                new InMemoryHomeRepository(),
                Clock.systemUTC()
        );
        system.registerUser("owner@example.com", "password123");
        system.loginUser("owner@example.com", "password123");
        system.inviteMember("member@example.com");
        Room room = system.createRoom("Living Room");
        Device lamp = system.createDevice(room.getId(), "Lamp", DeviceType.SWITCH);
        Schedule vacationSchedule = system.createVacationSchedule(
                "Vacation Morning",
                lamp.getId(),
                ScheduleActionType.SET_VALUE,
                1.0,
                LocalTime.of(6, 0),
                Set.of(DayOfWeek.MONDAY)
        );
        system.logoutUser();
        system.registerUser("member@example.com", "password123", UserRole.MEMBER);
        system.loginUser("member@example.com", "password123");

        Assert.assertThrows(
                IllegalStateException.class,
                () -> system.activateVacationMode(
                        vacationSchedule.getId(),
                        LocalDateTime.now().plusDays(1),
                        LocalDateTime.now().plusDays(2)
                )
        );
        Assert.assertThrows(IllegalStateException.class, system::deactivateVacationMode);
    }

    @Test
    public void vacationModeIsLoadedAgainAfterRestart() throws IOException {
        java.nio.file.Path databaseFile = java.nio.file.Files.createTempFile("smarthome-vacation", ".db");
        databaseFile.toFile().deleteOnExit();
        String databaseUrl = "jdbc:sqlite:" + databaseFile;

        SmartHomeSystem firstSystem = new SmartHomeSystem(
                new SQLiteUserRepository(databaseUrl),
                new SQLiteHomeRepository(databaseUrl),
                Clock.fixed(Instant.parse("2026-04-27T06:00:00Z"), ZoneOffset.UTC)
        );
        firstSystem.registerUser("owner@example.com", "password123");
        firstSystem.loginUser("owner@example.com", "password123");
        Room room = firstSystem.createRoom("Living Room");
        Device lamp = firstSystem.createDevice(room.getId(), "Lamp", DeviceType.SWITCH);
        Schedule schedule = firstSystem.createVacationSchedule(
                "Vacation Morning",
                lamp.getId(),
                ScheduleActionType.SET_VALUE,
                1.0,
                LocalTime.of(6, 0),
                Set.of(DayOfWeek.MONDAY)
        );
        firstSystem.activateVacationMode(
                schedule.getId(),
                LocalDateTime.of(2026, 4, 27, 0, 0),
                LocalDateTime.of(2026, 4, 30, 0, 0)
        );
        firstSystem.logoutUser();

        SmartHomeSystem secondSystem = new SmartHomeSystem(
                new SQLiteUserRepository(databaseUrl),
                new SQLiteHomeRepository(databaseUrl),
                Clock.fixed(Instant.parse("2026-04-28T06:00:00Z"), ZoneOffset.UTC)
        );
        secondSystem.loginUser("owner@example.com", "password123");

        Assert.assertNotNull(secondSystem.getVacationMode());
        Assert.assertEquals(schedule.getId(), secondSystem.getVacationMode().getScheduleId());
        Assert.assertTrue(secondSystem.isVacationModeActive());
    }

    private static final class MutableClock extends Clock {
        private Instant currentInstant;
        private final ZoneId zoneId;

        private MutableClock(Instant instant, ZoneId zoneId) {
            super();
            this.currentInstant = instant;
            this.zoneId = zoneId;
        }

        @Override
        public ZoneId getZone() {
            return zoneId;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(currentInstant, zone);
        }

        @Override
        public Instant instant() {
            return currentInstant;
        }

        private void setInstant(Instant newInstant) {
            this.currentInstant = newInstant;
        }
    }
}
