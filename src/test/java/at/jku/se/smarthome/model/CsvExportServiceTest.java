package at.jku.se.smarthome.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import org.junit.Test;

@SuppressWarnings("PMD")
public class CsvExportServiceTest {
    @Test
    public void buildActivityLogCsvExportsAllRelevantFieldsAndEscapesValues() {
        ActivityLogEntry entry = new ActivityLogEntry(
                Instant.parse("2026-04-30T08:15:00Z"),
                "device-1",
                "Lamp, Kitchen",
                ActivityActorType.USER,
                "owner@example.com",
                "Off",
                "On \"bright\""
        );

        String csvContent = CsvExportService.buildActivityLogCsv(List.of(entry));

        assertEquals(
                "timestamp,device_id,device_name,actor_type,actor_name,previous_state,new_state\n"
                        + "2026-04-30T08:15:00Z,device-1,\"Lamp, Kitchen\",USER,owner@example.com,Off,"
                        + "\"On \"\"bright\"\"\"\n",
                csvContent
        );
    }

    @Test
    public void buildActivityLogCsvExportsHeaderForEmptyLog() {
        String csvContent = CsvExportService.buildActivityLogCsv(List.of());

        assertEquals("timestamp,device_id,device_name,actor_type,actor_name,previous_state,new_state\n", csvContent);
    }

    @Test
    public void writeActivityLogCsvWritesUtf8File() throws IOException {
        Path exportPath = Files.createTempFile("activity-export", ".csv");
        exportPath.toFile().deleteOnExit();
        ActivityLogEntry entry = new ActivityLogEntry(
                Instant.parse("2026-04-30T09:00:00Z"),
                "device-1",
                "Lamp",
                ActivityActorType.RULE,
                "Morning rule",
                "Off",
                "On"
        );

        CsvExportService.writeActivityLogCsv(exportPath, List.of(entry));

        assertTrue(Files.readString(exportPath).contains("Morning rule"));
    }

    @Test
    public void buildEnergyDashboardCsvExportsDeviceRoomAndHouseholdData() {
        EnergyDeviceConsumption lampConsumption = new EnergyDeviceConsumption(
                "room-1",
                "Living Room",
                "device-1",
                "Lamp",
                DeviceType.SWITCH,
                0.36
        );
        EnergyDeviceConsumption dimmerConsumption = new EnergyDeviceConsumption(
                "room-1",
                "Living Room",
                "device-2",
                "Dimmer",
                DeviceType.DIMMER,
                0.48
        );
        EnergyDashboard dashboard = new EnergyDashboard(
                EnergyAggregationPeriod.DAY,
                Instant.parse("2026-04-30T00:00:00Z"),
                Instant.parse("2026-04-30T12:00:00Z"),
                List.of(new EnergyRoomConsumption("room-1", "Living Room",
                        List.of(lampConsumption, dimmerConsumption)))
        );

        String csvContent = CsvExportService.buildEnergyDashboardCsv(dashboard);

        assertEquals(
                "period,start,end,room_id,room_name,room_total_kwh,device_id,device_name,device_type,"
                        + "device_consumption_kwh,household_total_kwh\n"
                        + "DAY,2026-04-30T00:00:00Z,2026-04-30T12:00:00Z,room-1,Living Room,0.840,"
                        + "device-1,Lamp,SWITCH,0.360,0.840\n"
                        + "DAY,2026-04-30T00:00:00Z,2026-04-30T12:00:00Z,room-1,Living Room,0.840,"
                        + "device-2,Dimmer,DIMMER,0.480,0.840\n",
                csvContent
        );
    }

    @Test
    public void buildEnergyDashboardCsvKeepsRoomsWithoutDevicesVisible() {
        EnergyDashboard dashboard = new EnergyDashboard(
                EnergyAggregationPeriod.WEEK,
                Instant.parse("2026-04-27T00:00:00Z"),
                Instant.parse("2026-04-30T12:00:00Z"),
                List.of(new EnergyRoomConsumption("room-1", "Empty Room", List.of()))
        );

        String csvContent = CsvExportService.buildEnergyDashboardCsv(dashboard);

        assertTrue(csvContent.contains("WEEK,2026-04-27T00:00:00Z,2026-04-30T12:00:00Z,room-1,Empty Room,0.000,,,,,0.000"));
    }

    @Test
    public void writeEnergyDashboardCsvWritesUtf8File() throws IOException {
        Path exportPath = Files.createTempFile("energy-export", ".csv");
        exportPath.toFile().deleteOnExit();
        EnergyDashboard dashboard = new EnergyDashboard(
                EnergyAggregationPeriod.DAY,
                Instant.parse("2026-04-30T00:00:00Z"),
                Instant.parse("2026-04-30T12:00:00Z"),
                List.of(new EnergyRoomConsumption("room-1", "Office", List.of()))
        );

        CsvExportService.writeEnergyDashboardCsv(exportPath, dashboard);

        assertTrue(Files.readString(exportPath).contains("Office"));
    }

    @Test
    public void csvExportRejectsMissingRequiredInput() {
        assertThrows(IllegalArgumentException.class, () -> CsvExportService.buildActivityLogCsv(null));
        assertThrows(IllegalArgumentException.class, () -> CsvExportService.buildEnergyDashboardCsv(null));
        assertThrows(IllegalArgumentException.class, () -> CsvExportService.writeActivityLogCsv(null, List.of()));
    }
}
