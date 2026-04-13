package at.jku.se.smarthome.controller;

import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.model.DeviceType;
import at.jku.se.smarthome.model.Room;
import at.jku.se.smarthome.model.SmartHomeSystem;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DashboardController {

    private SmartHomeSystem system;

    @FXML
    private VBox roomListContainer;

    public void initialize() {
        system = SmartHomeSystem.createPersistentSystem();
        if (!system.isUserLoggedIn()) {
            Platform.runLater(this::openAuthView);
            return;
        }
        refreshRoomOverview();
    }

    @FXML
    public void logout() {
        system.logoutUser();
        openAuthView();
    }

    @FXML
    public void createRoom() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Create Room");
        dialog.setHeaderText("Create room");
        dialog.setContentText("Room name:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                system.createRoom(result.get());
                refreshRoomOverview();
            } catch (IllegalArgumentException exception) {
                showMessage("Invalid room name", exception.getMessage());
            }
        }
    }

    @FXML
    public void createDevice() {
        List<Room> rooms = system.getRooms();
        if (rooms.isEmpty()) {
            showMessage("No rooms", "Create a room first before adding a device.");
            return;
        }

        Optional<Room> selectedRoom = selectRoom(rooms);
        if (selectedRoom.isEmpty()) {
            return;
        }

        createDeviceForRoom(selectedRoom.get());
    }

    private void refreshRoomOverview() {
        roomListContainer.getChildren().clear();

        for (Room room : system.getRooms()) {
            roomListContainer.getChildren().add(createRoomCard(room));
        }

        if (system.getRooms().isEmpty()) {
            Label emptyState = new Label("No rooms yet. Create your first room.");
            emptyState.setStyle("-fx-text-fill: #6e6257;");
            roomListContainer.getChildren().add(emptyState);
        }
    }

    private VBox createRoomCard(Room room) {
        Label roomName = new Label(room.getName());
        roomName.setStyle("-fx-font-size: 16; -fx-text-fill: #2b2b2b;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addDeviceButton = new Button("+ Device");
        addDeviceButton.setStyle("-fx-background-color: #e3ded7; -fx-text-fill: #2b2b2b; -fx-background-radius: 8;");
        addDeviceButton.setOnAction(event -> createDeviceForRoom(room));

        Button renameRoomButton = new Button("Rename");
        renameRoomButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #6e6257;");
        renameRoomButton.setOnAction(event -> renameRoom(room));

        Button deleteRoomButton = new Button("Delete");
        deleteRoomButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #b04a2f;");
        deleteRoomButton.setOnAction(event -> deleteRoom(room));

        HBox roomHeader = new HBox(8, roomName, spacer, addDeviceButton, renameRoomButton, deleteRoomButton);

        VBox devicesContainer = new VBox(8);
        if (room.getDevices().isEmpty()) {
            Label emptyDevicesLabel = new Label("No devices yet.");
            emptyDevicesLabel.setStyle("-fx-text-fill: #8a6f5a;");
            devicesContainer.getChildren().add(emptyDevicesLabel);
        } else {
            for (Device device : room.getDevices()) {
                devicesContainer.getChildren().add(createDeviceCard(device));
            }
        }

        VBox roomCard = new VBox(12, roomHeader, devicesContainer);
        roomCard.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 12 14 12 14;");
        return roomCard;
    }

    private VBox createDeviceCard(Device device) {
        Label deviceName = new Label(device.getName() + " (" + formatDeviceType(device.getType()) + ")");
        deviceName.setStyle("-fx-font-size: 14; -fx-text-fill: #2b2b2b;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button renameDeviceButton = new Button("Rename");
        renameDeviceButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #6e6257;");
        renameDeviceButton.setOnAction(event -> renameDevice(device));

        Button deleteDeviceButton = new Button("Delete");
        deleteDeviceButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #b04a2f;");
        deleteDeviceButton.setOnAction(event -> deleteDevice(device));

        HBox deviceHeader = new HBox(8, deviceName, spacer, renameDeviceButton, deleteDeviceButton);

        VBox controls = buildDeviceControls(device);

        VBox deviceCard = new VBox(8, deviceHeader, controls);
        deviceCard.setStyle("-fx-background-color: #f7f5f2; -fx-background-radius: 10; -fx-padding: 10 12 10 12;");
        return deviceCard;
    }

    private VBox buildDeviceControls(Device device) {
        Label statusLabel = new Label(device.getStatusText());
        statusLabel.setStyle("-fx-text-fill: #8a6f5a;");

        return switch (device.getType()) {
            case SWITCH -> {
                Button toggleButton = new Button(device.isOn() ? "On" : "Off");
                toggleButton.setStyle("-fx-background-color: #e8752e; -fx-text-fill: white; -fx-background-radius: 20;");
                toggleButton.setOnAction(event -> {
                    device.toggle();
                    refreshRoomOverview();
                });
                yield new VBox(6, toggleButton);
            }
            case DIMMER -> {
                Slider dimmerSlider = new Slider(0, 100, device.getValue());
                dimmerSlider.setStyle("-fx-accent: #e8752e;");
                dimmerSlider.setMajorTickUnit(1);
                dimmerSlider.setMinorTickCount(0);
                dimmerSlider.setBlockIncrement(1);
                dimmerSlider.setSnapToTicks(true);
                dimmerSlider.setOnMouseReleased(event -> {
                    int roundedValue = (int) Math.round(dimmerSlider.getValue());
                    device.setValue(roundedValue);
                    refreshRoomOverview();
                });
                yield new VBox(6, statusLabel, dimmerSlider);
            }
            case THERMOSTAT -> {
                Slider thermostatSlider = new Slider(0, 40, device.getValue());
                thermostatSlider.setStyle("-fx-accent: #e8752e;");
                thermostatSlider.setMajorTickUnit(1);
                thermostatSlider.setMinorTickCount(0);
                thermostatSlider.setBlockIncrement(1);
                thermostatSlider.setSnapToTicks(true);
                thermostatSlider.setOnMouseReleased(event -> {
                    int roundedValue = (int) Math.round(thermostatSlider.getValue());
                    device.setValue(roundedValue);
                    refreshRoomOverview();
                });
                yield new VBox(6, statusLabel, thermostatSlider);
            }
            case BLIND -> {
                Button blindToggle = new Button(device.isOn() ? "Open" : "Closed");
                blindToggle.setStyle("-fx-background-color: #e8752e; -fx-text-fill: white; -fx-background-radius: 20;");
                blindToggle.setOnAction(event -> {
                    if (device.isOn()) {
                        device.setValue(0);
                    } else {
                        device.setValue(100);
                    }
                    refreshRoomOverview();
                });
                yield new VBox(6, blindToggle);
            }
            case SENSOR -> {
                Button setSensorValueButton = new Button("Set Value");
                setSensorValueButton.setStyle("-fx-background-color: #e8752e; -fx-text-fill: white; -fx-background-radius: 20;");
                setSensorValueButton.setOnAction(event -> setSensorValue(device));
                yield new VBox(6, statusLabel, setSensorValueButton);
            }
        };
    }

    private void renameRoom(Room room) {
        TextInputDialog dialog = new TextInputDialog(room.getName());
        dialog.setTitle("Rename Room");
        dialog.setHeaderText("Rename room");
        dialog.setContentText("New name:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                system.renameRoom(room.getId(), result.get());
                refreshRoomOverview();
            } catch (IllegalArgumentException exception) {
                showMessage("Invalid room name", exception.getMessage());
            }
        }
    }

    private void deleteRoom(Room room) {
        system.removeRoom(room.getId());
        refreshRoomOverview();
    }

    private void renameDevice(Device device) {
        TextInputDialog dialog = new TextInputDialog(device.getName());
        dialog.setTitle("Rename Device");
        dialog.setHeaderText("Rename device");
        dialog.setContentText("New name:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                system.renameDevice(device.getId(), result.get());
                refreshRoomOverview();
            } catch (IllegalArgumentException exception) {
                showMessage("Invalid device name", exception.getMessage());
            }
        }
    }

    private void deleteDevice(Device device) {
        system.removeDevice(device.getId());
        refreshRoomOverview();
    }

    private void setSensorValue(Device device) {
        TextInputDialog dialog = new TextInputDialog(String.valueOf(device.getValue()));
        dialog.setTitle("Set Sensor Value");
        dialog.setHeaderText("Update sensor value");
        dialog.setContentText("Value:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                double value = Double.parseDouble(result.get().trim());
                device.setValue(value);
                refreshRoomOverview();
            } catch (NumberFormatException exception) {
                showMessage("Invalid value", "Please enter a numeric value.");
            }
        }
    }

    private Optional<Room> selectRoom(List<Room> rooms) {
        Map<String, Room> optionsToRoom = new LinkedHashMap<>();
        for (Room room : rooms) {
            String roomId = room.getId();
            String roomIdPreview = roomId.length() > 6 ? roomId.substring(0, 6) : roomId;
            optionsToRoom.put(room.getName() + " [" + roomIdPreview + "]", room);
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(optionsToRoom.keySet().iterator().next(), optionsToRoom.keySet());
        dialog.setTitle("Select Room");
        dialog.setHeaderText("Choose target room");
        dialog.setContentText("Room:");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(optionsToRoom.get(result.get()));
    }

    private void createDeviceForRoom(Room room) {
        Optional<String> selectedDeviceName = promptDeviceName();
        if (selectedDeviceName.isEmpty()) {
            return;
        }

        Optional<DeviceType> selectedDeviceType = selectDeviceType();
        if (selectedDeviceType.isEmpty()) {
            return;
        }

        try {
            system.createDevice(room.getId(), selectedDeviceName.get(), selectedDeviceType.get());
            refreshRoomOverview();
        } catch (IllegalArgumentException exception) {
            showMessage("Invalid device", exception.getMessage());
        }
    }

    private Optional<String> promptDeviceName() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Create Device");
        dialog.setHeaderText("Create device");
        dialog.setContentText("Device name:");
        return dialog.showAndWait();
    }

    private Optional<DeviceType> selectDeviceType() {
        ChoiceDialog<String> dialog = new ChoiceDialog<>("Switch", "Switch", "Dimmer", "Thermostat", "Sensor",
                "Jalousie");
        dialog.setTitle("Device Type");
        dialog.setHeaderText("Select device type");
        dialog.setContentText("Type:");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(switch (result.get()) {
            case "Switch" -> DeviceType.SWITCH;
            case "Dimmer" -> DeviceType.DIMMER;
            case "Thermostat" -> DeviceType.THERMOSTAT;
            case "Sensor" -> DeviceType.SENSOR;
            case "Jalousie" -> DeviceType.BLIND;
            default -> throw new IllegalArgumentException("Unknown device type");
        });
    }

    private String formatDeviceType(DeviceType deviceType) {
        return switch (deviceType) {
            case SWITCH -> "Switch";
            case DIMMER -> "Dimmer";
            case THERMOSTAT -> "Thermostat";
            case SENSOR -> "Sensor";
            case BLIND -> "Jalousie";
        };
    }

    private void showMessage(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void openAuthView() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    DashboardController.class.getResource("/at/jku/se/smarthome/fxml/auth-view.fxml")
            );
            Scene scene = new Scene(loader.load(), 900, 600);
            if (roomListContainer.getScene() == null) {
                Platform.runLater(this::openAuthView);
                return;
            }
            Stage stage = (Stage) roomListContainer.getScene().getWindow();
            stage.setScene(scene);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to open login view", exception);
        }
    }
}
