package at.jku.se.smarthome.model;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Creates CSV exports for activity log and energy dashboard data.
 */
@SuppressWarnings({
        "PMD.TooManyMethods",
        "PMD.UseObjectForClearerAPI"
})
public final class CsvExportService {
    /**
     * Separator used between CSV records.
     */
    private static final String LINE_SEPARATOR = "\n";
    /**
     * Separator used between CSV fields.
     */
    private static final String FIELD_SEPARATOR = ",";
    /**
     * Header for activity log exports.
     */
    private static final String ACTIVITY_HEADER =
            "timestamp,device_id,device_name,actor_type,actor_name,previous_state,new_state";
    /**
     * Header for energy dashboard exports.
     */
    private static final String ENERGY_HEADER =
            "period,start,end,room_id,room_name,room_total_kwh,device_id,device_name,device_type,"
                    + "device_consumption_kwh,household_total_kwh";

    private CsvExportService() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Builds a CSV document for the provided activity log entries.
     *
     * @param activityLog the activity log entries
     * @return the CSV document
     */
    public static String buildActivityLogCsv(List<ActivityLogEntry> activityLog) {
        List<ActivityLogEntry> safeActivityLog = requireActivityLog(activityLog);
        StringBuilder csvBuilder = new StringBuilder(ACTIVITY_HEADER).append(LINE_SEPARATOR);
        for (ActivityLogEntry entry : safeActivityLog) {
            appendActivityEntry(csvBuilder, entry);
        }
        return csvBuilder.toString();
    }

    /**
     * Builds a CSV document for the provided energy dashboard.
     *
     * @param dashboard the energy dashboard
     * @return the CSV document
     */
    public static String buildEnergyDashboardCsv(EnergyDashboard dashboard) {
        EnergyDashboard safeDashboard = requireDashboard(dashboard);
        StringBuilder csvBuilder = new StringBuilder(ENERGY_HEADER).append(LINE_SEPARATOR);
        for (EnergyRoomConsumption roomConsumption : safeDashboard.getRoomConsumptions()) {
            appendRoomConsumption(csvBuilder, safeDashboard, roomConsumption);
        }
        return csvBuilder.toString();
    }

    /**
     * Writes an activity log CSV document.
     *
     * @param exportPath the target file path
     * @param activityLog the activity log entries
     */
    public static void writeActivityLogCsv(Path exportPath, List<ActivityLogEntry> activityLog) {
        writeCsv(exportPath, buildActivityLogCsv(activityLog));
    }

    /**
     * Writes an energy dashboard CSV document.
     *
     * @param exportPath the target file path
     * @param dashboard the energy dashboard
     */
    public static void writeEnergyDashboardCsv(Path exportPath, EnergyDashboard dashboard) {
        writeCsv(exportPath, buildEnergyDashboardCsv(dashboard));
    }

    private static void appendActivityEntry(StringBuilder csvBuilder, ActivityLogEntry entry) {
        appendFields(
                csvBuilder,
                entry.getTimestamp().toString(),
                entry.getDeviceId(),
                entry.getDeviceName(),
                entry.getActorType().name(),
                entry.getActorName(),
                entry.getPreviousState(),
                entry.getNewState()
        );
    }

    private static void appendRoomConsumption(StringBuilder csvBuilder, EnergyDashboard dashboard,
                                              EnergyRoomConsumption roomConsumption) {
        if (roomConsumption.getDeviceConsumptions().isEmpty()) {
            appendEnergyFields(csvBuilder, dashboard, roomConsumption, null);
        } else {
            for (EnergyDeviceConsumption deviceConsumption : roomConsumption.getDeviceConsumptions()) {
                appendEnergyFields(csvBuilder, dashboard, roomConsumption, deviceConsumption);
            }
        }
    }

    private static void appendEnergyFields(StringBuilder csvBuilder, EnergyDashboard dashboard,
                                           EnergyRoomConsumption roomConsumption,
                                           EnergyDeviceConsumption deviceConsumption) {
        appendFields(
                csvBuilder,
                dashboard.getAggregationPeriod().name(),
                dashboard.getStartInclusive().toString(),
                dashboard.getEndExclusive().toString(),
                roomConsumption.getRoomId(),
                roomConsumption.getRoomName(),
                formatKiloWattHours(roomConsumption.getTotalConsumptionKiloWattHours()),
                resolveDeviceId(deviceConsumption),
                resolveDeviceName(deviceConsumption),
                resolveDeviceType(deviceConsumption),
                resolveDeviceConsumption(deviceConsumption),
                formatKiloWattHours(dashboard.getTotalConsumptionKiloWattHours())
        );
    }

    private static String resolveDeviceId(EnergyDeviceConsumption deviceConsumption) {
        String deviceId = "";
        if (deviceConsumption != null) {
            deviceId = deviceConsumption.getDeviceId();
        }
        return deviceId;
    }

    private static String resolveDeviceName(EnergyDeviceConsumption deviceConsumption) {
        String deviceName = "";
        if (deviceConsumption != null) {
            deviceName = deviceConsumption.getDeviceName();
        }
        return deviceName;
    }

    private static String resolveDeviceType(EnergyDeviceConsumption deviceConsumption) {
        String deviceType = "";
        if (deviceConsumption != null) {
            deviceType = deviceConsumption.getDeviceType().name();
        }
        return deviceType;
    }

    private static String resolveDeviceConsumption(EnergyDeviceConsumption deviceConsumption) {
        String consumption = "";
        if (deviceConsumption != null) {
            consumption = formatKiloWattHours(deviceConsumption.getConsumptionKiloWattHours());
        }
        return consumption;
    }

    private static void appendFields(StringBuilder csvBuilder, String... fields) {
        for (int index = 0; index < fields.length; index++) {
            if (index > 0) {
                csvBuilder.append(FIELD_SEPARATOR);
            }
            csvBuilder.append(escapeField(fields[index]));
        }
        csvBuilder.append(LINE_SEPARATOR);
    }

    private static String escapeField(String value) {
        String safeValue = value == null ? "" : value;
        String escapedValue = safeValue;
        if (requiresQuotes(safeValue)) {
            escapedValue = "\"" + safeValue.replace("\"", "\"\"") + "\"";
        }
        return escapedValue;
    }

    private static boolean requiresQuotes(String value) {
        return value.contains(FIELD_SEPARATOR) || value.contains("\"")
                || value.contains("\n") || value.contains("\r");
    }

    private static String formatKiloWattHours(double value) {
        return String.format(Locale.ENGLISH, "%.3f", value);
    }

    private static List<ActivityLogEntry> requireActivityLog(List<ActivityLogEntry> activityLog) {
        if (activityLog == null) {
            throw new IllegalArgumentException("Activity log must not be null");
        }
        return activityLog;
    }

    private static EnergyDashboard requireDashboard(EnergyDashboard dashboard) {
        if (dashboard == null) {
            throw new IllegalArgumentException("Energy dashboard must not be null");
        }
        return dashboard;
    }

    private static Path requireExportPath(Path exportPath) {
        if (exportPath == null) {
            throw new IllegalArgumentException("Export path must not be null");
        }
        return exportPath;
    }

    private static void writeCsv(Path exportPath, String csvContent) {
        try {
            Files.writeString(requireExportPath(exportPath), csvContent, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write CSV export", exception);
        }
    }
}
