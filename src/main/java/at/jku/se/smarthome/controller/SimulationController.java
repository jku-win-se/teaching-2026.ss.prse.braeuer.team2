package at.jku.se.smarthome.controller;

import at.jku.se.smarthome.model.DaySimulationEvent;
import at.jku.se.smarthome.model.DaySimulationRequest;
import at.jku.se.smarthome.model.DaySimulationResult;
import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.model.DeviceType;
import at.jku.se.smarthome.model.Rule;
import at.jku.se.smarthome.model.SmartHomeSystem;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("PMD")
public class SimulationController {
    private static final DateTimeFormatter EVENT_TIME_FORMATTER = DateTimeFormatter.ofPattern("EEE HH:mm",
            Locale.ENGLISH);
    private static final Duration PLAYBACK_STEP_DURATION = Duration.millis(350);

    private final SmartHomeSystem system = SmartHomeSystem.createPersistentSystem();
    private final Map<Device, TextField> sensorValueFields = new LinkedHashMap<>();
    private final Map<Rule, CheckBox> ruleCheckBoxes = new LinkedHashMap<>();
    private Timeline playbackTimeline;
    private DaySimulationResult currentResult;
    private int playbackIndex;

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private Spinner<Integer> startHourSpinner;

    @FXML
    private Spinner<Integer> startMinuteSpinner;

    @FXML
    private VBox sensorInputContainer;

    @FXML
    private VBox activeRuleContainer;

    @FXML
    private VBox playbackContainer;

    @FXML
    private Label summaryLabel;

    public void initialize() {
        if (!system.isUserLoggedIn()) {
            Platform.runLater(this::openAuthView);
            return;
        }
        configureStartControls();
        refreshSensorInputs();
        refreshRuleInputs();
        resetPlayback("Configure the simulation and start playback.");
    }

    @FXML
    public void openDashboard() {
        stopPlayback();
        navigateTo("/at/jku/se/smarthome/fxml/main-view.fxml", 1000, 600, "Failed to open dashboard view");
    }

    @FXML
    public void openRules() {
        stopPlayback();
        navigateTo("/at/jku/se/smarthome/fxml/rules-view.fxml", 1000, 600, "Failed to open rules view");
    }

    @FXML
    public void openSchedules() {
        stopPlayback();
        navigateTo("/at/jku/se/smarthome/fxml/schedules-view.fxml", 1000, 600, "Failed to open schedules view");
    }

    @FXML
    public void openScenes() {
        stopPlayback();
        navigateTo("/at/jku/se/smarthome/fxml/scenes-view.fxml", 1000, 600, "Failed to open scenes view");
    }

    @FXML
    public void openActivity() {
        stopPlayback();
        navigateTo("/at/jku/se/smarthome/fxml/activity-view.fxml", 1000, 600, "Failed to open activity view");
    }

    @FXML
    public void openEnergy() {
        stopPlayback();
        navigateTo("/at/jku/se/smarthome/fxml/energy-view.fxml", 1000, 600, "Failed to open energy view");
    }

    @FXML
    public void logout() {
        stopPlayback();
        system.logoutUser();
        openAuthView();
    }

    @FXML
    public void startSimulation() {
        try {
            DaySimulationRequest request = new DaySimulationRequest(
                    readStartDateTime(),
                    readInitialSensorValues(),
                    readActiveRuleIds()
            );
            currentResult = system.simulateDay(request);
            playbackIndex = 0;
            resetPlayback(buildSummaryText(currentResult));
            startPlayback();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showMessage("Simulation failed", exception.getMessage());
        }
    }

    @FXML
    public void stopSimulation() {
        stopPlayback();
    }

