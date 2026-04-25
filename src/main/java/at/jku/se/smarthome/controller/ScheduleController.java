package at.jku.se.smarthome.controller;

import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.model.DeviceType;
import at.jku.se.smarthome.model.Schedule;
import at.jku.se.smarthome.model.ScheduleActionType;
import at.jku.se.smarthome.model.SmartHomeSystem;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@SuppressWarnings("PMD")
public class ScheduleController {
    private static final String SWITCH_ON_ACTION_LABEL = "On";
    private static final String SWITCH_OFF_ACTION_LABEL = "Off";
    private static final String BLIND_OPEN_ACTION_LABEL = "Open";
    private static final String BLIND_CLOSED_ACTION_LABEL = "Closed";
    private static final String SET_VALUE_ACTION_LABEL = "Set Value";

    private final SmartHomeSystem system = SmartHomeSystem.createPersistentSystem();
    private Timeline schedulePollingTimeline;

    @FXML
    private VBox scheduleListContainer;

    public void initialize() {
        if (!system.isUserLoggedIn()) {
            Platform.runLater(this::openAuthView);
            return;
        }
        startSchedulePolling();
        refreshScheduleOverview();
    }

    @FXML
    public void openDashboard() {
        navigateTo("/at/jku/se/smarthome/fxml/main-view.fxml", 1000, 600, "Failed to open dashboard view");
    }

    @FXML
    public void openActivity() {
        navigateTo("/at/jku/se/smarthome/fxml/activity-view.fxml", 1000, 600, "Failed to open activity view");
    }

    @FXML
    public void logout() {
        stopSchedulePolling();
        system.logoutUser();
        openAuthView();
    }

    @FXML
    public void createSchedule() {
        Optional<ScheduleFormData> scheduleFormData = showScheduleDialog(null);
        if (scheduleFormData.isEmpty()) {
            return;
        }

        ScheduleFormData formData = scheduleFormData.get();
        try {
            system.createSchedule(
                    formData.name(),
                    formData.deviceId(),
                    formData.actionType(),
                    formData.targetValue(),
                    formData.executionTime(),
                    formData.recurringDays()
            );
            refreshScheduleOverview();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showMessage("Invalid schedule", exception.getMessage());
        }
    }

    private void refreshScheduleOverview() {
        scheduleListContainer.getChildren().clear();

        for (Schedule schedule : system.getSchedules()) {
            scheduleListContainer.getChildren().add(createScheduleCard(schedule));
        }

        if (system.getSchedules().isEmpty()) {
            Label emptyState = new Label("No schedules yet. Create your first recurring schedule.");
            emptyState.setStyle("-fx-text-fill: #6e6257;");
            scheduleListContainer.getChildren().add(emptyState);
        }
    }

    private VBox createScheduleCard(Schedule schedule) {
        Device device = system.findDeviceById(schedule.getDeviceId());
        String deviceName = device == null ? "Unknown device" : device.getName();

        Label scheduleName = new Label(schedule.getName());
        scheduleName.setStyle("-fx-font-size: 14; -fx-text-fill: #2b2b2b;");

        Label scheduleDetails = new Label(buildScheduleDetails(schedule, device));
        scheduleDetails.setStyle("-fx-text-fill: #8a6f5a;");
        scheduleDetails.setWrapText(true);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label deviceLabel = new Label("Device: " + deviceName);
        deviceLabel.setStyle("-fx-text-fill: #6e6257;");

        Button editButton = new Button("Edit");
        editButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #6e6257;");
        editButton.setOnAction(event -> editSchedule(schedule));

        Button deleteButton = new Button("Delete");
        deleteButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #b04a2f;");
        deleteButton.setOnAction(event -> deleteSchedule(schedule));

        HBox header = new HBox(8, scheduleName, spacer, deviceLabel, editButton, deleteButton);

        VBox scheduleCard = new VBox(8, header, scheduleDetails);
        scheduleCard.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 12 14 12 14;");
        return scheduleCard;
    }

