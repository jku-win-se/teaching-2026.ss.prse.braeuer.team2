package at.jku.se.smarthome.controller;

import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.model.DeviceType;
import at.jku.se.smarthome.model.Room;
import at.jku.se.smarthome.model.SceneDeviceState;
import at.jku.se.smarthome.model.SmartHomeSystem;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings("PMD")
public class SceneController {
    private final SmartHomeSystem system = SmartHomeSystem.createPersistentSystem();

    @FXML
    private VBox sceneListContainer;

    @FXML
    private Button createSceneButton;

    public void initialize() {
        if (!system.isUserLoggedIn()) {
            Platform.runLater(this::openAuthView);
            return;
        }
        configureRoleAccess();
        refreshSceneOverview();
    }

    @FXML
    public void openDashboard() {
        navigateTo("/at/jku/se/smarthome/fxml/main-view.fxml", "Failed to open dashboard view");
    }

    @FXML
    public void openRules() {
        navigateTo("/at/jku/se/smarthome/fxml/rules-view.fxml", "Failed to open rules view");
    }

    @FXML
    public void openSchedules() {
        navigateTo("/at/jku/se/smarthome/fxml/schedules-view.fxml", "Failed to open schedules view");
    }

    @FXML
    public void openActivity() {
        navigateTo("/at/jku/se/smarthome/fxml/activity-view.fxml", "Failed to open activity view");
    }

    @FXML
    public void openEnergy() {
        navigateTo("/at/jku/se/smarthome/fxml/energy-view.fxml", "Failed to open energy view");
    }

    @FXML
    public void logout() {
        system.logoutUser();
        openAuthView();
    }

    @FXML
    public void createScene() {
        Optional<String> sceneName = promptSceneName(null);
        if (sceneName.isEmpty()) {
            return;
        }

        List<SceneDeviceState> deviceStates = promptSceneDeviceStates(List.of());
        if (deviceStates.isEmpty()) {
            return;
        }

        try {
            system.createScene(sceneName.get(), deviceStates);
            refreshSceneOverview();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showMessage("Invalid scene", exception.getMessage());
        }
    }

    private void configureRoleAccess() {
        boolean owner = system.isCurrentUserOwner();
        createSceneButton.setVisible(owner);
        createSceneButton.setManaged(owner);
    }

    private void refreshSceneOverview() {
        sceneListContainer.getChildren().clear();

        List<at.jku.se.smarthome.model.Scene> scenes = system.getScenes();
        if (scenes.isEmpty()) {
            Label emptyState = new Label("No scenes yet. Create your first scene.");
            emptyState.setStyle("-fx-text-fill: #6e6257;");
            sceneListContainer.getChildren().add(emptyState);
            return;
        }

        for (at.jku.se.smarthome.model.Scene scene : scenes) {
            sceneListContainer.getChildren().add(createSceneCard(scene));
        }
    }

