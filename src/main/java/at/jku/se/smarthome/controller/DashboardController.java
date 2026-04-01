package at.jku.se.smarthome.controller;

import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.model.DeviceType;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;

public class DashboardController {

    private Device switchDevice;
    private Device dimmerDevice;
    private Device thermostatDevice;

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
        switchDevice = new Device("1", "Ceiling Light", DeviceType.SWITCH);
        dimmerDevice = new Device("2", "Floor Lamp", DeviceType.DIMMER);
        thermostatDevice = new Device("3", "Thermostat", DeviceType.THERMOSTAT);

        dimmerDevice.setValue(75);
        thermostatDevice.setValue(22);

        updateUI();
    }

    @FXML
    public void toggleDevice() {
        switchDevice.toggle();
        updateUI();
    }

    @FXML
    public void changeDeviceValue(Event event) {
        Object source = event.getSource();

        if (source == dimmerSlider) {
            int roundedValue = (int) Math.round(dimmerSlider.getValue());
            dimmerSlider.setValue(roundedValue);
            dimmerDevice.setValue(roundedValue);
        } else if (source == thermostatSlider) {
            int roundedValue = (int) Math.round(thermostatSlider.getValue());
            thermostatSlider.setValue(roundedValue);
            thermostatDevice.setValue(roundedValue);
        }

        updateUI();
    }

    private void updateUI() {
        toggleButton.setText(switchDevice.isOn() ? "On" : "Off");
        switchStatus.setText(switchDevice.getStatusText());

        dimmerStatus.setText(dimmerDevice.getStatusText());
        dimmerSlider.setValue(dimmerDevice.getValue());

        thermostatStatus.setText(thermostatDevice.getStatusText());
        thermostatSlider.setValue(thermostatDevice.getValue());
    }
}