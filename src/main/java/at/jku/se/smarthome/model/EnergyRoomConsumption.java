package at.jku.se.smarthome.model;

import java.util.List;

/**
 * Immutable estimated energy consumption for one room in one aggregation period.
 */
@SuppressWarnings("PMD.DataClass")
public final class EnergyRoomConsumption {
    private final String roomId;
    private final String roomName;
    private final List<EnergyDeviceConsumption> deviceConsumptions;
    private final double totalConsumptionKiloWattHours;

    /**
     * Creates a room energy aggregate.
     *
     * @param roomId the room id
     * @param roomName the room name
     * @param deviceConsumptions the device breakdown of the room
     */
    public EnergyRoomConsumption(String roomId, String roomName, List<EnergyDeviceConsumption> deviceConsumptions) {
        this.roomId = requireNonBlank(roomId, "Room id must not be blank");
        this.roomName = requireNonBlank(roomName, "Room name must not be blank");
        this.deviceConsumptions = List.copyOf(requireDevices(deviceConsumptions));
        this.totalConsumptionKiloWattHours = sumDeviceConsumption(this.deviceConsumptions);
    }

    public String getRoomId() {
        return roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public List<EnergyDeviceConsumption> getDeviceConsumptions() {
        return deviceConsumptions;
    }

    public double getTotalConsumptionKiloWattHours() {
        return totalConsumptionKiloWattHours;
    }

    private static List<EnergyDeviceConsumption> requireDevices(
            List<EnergyDeviceConsumption> deviceConsumptions) {
        if (deviceConsumptions == null) {
            throw new IllegalArgumentException("Device consumptions must not be null");
        }
        return deviceConsumptions;
    }

    private static double sumDeviceConsumption(List<EnergyDeviceConsumption> deviceConsumptions) {
        double sum = 0.0;
        for (EnergyDeviceConsumption deviceConsumption : deviceConsumptions) {
            sum += deviceConsumption.getConsumptionKiloWattHours();
        }
        return sum;
    }

    private static String requireNonBlank(String candidate, String message) {
        if (candidate == null || candidate.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return candidate.trim();
    }
}