    private void configureStartControls() {
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        startDatePicker.setValue(now.toLocalDate());
        startHourSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, now.getHour()));
        startMinuteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, now.getMinute()));
        startHourSpinner.setEditable(true);
        startMinuteSpinner.setEditable(true);
    }

    private void refreshSensorInputs() {
        sensorInputContainer.getChildren().clear();
        sensorValueFields.clear();
        for (Device sensor : collectSensors()) {
            TextField valueField = new TextField(formatValue(sensor.getValue()));
            sensorValueFields.put(sensor, valueField);
            sensorInputContainer.getChildren().add(createInputRow(sensor.getName(), valueField));
        }
        if (sensorValueFields.isEmpty()) {
            Label emptyState = new Label("No sensors available.");
            emptyState.setStyle("-fx-text-fill: #6e6257;");
            sensorInputContainer.getChildren().add(emptyState);
        }
    }

    private void refreshRuleInputs() {
        activeRuleContainer.getChildren().clear();
        ruleCheckBoxes.clear();
        for (Rule rule : system.getRules()) {
            CheckBox checkBox = new CheckBox(rule.getName());
            checkBox.setSelected(true);
            ruleCheckBoxes.put(rule, checkBox);
            activeRuleContainer.getChildren().add(checkBox);
        }
        if (ruleCheckBoxes.isEmpty()) {
            Label emptyState = new Label("No rules configured.");
            emptyState.setStyle("-fx-text-fill: #6e6257;");
            activeRuleContainer.getChildren().add(emptyState);
        }
    }

    private HBox createInputRow(String labelText, TextField valueField) {
        Label label = new Label(labelText);
        label.setStyle("-fx-text-fill: #2b2b2b;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        valueField.setPrefWidth(120);
        return new HBox(10, label, spacer, valueField);
    }

    private LocalDateTime readStartDateTime() {
        LocalDate startDate = startDatePicker.getValue();
        if (startDate == null) {
            throw new IllegalArgumentException("Please select a start date");
        }
        return startDate.atTime(startHourSpinner.getValue(), startMinuteSpinner.getValue());
    }

    private Map<String, Double> readInitialSensorValues() {
        Map<String, Double> sensorValues = new HashMap<>();
        for (Map.Entry<Device, TextField> entry : sensorValueFields.entrySet()) {
            sensorValues.put(entry.getKey().getId(), parseSensorValue(entry.getValue().getText()));
        }
        return sensorValues;
    }

    private Set<String> readActiveRuleIds() {
        return ruleCheckBoxes.entrySet().stream()
                .filter(entry -> entry.getValue().isSelected())
                .map(entry -> entry.getKey().getId())
                .collect(Collectors.toSet());
    }

    private double parseSensorValue(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException("Please enter all sensor values");
        }
        try {
            return Double.parseDouble(rawValue.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Sensor values must be numeric", exception);
        }
    }

    private List<Device> collectSensors() {
        return system.getRooms().stream()
                .flatMap(room -> room.getDevices().stream())
                .filter(device -> device.getType() == DeviceType.SENSOR)
                .toList();
    }

    private void startPlayback() {
        stopPlayback();
        if (currentResult == null || currentResult.getEvents().isEmpty()) {
            playbackContainer.getChildren().add(createEventLabel("No simulated state changes."));
            return;
        }

        playbackTimeline = new Timeline(new KeyFrame(PLAYBACK_STEP_DURATION, event -> playNextEvent()));
        playbackTimeline.setCycleCount(Timeline.INDEFINITE);
        playbackTimeline.play();
        playNextEvent();
    }

    private void playNextEvent() {
        if (currentResult == null || playbackIndex >= currentResult.getEvents().size()) {
            stopPlayback();
            return;
        }
        playbackContainer.getChildren().add(createEventLabel(formatEvent(currentResult.getEvents().get(playbackIndex))));
        playbackIndex++;
    }

    private void stopPlayback() {
        if (playbackTimeline != null) {
            playbackTimeline.stop();
            playbackTimeline = null;
        }
    }

    private void resetPlayback(String summaryText) {
        stopPlayback();
        playbackContainer.getChildren().clear();
        summaryLabel.setText(summaryText);
    }

    private Label createEventLabel(String text) {
        Label eventLabel = new Label(text);
        eventLabel.setWrapText(true);
        eventLabel.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-padding: 10 12 10 12;"
                + " -fx-text-fill: #2b2b2b;");
        eventLabel.setMaxWidth(Double.MAX_VALUE);
        return eventLabel;
    }

    private String formatEvent(DaySimulationEvent event) {
        return EVENT_TIME_FORMATTER.format(event.getTimestamp()) + " - " + event.getDeviceName()
                + ": " + event.getPreviousState() + " -> " + event.getNewState()
                + " (" + event.getActorName() + ")";
    }

    private String buildSummaryText(DaySimulationResult result) {
        return "Simulated " + result.getStartDateTime() + " to " + result.getEndDateTime()
                + " with " + result.getEvents().size() + " state changes.";
    }

    private String formatValue(double value) {
        if (value == Math.rint(value)) {
            return String.format(Locale.ENGLISH, "%.0f", value);
        }
        return String.format(Locale.ENGLISH, "%.1f", value);
    }

    private void navigateTo(String resourcePath, double width, double height, String errorMessage) {
        try {
            FXMLLoader loader = new FXMLLoader(SimulationController.class.getResource(resourcePath));
            Scene scene = new Scene(loader.load(), width, height);
            Stage stage = (Stage) playbackContainer.getScene().getWindow();
            stage.setScene(scene);
        } catch (IOException exception) {
            throw new IllegalStateException(errorMessage, exception);
        }
    }

    private void openAuthView() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    SimulationController.class.getResource("/at/jku/se/smarthome/fxml/auth-view.fxml")
            );
            Scene scene = new Scene(loader.load(), 900, 600);
            if (playbackContainer.getScene() == null) {
                Platform.runLater(this::openAuthView);
                return;
            }
            Stage stage = (Stage) playbackContainer.getScene().getWindow();
            stage.setScene(scene);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to open login view", exception);
        }
    }

    private void showMessage(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
