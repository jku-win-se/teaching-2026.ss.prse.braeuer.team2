package at.jku.se.smarthome.controller;

import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.model.DeviceType;
import at.jku.se.smarthome.model.Room;
import at.jku.se.smarthome.model.SceneDeviceState;
import at.jku.se.smarthome.model.SmartHomeSystem;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
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
    public void openSimulation() {
        navigateTo("/at/jku/se/smarthome/fxml/simulation-view.fxml", "Failed to open simulation view");
    }

    @FXML
    public void logout() {
        system.logoutUser();
        openAuthView();
    }

    @FXML
    public void createScene() {
        Optional<SceneFormData> sceneData = showSceneEditor("Create Scene", "", List.of());
        if (sceneData.isEmpty()) {
            return;
        }

        try {
            system.createScene(sceneData.get().name(), sceneData.get().deviceStates());
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
        Label sceneName = new Label(scene.getName());
        sceneName.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #2b2b2b;");

        Label sceneSummary = new Label(scene.getDeviceStates().size() + " devices - " + summarizeSceneStates(scene));
        sceneSummary.setWrapText(true);
        sceneSummary.setStyle("-fx-font-size: 12; -fx-text-fill: #6e6257;");

        Label activeBadge = new Label(system.isSceneActive(scene.getId()) ? "Active" : "Inactive");
        activeBadge.setStyle(system.isSceneActive(scene.getId())
                ? "-fx-background-color: #e9f3e5; -fx-text-fill: #3f7c3a; -fx-background-radius: 12; -fx-padding: 4 10 4 10;"
                : "-fx-background-color: #f0ede8; -fx-text-fill: #6e6257; -fx-background-radius: 12; -fx-padding: 4 10 4 10;");

        VBox sceneInfo = new VBox(4, sceneName, sceneSummary);
        HBox.setHgrow(sceneInfo, Priority.ALWAYS);

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

        HBox sceneCard = new HBox(10, sceneInfo, spacer, activeBadge, activationButton);
        if (system.isCurrentUserOwner()) {
            sceneCard.getChildren().addAll(editButton, deleteButton);
        }
        sceneCard.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 14 16 14 16;");
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
        Optional<SceneFormData> sceneData = showSceneEditor("Edit Scene", scene.getName(), scene.getDeviceStates());
        if (sceneData.isEmpty()) {
            return;
        }

        try {
            system.updateScene(scene.getId(), sceneData.get().name(), sceneData.get().deviceStates());
            refreshSceneOverview();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showMessage("Invalid scene", exception.getMessage());
        }
    }

    private void deleteScene(at.jku.se.smarthome.model.Scene scene) {
        system.removeScene(scene.getId());
        refreshSceneOverview();
    }

    private Optional<SceneFormData> showSceneEditor(String title, String existingName,
            List<SceneDeviceState> existingStates) {
        List<Device> devices = getAllDevices();
        if (devices.isEmpty()) {
            showMessage("No devices", "Create a device first before adding scene states.");
            return Optional.empty();
        }

        Map<String, Double> existingValues = new LinkedHashMap<>();
        for (SceneDeviceState existingState : existingStates) {
            existingValues.put(existingState.getDeviceId(), existingState.getTargetValue());
        }

        TextField nameField = new TextField(existingName == null ? "" : existingName);
        nameField.setPromptText("e.g. Movie Night");
        nameField.setStyle("-fx-background-radius: 10; -fx-padding: 9 12 9 12;");

        Label hint = new Label("Select the devices this scene should control and set their target state.");
        hint.setWrapText(true);
        hint.setStyle("-fx-text-fill: #6e6257;");

        List<SceneDeviceEditor> editors = new ArrayList<>();
        VBox deviceList = new VBox(10);
        for (Device device : devices) {
            SceneDeviceEditor editor = new SceneDeviceEditor(device, existingValues.get(device.getId()));
            editors.add(editor);
            deviceList.getChildren().add(editor.row());
        }

        ScrollPane scrollPane = new ScrollPane(deviceList);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(340);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        Label validationLabel = new Label();
        validationLabel.setStyle("-fx-text-fill: #b04a2f;");

        VBox content = new VBox(12,
                new Label("Scene name"),
                nameField,
                hint,
                scrollPane,
                validationLabel);
        content.setPrefWidth(620);
        content.setStyle("-fx-padding: 8 0 0 0;");

        ButtonType saveButtonType = new ButtonType("Save Scene", ButtonData.OK_DONE);
        Dialog<SceneFormData> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(title);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, saveButtonType);
        dialog.getDialogPane().setStyle("-fx-background-color: #f7f5f2;");

        Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            Optional<String> validationMessage = validateSceneEditor(nameField, editors);
            if (validationMessage.isPresent()) {
                validationLabel.setText(validationMessage.get());
                event.consume();
            }
        });

        dialog.setResultConverter(buttonType -> {
            if (buttonType != saveButtonType) {
                return null;
            }
            List<SceneDeviceState> selectedStates = new ArrayList<>();
            for (SceneDeviceEditor editor : editors) {
                editor.toSceneDeviceState().ifPresent(selectedStates::add);
            }
            return new SceneFormData(nameField.getText().trim(), selectedStates);
        });

        return dialog.showAndWait();
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

    private Optional<String> validateSceneEditor(TextField nameField, List<SceneDeviceEditor> editors) {
        if (nameField.getText() == null || nameField.getText().isBlank()) {
            return Optional.of("Please enter a scene name.");
        }
        boolean selectedDevice = false;
        for (SceneDeviceEditor editor : editors) {
            if (!editor.isSelected()) {
                continue;
            }
            selectedDevice = true;
            if (editor.readTargetValue().isEmpty()) {
                return Optional.of("Please enter a numeric target value for " + editor.deviceName() + ".");
            }
        }
        if (!selectedDevice) {
            return Optional.of("Select at least one device for this scene.");
        }
        return Optional.empty();
    }

    private String summarizeSceneStates(at.jku.se.smarthome.model.Scene scene) {
        List<String> summaries = new ArrayList<>();
        for (SceneDeviceState deviceState : scene.getDeviceStates()) {
            Device device = findDeviceById(deviceState.getDeviceId());
            if (device == null) {
                continue;
            }
            summaries.add(device.getName() + ": " + formatSceneValue(device, deviceState.getTargetValue()));
            if (summaries.size() == 3) {
                break;
            }
        }
        String summary = String.join(", ", summaries);
        if (scene.getDeviceStates().size() > summaries.size()) {
            summary += ", ...";
        }
        return summary.isBlank() ? "No known devices" : summary;
    }

    private Device findDeviceById(String deviceId) {
        for (Device device : getAllDevices()) {
            if (device.getId().equals(deviceId)) {
                return device;
            }
        }
        return null;
    }

    private String formatSceneValue(Device device, double value) {
        return switch (device.getType()) {
            case SWITCH -> value >= 1 ? "On" : "Off";
            case DIMMER -> Math.round(value) + "%";
            case THERMOSTAT -> value + " C";
            case SENSOR -> String.valueOf(value);
            case BLIND -> Math.round(value) + "%";
        };
    }

    private List<Device> getAllDevices() {
        List<Device> devices = new ArrayList<>();
        for (Room room : system.getRooms()) {
            devices.addAll(room.getDevices());
        }
        return devices;
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

    private record SceneFormData(String name, List<SceneDeviceState> deviceStates) {
    }

    private final class SceneDeviceEditor {
        private final Device device;
        private final CheckBox selectedCheckBox;
        private final Control valueControl;
        private final HBox row;

        private SceneDeviceEditor(Device device, Double existingValue) {
            this.device = device;
            this.selectedCheckBox = new CheckBox();
            this.selectedCheckBox.setSelected(existingValue != null);
            this.valueControl = createValueControl(device, existingValue);

            Label deviceName = new Label(device.getName());
            deviceName.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #2b2b2b;");
            Label deviceType = new Label(formatDeviceType(device.getType()) + " - " + sceneTargetPrompt(device));
            deviceType.setWrapText(true);
            deviceType.setStyle("-fx-font-size: 12; -fx-text-fill: #6e6257;");

            VBox deviceInfo = new VBox(3, deviceName, deviceType);
            HBox.setHgrow(deviceInfo, Priority.ALWAYS);
            valueControl.setDisable(!selectedCheckBox.isSelected());
            selectedCheckBox.selectedProperty().addListener((observable, oldValue, selected) ->
                    valueControl.setDisable(!selected));

            this.row = new HBox(12, selectedCheckBox, deviceInfo, valueControl);
            this.row.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 12;");
        }

        private HBox row() {
            return row;
        }

        private boolean isSelected() {
            return selectedCheckBox.isSelected();
        }

        private String deviceName() {
            return device.getName();
        }

        private Optional<SceneDeviceState> toSceneDeviceState() {
            if (!isSelected()) {
                return Optional.empty();
            }
            return readTargetValue().map(value -> new SceneDeviceState(device.getId(), value));
        }

        private Optional<Double> readTargetValue() {
            if (valueControl instanceof Slider slider) {
                return Optional.of(slider.getValue());
            }
            if (valueControl instanceof TextField textField) {
                try {
                    return Optional.of(Double.parseDouble(textField.getText().trim()));
                } catch (NumberFormatException exception) {
                    return Optional.empty();
                }
            }
            return Optional.empty();
        }

        private Control createValueControl(Device device, Double existingValue) {
            double value = existingValue == null ? Double.parseDouble(defaultSceneTargetValue(device)) : existingValue;
            if (device.getType() == DeviceType.SWITCH) {
                Slider slider = createSlider(0, 1, value);
                slider.setMajorTickUnit(1);
                slider.setMinorTickCount(0);
                return slider;
            }
            if (device.getType() == DeviceType.DIMMER || device.getType() == DeviceType.BLIND) {
                return createSlider(0, 100, value);
            }
            TextField textField = new TextField(String.valueOf(value));
            textField.setPrefWidth(120);
            textField.setStyle("-fx-background-radius: 10; -fx-padding: 8 10 8 10;");
            return textField;
        }

        private Slider createSlider(double min, double max, double value) {
            Slider slider = new Slider(min, max, value);
            slider.setPrefWidth(160);
            slider.setShowTickLabels(true);
            slider.setShowTickMarks(true);
            slider.setMajorTickUnit(max == 1 ? 1 : 50);
            slider.setBlockIncrement(max == 1 ? 1 : 5);
            slider.setSnapToTicks(max == 1);
            return slider;
        }
    }
}