    private void editSchedule(Schedule schedule) {
        Optional<ScheduleFormData> scheduleFormData = showScheduleDialog(schedule);
        if (scheduleFormData.isEmpty()) {
            return;
        }

        ScheduleFormData formData = scheduleFormData.get();
        try {
            system.updateSchedule(
                    schedule.getId(),
                    formData.name(),
                    formData.actionType(),
                    formData.targetValue(),
                    formData.executionTime(),
                    formData.recurringDays()
            );
            refreshScheduleOverview();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showMessage("Invalid schedule", exception.getMessage());
        }
    }

    private void deleteSchedule(Schedule schedule) {
        system.removeSchedule(schedule.getId());
        refreshScheduleOverview();
    }

    private Optional<ScheduleFormData> showScheduleDialog(Schedule existingSchedule) {
        List<Device> devices = collectDevices();
        if (devices.isEmpty()) {
            showMessage("No devices", "Create a device first before adding a schedule.");
            return Optional.empty();
        }

        Dialog<ScheduleFormData> dialog = new Dialog<>();
        dialog.setTitle(existingSchedule == null ? "Create Schedule" : "Edit Schedule");
        dialog.setHeaderText(existingSchedule == null ? "Create recurring schedule" : "Edit recurring schedule");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField nameField = new TextField(existingSchedule == null ? "" : existingSchedule.getName());
        ComboBox<DeviceOption> deviceBox = new ComboBox<>();
        deviceBox.getItems().addAll(createDeviceOptions(devices));
        deviceBox.setMaxWidth(Double.MAX_VALUE);
        if (existingSchedule != null) {
            deviceBox.getSelectionModel().select(findDeviceOption(deviceBox.getItems(), existingSchedule.getDeviceId()));
            deviceBox.setDisable(true);
        } else {
            deviceBox.getSelectionModel().selectFirst();
        }

        ComboBox<String> actionBox = new ComboBox<>();
        actionBox.setMaxWidth(Double.MAX_VALUE);

        TextField valueField = new TextField();
        Spinner<Integer> hourSpinner = new Spinner<>();
        Spinner<Integer> minuteSpinner = new Spinner<>();
        hourSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23));
        minuteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59));
        hourSpinner.setEditable(true);
        minuteSpinner.setEditable(true);

        LocalTime existingTime = existingSchedule == null ? LocalTime.of(7, 0) : existingSchedule.getExecutionTime();
        hourSpinner.getValueFactory().setValue(existingTime.getHour());
        minuteSpinner.getValueFactory().setValue(existingTime.getMinute());

        Map<DayOfWeek, CheckBox> dayCheckBoxes = createDayCheckBoxes(existingSchedule);
        updateActionControls(deviceBox, actionBox, valueField, existingSchedule);
        deviceBox.valueProperty().addListener((observable, oldValue, newValue) ->
                updateActionControls(deviceBox, actionBox, valueField, existingSchedule));
        actionBox.valueProperty().addListener((observable, oldValue, newValue) ->
                updateValueFieldState(actionBox, valueField, deviceBox.getValue()));

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.add(new Label("Name"), 0, 0);
        form.add(nameField, 1, 0);
        form.add(new Label("Device"), 0, 1);
        form.add(deviceBox, 1, 1);
        form.add(new Label("Action"), 0, 2);
        form.add(actionBox, 1, 2);
        form.add(new Label("Value"), 0, 3);
        form.add(valueField, 1, 3);
        form.add(new Label("Time"), 0, 4);
        form.add(new HBox(8, hourSpinner, new Label(":"), minuteSpinner), 1, 4);
        form.add(new Label("Days"), 0, 5);
        form.add(new HBox(8,
                dayCheckBoxes.get(DayOfWeek.MONDAY),
                dayCheckBoxes.get(DayOfWeek.TUESDAY),
                dayCheckBoxes.get(DayOfWeek.WEDNESDAY),
                dayCheckBoxes.get(DayOfWeek.THURSDAY),
                dayCheckBoxes.get(DayOfWeek.FRIDAY),
                dayCheckBoxes.get(DayOfWeek.SATURDAY),
                dayCheckBoxes.get(DayOfWeek.SUNDAY)), 1, 5);

        dialog.getDialogPane().setContent(form);
        dialog.setResultConverter(buttonType -> {
            if (buttonType != ButtonType.OK) {
                return null;
            }

            DeviceOption deviceOption = deviceBox.getValue();
            if (deviceOption == null) {
                throw new IllegalArgumentException("Please select a device");
            }

            Set<DayOfWeek> recurringDays = readRecurringDays(dayCheckBoxes);
            LocalTime executionTime = LocalTime.of(hourSpinner.getValue(), minuteSpinner.getValue());
            Double targetValue = parseTargetValue(deviceOption.deviceType(), actionBox.getValue(), valueField.getText());
            return new ScheduleFormData(
                    nameField.getText(),
                    deviceOption.deviceId(),
                    resolveActionType(actionBox.getValue()),
                    targetValue,
                    executionTime,
                    recurringDays
            );
        });

        try {
            return dialog.showAndWait();
        } catch (IllegalArgumentException exception) {
            showMessage("Invalid schedule", exception.getMessage());
            return Optional.empty();
        }
    }

    private List<Device> collectDevices() {
        return system.getRooms().stream()
                .flatMap(room -> room.getDevices().stream())
                .toList();
    }

    private List<DeviceOption> createDeviceOptions(List<Device> devices) {
        return devices.stream()
                .map(device -> new DeviceOption(device.getId(), device.getName(), device.getType()))
                .toList();
    }

    private DeviceOption findDeviceOption(List<DeviceOption> deviceOptions, String deviceId) {
        for (DeviceOption deviceOption : deviceOptions) {
            if (deviceOption.deviceId().equals(deviceId)) {
                return deviceOption;
            }
        }
        return null;
    }

    private Map<DayOfWeek, CheckBox> createDayCheckBoxes(Schedule existingSchedule) {
        Set<DayOfWeek> selectedDays = existingSchedule == null
                ? EnumSet.of(DayOfWeek.MONDAY)
                : existingSchedule.getRecurringDays();
        Map<DayOfWeek, CheckBox> dayCheckBoxes = new LinkedHashMap<>();

        for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
            CheckBox checkBox = new CheckBox(dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
            checkBox.setSelected(selectedDays.contains(dayOfWeek));
            dayCheckBoxes.put(dayOfWeek, checkBox);
        }
        return dayCheckBoxes;
    }

    private void updateActionControls(ComboBox<DeviceOption> deviceBox, ComboBox<String> actionBox, TextField valueField,
                                      Schedule existingSchedule) {
        DeviceOption selectedDevice = deviceBox.getValue();
        if (selectedDevice == null) {
            actionBox.getItems().clear();
            valueField.setDisable(true);
            valueField.clear();
            return;
        }

        actionBox.getItems().setAll(getSupportedActionLabels(selectedDevice.deviceType()));
        String preferredAction = determineInitialActionLabel(selectedDevice.deviceType(), existingSchedule);
        actionBox.getSelectionModel().select(preferredAction);
        if (existingSchedule != null && existingSchedule.getTargetValue() != null) {
            valueField.setText(formatScheduleValue(existingSchedule.getTargetValue()));
        } else if (SET_VALUE_ACTION_LABEL.equals(preferredAction)) {
            valueField.setText(defaultValueForDeviceType(selectedDevice.deviceType()));
        } else {
            valueField.clear();
        }
        updateValueFieldState(actionBox, valueField, selectedDevice);
    }

    private List<String> getSupportedActionLabels(DeviceType deviceType) {
        return switch (deviceType) {
            case SWITCH -> List.of(SWITCH_ON_ACTION_LABEL, SWITCH_OFF_ACTION_LABEL);
            case BLIND -> List.of(BLIND_OPEN_ACTION_LABEL, BLIND_CLOSED_ACTION_LABEL);
            case DIMMER, THERMOSTAT, SENSOR -> List.of(SET_VALUE_ACTION_LABEL);
        };
    }

    private String determineInitialActionLabel(DeviceType deviceType, Schedule existingSchedule) {
        if (existingSchedule != null) {
            if (existingSchedule.getActionType() == ScheduleActionType.TOGGLE) {
                return SWITCH_ON_ACTION_LABEL;
            }
            if (existingSchedule.getTargetValue() != null) {
                if (deviceType == DeviceType.SWITCH) {
                    return existingSchedule.getTargetValue() == 1.0 ? SWITCH_ON_ACTION_LABEL : SWITCH_OFF_ACTION_LABEL;
                }
                if (deviceType == DeviceType.BLIND) {
                    return existingSchedule.getTargetValue() == 100.0
                            ? BLIND_OPEN_ACTION_LABEL
                            : BLIND_CLOSED_ACTION_LABEL;
                }
            }
        }
        return switch (deviceType) {
            case SWITCH -> SWITCH_ON_ACTION_LABEL;
            case BLIND -> BLIND_OPEN_ACTION_LABEL;
            case DIMMER, THERMOSTAT, SENSOR -> SET_VALUE_ACTION_LABEL;
        };
    }

    private void updateValueFieldState(ComboBox<String> actionBox, TextField valueField, DeviceOption deviceOption) {
        boolean needsValue = deviceOption != null && needsNumericValue(deviceOption.deviceType(), actionBox.getValue());
        valueField.setDisable(!needsValue);
        if (needsValue && valueField.getText().isBlank()) {
            valueField.setText(defaultValueForDeviceType(deviceOption.deviceType()));
        }
        if (!needsValue) {
            valueField.clear();
        }
    }

    private Set<DayOfWeek> readRecurringDays(Map<DayOfWeek, CheckBox> dayCheckBoxes) {
        EnumSet<DayOfWeek> recurringDays = EnumSet.noneOf(DayOfWeek.class);
        for (Map.Entry<DayOfWeek, CheckBox> entry : dayCheckBoxes.entrySet()) {
            if (entry.getValue().isSelected()) {
                recurringDays.add(entry.getKey());
            }
        }
        if (recurringDays.isEmpty()) {
            throw new IllegalArgumentException("Please select at least one weekday");
        }
        return recurringDays;
    }

    private Double parseTargetValue(DeviceType deviceType, String actionLabel, String rawValue) {
        return switch (deviceType) {
            case SWITCH -> parseSwitchTargetValue(actionLabel);
            case BLIND -> parseBlindTargetValue(actionLabel);
            case DIMMER, THERMOSTAT, SENSOR -> parseNumericTargetValue(rawValue);
        };
    }

    private ScheduleActionType resolveActionType(String actionLabel) {
        return switch (actionLabel) {
            case SWITCH_ON_ACTION_LABEL, SWITCH_OFF_ACTION_LABEL, BLIND_OPEN_ACTION_LABEL,
                    BLIND_CLOSED_ACTION_LABEL, SET_VALUE_ACTION_LABEL -> ScheduleActionType.SET_VALUE;
            default -> throw new IllegalArgumentException("Unknown action type");
        };
    }

    private String defaultValueForDeviceType(DeviceType deviceType) {
        return switch (deviceType) {
            case DIMMER -> "50";
            case THERMOSTAT -> "21";
            case BLIND -> "100";
            case SENSOR -> "1";
            case SWITCH -> "";
        };
    }

    private String buildScheduleDetails(Schedule schedule, Device device) {
        String dayText = schedule.getRecurringDays().stream()
                .sorted()
                .map(day -> day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                .reduce((left, right) -> left + ", " + right)
                .orElse("-");

        return String.format(
                Locale.ENGLISH,
                "%s at %s • %s",
                dayText,
                schedule.getExecutionTime(),
                formatScheduleAction(schedule, device)
        );
    }

    private String formatScheduleAction(Schedule schedule, Device device) {
        DeviceType deviceType = device == null ? DeviceType.SENSOR : device.getType();
        return switch (deviceType) {
            case SWITCH -> schedule.getTargetValue() != null && schedule.getTargetValue() == 1.0 ? "Turn switch on"
                    : "Turn switch off";
            case DIMMER -> "Set to " + formatScheduleValue(schedule.getTargetValue()) + " %";
            case THERMOSTAT -> "Set to " + formatScheduleValue(schedule.getTargetValue()) + " °C";
            case BLIND -> schedule.getTargetValue() != null && schedule.getTargetValue() == 100.0
                    ? "Open blind"
                    : "Close blind";
            case SENSOR -> "Set sensor to " + formatScheduleValue(schedule.getTargetValue());
        };
    }

    private boolean needsNumericValue(DeviceType deviceType, String actionLabel) {
        return switch (deviceType) {
            case DIMMER, THERMOSTAT, SENSOR -> SET_VALUE_ACTION_LABEL.equals(actionLabel);
            case SWITCH, BLIND -> false;
        };
    }

    private Double parseSwitchTargetValue(String actionLabel) {
        return switch (actionLabel) {
            case SWITCH_ON_ACTION_LABEL -> 1.0;
            case SWITCH_OFF_ACTION_LABEL -> 0.0;
            default -> throw new IllegalArgumentException("Please choose On or Off");
        };
    }

    private Double parseBlindTargetValue(String actionLabel) {
        return switch (actionLabel) {
            case BLIND_OPEN_ACTION_LABEL -> 100.0;
            case BLIND_CLOSED_ACTION_LABEL -> 0.0;
            default -> throw new IllegalArgumentException("Please choose Open or Closed");
        };
    }

    private Double parseNumericTargetValue(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException("Please enter a target value");
        }
        try {
            return Double.parseDouble(rawValue.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Please enter a numeric target value", exception);
        }
    }

    private String formatScheduleValue(Double value) {
        if (value == null) {
            return "";
        }
        if (value == Math.rint(value)) {
            return String.format(Locale.ENGLISH, "%.0f", value);
        }
        return String.format(Locale.ENGLISH, "%.1f", value);
    }

    private void startSchedulePolling() {
        stopSchedulePolling();
        schedulePollingTimeline = new Timeline(new KeyFrame(Duration.seconds(15), event -> executeDueSchedules()));
        schedulePollingTimeline.setCycleCount(Timeline.INDEFINITE);
        schedulePollingTimeline.play();
        executeDueSchedules();
    }

    private void stopSchedulePolling() {
        if (schedulePollingTimeline != null) {
            schedulePollingTimeline.stop();
            schedulePollingTimeline = null;
        }
    }

    private void executeDueSchedules() {
        int executedSchedules = system.executeDueSchedules();
        if (executedSchedules > 0) {
            refreshScheduleOverview();
        }
    }

    private void showMessage(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void navigateTo(String resourcePath, double width, double height, String errorMessage) {
        try {
            stopSchedulePolling();
            FXMLLoader loader = new FXMLLoader(ScheduleController.class.getResource(resourcePath));
            Scene scene = new Scene(loader.load(), width, height);
            Stage stage = (Stage) scheduleListContainer.getScene().getWindow();
            stage.setScene(scene);
        } catch (IOException exception) {
            throw new IllegalStateException(errorMessage, exception);
        }
    }

    private void openAuthView() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    ScheduleController.class.getResource("/at/jku/se/smarthome/fxml/auth-view.fxml")
            );
            Scene scene = new Scene(loader.load(), 900, 600);
            if (scheduleListContainer.getScene() == null) {
                Platform.runLater(this::openAuthView);
                return;
            }
            Stage stage = (Stage) scheduleListContainer.getScene().getWindow();
            stage.setScene(scene);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to open login view", exception);
        }
    }

    private record DeviceOption(String deviceId, String deviceName, DeviceType deviceType) {
        @Override
        public String toString() {
            return deviceName + " (" + formatDeviceTypeLabel(deviceType) + ")";
        }
    }

    private record ScheduleFormData(String name, String deviceId, ScheduleActionType actionType, Double targetValue,
                                    LocalTime executionTime, Set<DayOfWeek> recurringDays) {
    }

    private static String formatDeviceTypeLabel(DeviceType deviceType) {
        return switch (deviceType) {
            case SWITCH -> "Switch";
            case DIMMER -> "Dimmer";
            case THERMOSTAT -> "Thermostat";
            case SENSOR -> "Sensor";
            case BLIND -> "Jalousie";
        };
    }
}