    private HBox createSceneCard(at.jku.se.smarthome.model.Scene scene) {
        Label sceneName = new Label(scene.getName() + " (" + scene.getDeviceStates().size() + " devices)");
        sceneName.setStyle("-fx-font-size: 14; -fx-text-fill: #2b2b2b;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button activationButton = new Button(system.isSceneActive(scene.getId()) ? "Deactivate" : "Activate");
        activationButton.setStyle("-fx-background-color: #e8752e; -fx-text-fill: white; -fx-background-radius: 20;");
        activationButton.setOnAction(event -> toggleScene(scene));

        Button editButton = new Button("Edit");
        editButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #6e6257;");
        editButton.setOnAction(event -> editScene(scene));

        Button deleteButton = new Button("Delete");
        deleteButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #b04a2f;");
        deleteButton.setOnAction(event -> deleteScene(scene));

        HBox sceneCard = new HBox(8, sceneName, spacer, activationButton);
        if (system.isCurrentUserOwner()) {
            sceneCard.getChildren().addAll(editButton, deleteButton);
        }
        sceneCard.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 12 14 12 14;");
        return sceneCard;
    }

    private void toggleScene(at.jku.se.smarthome.model.Scene scene) {
        try {
            if (system.isSceneActive(scene.getId())) {
                system.deactivateScene(scene.getId());
            } else {
                system.activateScene(scene.getId());
            }
            refreshSceneOverview();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showMessage("Scene update failed", exception.getMessage());
        }
    }

    private void editScene(at.jku.se.smarthome.model.Scene scene) {
        Optional<String> sceneName = promptSceneName(scene.getName());
        if (sceneName.isEmpty()) {
            return;
        }

        List<SceneDeviceState> deviceStates = promptSceneDeviceStates(scene.getDeviceStates());
        if (deviceStates.isEmpty()) {
            return;
        }

        try {
            system.updateScene(scene.getId(), sceneName.get(), deviceStates);
            refreshSceneOverview();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showMessage("Invalid scene", exception.getMessage());
        }
    }

    private void deleteScene(at.jku.se.smarthome.model.Scene scene) {
        system.removeScene(scene.getId());
        refreshSceneOverview();
    }

    private Optional<String> promptSceneName(String existingName) {
        TextInputDialog dialog = new TextInputDialog(existingName == null ? "" : existingName);
        dialog.setTitle(existingName == null ? "Create Scene" : "Edit Scene");
        dialog.setHeaderText(existingName == null ? "Create scene" : "Edit scene");
        dialog.setContentText("Scene name:");
        return dialog.showAndWait();
    }

    private List<SceneDeviceState> promptSceneDeviceStates(List<SceneDeviceState> existingStates) {
        List<SceneDeviceState> deviceStates = new ArrayList<>(existingStates);
        while (confirmAddSceneDeviceState(deviceStates)) {
            Optional<SceneDeviceState> nextState = promptSingleSceneDeviceState();
            if (nextState.isEmpty()) {
                break;
            }
            removeExistingSceneDeviceState(deviceStates, nextState.get().getDeviceId());
            deviceStates.add(nextState.get());
        }
        return deviceStates;
    }

    private boolean confirmAddSceneDeviceState(List<SceneDeviceState> deviceStates) {
        Alert dialog = new Alert(AlertType.CONFIRMATION);
        dialog.setTitle("Scene Devices");
        dialog.setHeaderText("Scene contains " + deviceStates.size() + " device states.");
        dialog.setContentText("Add another device state?");
        Optional<ButtonType> result = dialog.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private Optional<SceneDeviceState> promptSingleSceneDeviceState() {
        List<Device> devices = getAllDevices();
        if (devices.isEmpty()) {
            showMessage("No devices", "Create a device first before adding scene states.");
            return Optional.empty();
        }

        Optional<Device> selectedDevice = selectDevice(devices);
        if (selectedDevice.isEmpty()) {
            return Optional.empty();
        }

        return promptTargetValue(selectedDevice.get())
                .map(targetValue -> new SceneDeviceState(selectedDevice.get().getId(), targetValue));
    }

    private void removeExistingSceneDeviceState(List<SceneDeviceState> deviceStates, String deviceId) {
        deviceStates.removeIf(deviceState -> deviceState.getDeviceId().equals(deviceId));
    }

    private List<Device> getAllDevices() {
        List<Device> devices = new ArrayList<>();
        for (Room room : system.getRooms()) {
            devices.addAll(room.getDevices());
        }
        return devices;
    }

    private Optional<Device> selectDevice(List<Device> devices) {
        Map<String, Device> optionsToDevice = new LinkedHashMap<>();
        for (Device device : devices) {
            String deviceId = device.getId();
            String deviceIdPreview = deviceId.length() > 6 ? deviceId.substring(0, 6) : deviceId;
            optionsToDevice.put(device.getName() + " (" + formatDeviceType(device.getType()) + ") ["
                    + deviceIdPreview + "]", device);
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(optionsToDevice.keySet().iterator().next(),
                optionsToDevice.keySet());
        dialog.setTitle("Select Device");
        dialog.setHeaderText("Choose scene device");
        dialog.setContentText("Device:");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(optionsToDevice.get(result.get()));
    }

    private Optional<Double> promptTargetValue(Device device) {
        TextInputDialog dialog = new TextInputDialog(defaultSceneTargetValue(device));
        dialog.setTitle("Target State");
        dialog.setHeaderText("Set target state for " + device.getName());
        dialog.setContentText(sceneTargetPrompt(device));

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return Optional.empty();
        }

        try {
            return Optional.of(Double.parseDouble(result.get().trim()));
        } catch (NumberFormatException exception) {
            showMessage("Invalid value", "Please enter a numeric value.");
            return Optional.empty();
        }
    }

    private String defaultSceneTargetValue(Device device) {
        if (device.getType() == DeviceType.SWITCH) {
            return device.isOn() ? "1" : "0";
        }
        return String.valueOf(device.getValue());
    }

    private String sceneTargetPrompt(Device device) {
        return switch (device.getType()) {
            case SWITCH -> "Target value (0 = Off, 1 = On):";
            case DIMMER -> "Target brightness (0-100):";
            case THERMOSTAT -> "Target temperature:";
            case SENSOR -> "Target sensor value:";
            case BLIND -> "Target position (0 = Closed, 100 = Open):";
        };
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

    private void navigateTo(String resourcePath, String errorMessage) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneController.class.getResource(resourcePath));
            Scene scene = new Scene(loader.load(), 1000, 600);
            Stage stage = (Stage) sceneListContainer.getScene().getWindow();
            stage.setScene(scene);
        } catch (IOException exception) {
            throw new IllegalStateException(errorMessage, exception);
        }
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
                    SceneController.class.getResource("/at/jku/se/smarthome/fxml/auth-view.fxml")
            );
            Scene scene = new Scene(loader.load(), 900, 600);
            if (sceneListContainer.getScene() == null) {
                Platform.runLater(this::openAuthView);
                return;
            }
            Stage stage = (Stage) sceneListContainer.getScene().getWindow();
            stage.setScene(scene);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to open login view", exception);
        }
    }
}
