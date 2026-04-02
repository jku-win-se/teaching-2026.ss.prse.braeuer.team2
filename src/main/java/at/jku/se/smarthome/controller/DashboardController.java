package at.jku.se.smarthome.controller;

import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.model.DeviceType;
import at.jku.se.smarthome.model.Room;
import at.jku.se.smarthome.model.SmartHomeSystem;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;

import java.util.Optional;

public class DashboardController {

    private SmartHomeSystem system;
    private Room livingRoom;

    private Device switchDevice;
    private Device dimmerDevice;
    private Device thermostatDevice;

    @FXML
    private VBox switchCard;

    @FXML
    private VBox dimmerCard;

    @FXML
    private VBox thermostatCard;

    @FXML
    private Label switchName;

    @FXML
    private Label dimmerName;

    @FXML
    private Label thermostatName;

    @FXML
    private Button toggleButton;

    @FXML
    private Label switchStatus;

    @FXML
    private Slider dimmerSlider;

    @FXML
    private Label dimmerStatus;

    @FXML
    private Slider thermostatSlider;

    @FXML
    private Label thermostatStatus;

    public void initialize() {
        loadRooms();
    }

    public void loadRooms() {
        system = new SmartHomeSystem();
        livingRoom = new Room("r1", "Living Room");
        system.addRoom(livingRoom);

        switchDevice = new Device("1", "Ceiling Light", DeviceType.SWITCH);
        dimmerDevice = new Device("2", "Floor Lamp", DeviceType.DIMMER);
        thermostatDevice = new Device("3", "Thermostat", DeviceType.THERMOSTAT);

        livingRoom.addDevice(switchDevice);
        livingRoom.addDevice(dimmerDevice);
        livingRoom.addDevice(thermostatDevice);

        dimmerDevice.setValue(75);
        thermostatDevice.setValue(22);

        updateUI();
    }

    @FXML
    public void toggleDevice() {
        if (switchDevice == null) {
            return;
        }

        switchDevice.toggle();
        updateUI();
    }

    @FXML
    public void changeDeviceValue(Event event) {
        Object source = event.getSource();

        if (source == dimmerSlider && dimmerDevice != null) {
            int roundedValue = (int) Math.round(dimmerSlider.getValue());
            dimmerSlider.setValue(roundedValue);
            dimmerDevice.setValue(roundedValue);
        } else if (source == thermostatSlider && thermostatDevice != null) {
            int roundedValue = (int) Math.round(thermostatSlider.getValue());
            thermostatSlider.setValue(roundedValue);
            thermostatDevice.setValue(roundedValue);
        }

        updateUI();
    }

    @FXML
    public void renameSwitchDevice() {
        renameDevice(switchDevice);
    }

    @FXML
    public void renameDimmerDevice() {
        renameDevice(dimmerDevice);
    }

    @FXML
    public void renameThermostatDevice() {
        renameDevice(thermostatDevice);
    }

    @FXML
    public void removeSwitchDevice() {
        removeDevice(switchDevice, switchCard);
        switchDevice = null;
        updateUI();
    }

    @FXML
    public void removeDimmerDevice() {
        removeDevice(dimmerDevice, dimmerCard);
        dimmerDevice = null;
        updateUI();
    }

    @FXML
    public void removeThermostatDevice() {
        removeDevice(thermostatDevice, thermostatCard);
        thermostatDevice = null;
        updateUI();
    }

    private void renameDevice(Device device) {
        if (device == null) {
            return;
        }

        TextInputDialog dialog = new TextInputDialog(device.getName());
        dialog.setTitle("Rename Device");
        dialog.setHeaderText("Rename device");
        dialog.setContentText("New name:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            system.renameDevice(device.getId(), result.get());
            updateUI();
        }
    }

    private void removeDevice(Device device, VBox card) {
        if (device == null) {
            return;
        }

        system.removeDevice(device.getId());
        card.setVisible(false);
        card.setManaged(false);
    }

    private void updateUI() {
        if (switchDevice != null) {
            switchName.setText(switchDevice.getName());
            toggleButton.setText(switchDevice.isOn() ? "On" : "Off");
            switchStatus.setText(switchDevice.getStatusText());
        }

        if (dimmerDevice != null) {
            dimmerName.setText(dimmerDevice.getName());
            dimmerStatus.setText(dimmerDevice.getStatusText());
            dimmerSlider.setValue(dimmerDevice.getValue());
        }

        if (thermostatDevice != null) {
            thermostatName.setText(thermostatDevice.getName());
            thermostatStatus.setText(thermostatDevice.getStatusText());
            thermostatSlider.setValue(thermostatDevice.getValue());
        }
    }
}