package at.jku.se.smarthome.controller;

import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.model.DeviceType;
import at.jku.se.smarthome.model.Room;
import at.jku.se.smarthome.model.Schedule;
import at.jku.se.smarthome.model.ScheduleActionType;
import at.jku.se.smarthome.model.SmartHomeSystem;
import at.jku.se.smarthome.model.VacationMode;
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
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@SuppressWarnings("PMD")
public class VacationModeController {
    private static final String SWITCH_ON_ACTION_LABEL = "On";
    private static final String SWITCH_OFF_ACTION_LABEL = "Off";
    private static final String BLIND_OPEN_ACTION_LABEL = "Open";
    private static final String BLIND_CLOSED_ACTION_LABEL = "Closed";
    private static final String SET_VALUE_ACTION_LABEL = "Set Value";
    private static final DateTimeFormatter STATUS_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH);

    private final SmartHomeSystem system = SmartHomeSystem.createPersistentSystem();
    private Timeline schedulePollingTimeline;
    private final Set<String> selectedScheduleIds = new LinkedHashSet<>();

    @FXML
    private Label statusLabel;

    @FXML
    private Label selectedScheduleLabel;

    @FXML
    private Label periodLabel;

    @FXML
    private DatePicker vacationStartDatePicker;

    @FXML
    private DatePicker vacationEndDatePicker;

    @FXML
    private Spinner<Integer> vacationStartHourSpinner;

    @FXML
    private Spinner<Integer> vacationStartMinuteSpinner;

    @FXML
    private Spinner<Integer> vacationEndHourSpinner;

    @FXML
    private Spinner<Integer> vacationEndMinuteSpinner;

    @FXML
    private Button activateButton;

    @FXML
    private Button deactivateButton;

    @FXML
    private VBox roomScheduleContainer;

    @FXML
    private VBox interruptedScheduleContainer;

    @FXML
    private VBox selectedScheduleContainer;

    public void initialize() {
        if (!system.isUserLoggedIn()) {
            Platform.runLater(this::openAuthView);
            return;
        }
        configureTimeControls();
        restoreConfiguredVacationMode();
        startSchedulePolling();
        refreshVacationModeOverview();
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
    public void openScenes() {
        navigateTo("/at/jku/se/smarthome/fxml/scenes-view.fxml", "Failed to open scenes view");
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
        stopSchedulePolling();
        system.logoutUser();
        openAuthView();
    }

    @FXML
    public void activateVacationMode() {
        if (selectedScheduleIds.isEmpty()) {
            showMessage("Vacation mode", "Please add at least one vacation schedule.");
            return;
        }

        try {
            LocalDateTime startAt = readVacationStartDateTime();
            LocalDateTime endAt = readVacationEndDateTime();
            system.activateVacationMode(selectedScheduleIds, startAt, endAt);
            refreshVacationModeOverview();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showMessage("Vacation mode", exception.getMessage());
        }
    }

    @FXML
    public void deactivateVacationMode() {
        try {
            system.deactivateVacationMode();
            selectedScheduleIds.clear();
            refreshVacationModeOverview();
        } catch (IllegalStateException exception) {
            showMessage("Vacation mode", exception.getMessage());
        }
    }

    private void configureTimeControls() {
        vacationStartHourSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 0));
        vacationStartMinuteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));
        vacationEndHourSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 23));
        vacationEndMinuteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 59));
        vacationStartHourSpinner.setEditable(true);
        vacationStartMinuteSpinner.setEditable(true);
        vacationEndHourSpinner.setEditable(true);
        vacationEndMinuteSpinner.setEditable(true);
        vacationStartDatePicker.setValue(LocalDate.now());
        vacationEndDatePicker.setValue(LocalDate.now().plusDays(7));
        addVacationPeriodListeners();
    }

    private void addVacationPeriodListeners() {
        vacationStartDatePicker.valueProperty().addListener((observable, oldValue, newValue) -> updateInterruptedSchedules());
        vacationEndDatePicker.valueProperty().addListener((observable, oldValue, newValue) -> updateInterruptedSchedules());
        vacationStartHourSpinner.valueProperty().addListener((observable, oldValue, newValue) -> updateInterruptedSchedules());
        vacationStartMinuteSpinner.valueProperty().addListener((observable, oldValue, newValue) -> updateInterruptedSchedules());
        vacationEndHourSpinner.valueProperty().addListener((observable, oldValue, newValue) -> updateInterruptedSchedules());
        vacationEndMinuteSpinner.valueProperty().addListener((observable, oldValue, newValue) -> updateInterruptedSchedules());
    }

    private void restoreConfiguredVacationMode() {
        VacationMode vacationMode = system.getVacationMode();
        if (vacationMode == null) {
            return;
        }
        selectedScheduleIds.clear();
        selectedScheduleIds.addAll(vacationMode.getScheduleIds());
        vacationStartDatePicker.setValue(vacationMode.getStartAt().toLocalDate());
        vacationStartHourSpinner.getValueFactory().setValue(vacationMode.getStartAt().getHour());
        vacationStartMinuteSpinner.getValueFactory().setValue(vacationMode.getStartAt().getMinute());
        vacationEndDatePicker.setValue(vacationMode.getEndAt().toLocalDate());
        vacationEndHourSpinner.getValueFactory().setValue(vacationMode.getEndAt().getHour());
        vacationEndMinuteSpinner.getValueFactory().setValue(vacationMode.getEndAt().getMinute());
    }

    private void refreshVacationModeOverview() {
        VacationMode vacationMode = system.getVacationMode();
        if (vacationMode != null) {
            selectedScheduleIds.clear();
            selectedScheduleIds.addAll(vacationMode.getScheduleIds());
        }
        renderRoomDeviceScheduleChoices();
        updateVacationStatusText(vacationMode);
        updateInterruptedSchedules();
        boolean owner = system.isCurrentUserOwner();
        activateButton.setDisable(!owner || selectedScheduleIds.isEmpty());
        deactivateButton.setDisable(!owner || vacationMode == null);
    }

    private void renderRoomDeviceScheduleChoices() {
        roomScheduleContainer.getChildren().clear();
        List<Room> rooms = system.getRooms();
        if (rooms.isEmpty()) {
            roomScheduleContainer.getChildren().add(createMutedLabel("No rooms available."));
            return;
        }

        boolean renderedAnySchedule = false;
        for (Room room : rooms) {
            VBox roomCard = new VBox(8);
            roomCard.setStyle("-fx-background-color: #f7f5f2; -fx-background-radius: 10; -fx-padding: 12;");
            Label roomName = new Label(room.getName());
            roomName.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #2b2b2b;");
            roomCard.getChildren().add(roomName);

            if (room.getDevices().isEmpty()) {
                roomCard.getChildren().add(createMutedLabel("No devices in this room."));
            }

            for (Device device : room.getDevices()) {
                List<Schedule> deviceSchedules = schedulesForDevice(device.getId());
                roomCard.getChildren().add(createDeviceScheduleSection(device, deviceSchedules));
                renderedAnySchedule = true;
            }
            roomScheduleContainer.getChildren().add(roomCard);
        }

        if (!renderedAnySchedule) {
            roomScheduleContainer.getChildren().add(createMutedLabel("Create a device before configuring vacation mode."));
        }
    }

    private VBox createDeviceScheduleSection(Device device, List<Schedule> deviceSchedules) {
        VBox deviceSection = new VBox(6);
        Label deviceName = new Label(device.getName() + " (" + formatDeviceTypeLabel(device.getType()) + ")");
        deviceName.setStyle("-fx-text-fill: #6e6257; -fx-font-weight: bold;");
        Button addScheduleButton = new Button("Add Vacation Schedule");
        addScheduleButton.setOnAction(event -> createVacationScheduleForDevice(device));
        addScheduleButton.setDisable(!system.isCurrentUserOwner());
        addScheduleButton.setStyle("-fx-background-color: #e3ded7; -fx-text-fill: #2b2b2b; -fx-background-radius: 8;");
        HBox deviceHeader = new HBox(10, deviceName, addScheduleButton);
        deviceSection.getChildren().add(deviceHeader);

        if (deviceSchedules.isEmpty()) {
            deviceSection.getChildren().add(createMutedLabel("No vacation schedule configured for this device."));
            return deviceSection;
        }

        for (Schedule schedule : deviceSchedules) {
            deviceSection.getChildren().add(createScheduleChoiceRow(schedule, device));
        }
        return deviceSection;
    }

    private HBox createScheduleChoiceRow(Schedule schedule, Device device) {
        boolean selected = selectedScheduleIds.contains(schedule.getId());
        Label scheduleName = new Label(schedule.getName());
        scheduleName.setStyle("-fx-font-weight: bold; -fx-text-fill: #2b2b2b;");
        Label scheduleDetails = new Label(buildScheduleDetails(schedule, device));
        scheduleDetails.setWrapText(true);
        scheduleDetails.setStyle("-fx-text-fill: #6e6257;");

        VBox scheduleText = new VBox(2, scheduleName, scheduleDetails);
        HBox.setHgrow(scheduleText, Priority.ALWAYS);

        Button useButton = new Button(selected ? "Remove" : "Add");
        configureScheduleActionButton(useButton);
        useButton.setDisable(!system.isCurrentUserOwner());
        useButton.setOnAction(event -> {
            toggleVacationSchedule(schedule.getId());
            refreshVacationModeOverview();
        });
        useButton.setStyle(selected
                ? "-fx-background-color: #b04a2f; -fx-text-fill: white; -fx-background-radius: 8;"
                : "-fx-background-color: #e3ded7; -fx-text-fill: #2b2b2b; -fx-background-radius: 8;");
        Button deleteButton = new Button("Delete");
        configureScheduleActionButton(deleteButton);
        deleteButton.setDisable(!system.isCurrentUserOwner());
        deleteButton.setOnAction(event -> deleteVacationSchedule(schedule));
        deleteButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #b04a2f;");
        HBox actionButtons = new HBox(6, useButton, deleteButton);
        actionButtons.setMinWidth(150);
        actionButtons.setPrefWidth(150);
        actionButtons.setMaxWidth(150);

        HBox row = new HBox(10, scheduleText, actionButtons);
        row.setStyle(selected
                ? "-fx-background-color: #fff0e6; -fx-background-radius: 10; -fx-padding: 10;"
                : "-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 10;");
        return row;
    }

    private void configureScheduleActionButton(Button button) {
        button.setMinWidth(72);
        button.setPrefWidth(72);
        button.setMaxWidth(72);
    }

    private void deleteVacationSchedule(Schedule schedule) {
        selectedScheduleIds.remove(schedule.getId());
        try {
            system.removeSchedule(schedule.getId());
            refreshVacationModeOverview();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showMessage("Vacation schedule", exception.getMessage());
        }
    }

    private void toggleVacationSchedule(String scheduleId) {
        if (selectedScheduleIds.contains(scheduleId)) {
            selectedScheduleIds.remove(scheduleId);
            return;
        }
        selectedScheduleIds.add(scheduleId);
    }

    private void createVacationScheduleForDevice(Device device) {
        try {
            Optional<VacationScheduleFormData> formData = showVacationScheduleDialog(device);
            if (formData.isEmpty()) {
                return;
            }

            VacationScheduleFormData data = formData.get();
            Schedule schedule = system.createVacationSchedule(
                    data.name(),
                    device.getId(),
                    data.actionType(),
                    data.targetValue(),
                    data.executionTime(),
                    data.recurringDays()
            );
            selectedScheduleIds.add(schedule.getId());
            refreshVacationModeOverview();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showMessage("Vacation schedule", exception.getMessage());
        }
    }

    private Optional<VacationScheduleFormData> showVacationScheduleDialog(Device device) {
        Dialog<VacationScheduleFormData> dialog = new Dialog<>();
        dialog.setTitle("Vacation Schedule");
        dialog.setHeaderText("Create vacation schedule for " + device.getName());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField nameField = new TextField("Vacation " + device.getName());
        ComboBox<String> actionBox = new ComboBox<>();
        actionBox.getItems().addAll(actionLabelsForDevice(device.getType()));
        actionBox.getSelectionModel().selectFirst();

        TextField valueField = new TextField(defaultValueForDeviceType(device.getType()));
        Spinner<Integer> hourSpinner = new Spinner<>();
        Spinner<Integer> minuteSpinner = new Spinner<>();
        hourSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 19));
        minuteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));
        hourSpinner.setEditable(true);
        minuteSpinner.setEditable(true);

        Map<DayOfWeek, CheckBox> dayCheckBoxes = createDayCheckBoxes();
        actionBox.valueProperty().addListener((observable, oldValue, newValue) ->
                updateValueFieldState(actionBox, valueField, device.getType()));
        updateValueFieldState(actionBox, valueField, device.getType());

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.add(new Label("Name"), 0, 0);
        form.add(nameField, 1, 0);
        form.add(new Label("Action"), 0, 1);
        form.add(actionBox, 1, 1);
        form.add(new Label("Value"), 0, 2);
        form.add(valueField, 1, 2);
        form.add(new Label("Time"), 0, 3);
        form.add(new HBox(8, hourSpinner, new Label(":"), minuteSpinner), 1, 3);
        form.add(new Label("Days"), 0, 4);
        form.add(new HBox(8,
                dayCheckBoxes.get(DayOfWeek.MONDAY),
                dayCheckBoxes.get(DayOfWeek.TUESDAY),
                dayCheckBoxes.get(DayOfWeek.WEDNESDAY),
                dayCheckBoxes.get(DayOfWeek.THURSDAY),
                dayCheckBoxes.get(DayOfWeek.FRIDAY),
                dayCheckBoxes.get(DayOfWeek.SATURDAY),
                dayCheckBoxes.get(DayOfWeek.SUNDAY)), 1, 4);

        dialog.getDialogPane().setContent(form);
        dialog.setResultConverter(buttonType -> {
            if (buttonType != ButtonType.OK) {
                return null;
            }
            String actionLabel = actionBox.getValue();
            return new VacationScheduleFormData(
                    nameField.getText(),
                    resolveActionType(actionLabel),
                    parseTargetValue(device.getType(), actionLabel, valueField.getText()),
                    LocalTime.of(hourSpinner.getValue(), minuteSpinner.getValue()),
                    collectRecurringDays(dayCheckBoxes)
            );
        });

        return dialog.showAndWait();
    }

    private void updateVacationStatusText(VacationMode vacationMode) {
        if (vacationMode == null) {
            statusLabel.setText("Inactive");
            periodLabel.setText("Normal schedules are running.");
        } else {
            statusLabel.setText(system.isVacationModeActive() ? "Active" : "Scheduled");
            periodLabel.setText(String.format(
                    Locale.ENGLISH,
                    "%s to %s",
                    formatStatusDateTime(vacationMode.getStartAt()),
                    formatStatusDateTime(vacationMode.getEndAt())
            ));
        }

        List<Schedule> selectedSchedules = selectedVacationSchedules();
        if (selectedSchedules.isEmpty()) {
            selectedScheduleLabel.setText("No vacation schedules configured");
            selectedScheduleContainer.getChildren().setAll(createMutedLabel("Add vacation schedules per device below."));
            return;
        }

        selectedScheduleLabel.setText("Vacation schedules: " + selectedSchedules.size());
        selectedScheduleContainer.getChildren().clear();
        for (Schedule schedule : selectedSchedules) {
            selectedScheduleContainer.getChildren().add(createSelectedScheduleSummary(schedule));
        }
    }

    private HBox createSelectedScheduleSummary(Schedule schedule) {
        Device device = system.findDeviceById(schedule.getDeviceId());
        Label scheduleName = new Label(schedule.getName());
        scheduleName.setStyle("-fx-font-weight: bold; -fx-text-fill: #2b2b2b;");
        Label scheduleDetails = new Label(buildCompactScheduleDetails(schedule, device));
        scheduleDetails.setWrapText(true);
        scheduleDetails.setStyle("-fx-text-fill: #6e6257;");
        VBox text = new VBox(1, scheduleName, scheduleDetails);
        HBox.setHgrow(text, Priority.ALWAYS);

        HBox row = new HBox(8, text);
        row.setStyle("-fx-background-color: #f7f5f2; -fx-background-radius: 8; -fx-padding: 8 10 8 10;");
        return row;
    }

    private void updateInterruptedSchedules() {
        interruptedScheduleContainer.getChildren().clear();
        if (selectedScheduleIds.isEmpty()) {
            interruptedScheduleContainer.getChildren().add(createMutedLabel("Add vacation schedules to preview interruptions."));
            return;
        }

        Optional<Period> selectedPeriod = readPreviewPeriod();
        if (selectedPeriod.isEmpty()) {
            interruptedScheduleContainer.getChildren().add(createMutedLabel("Set a valid start and end period."));
            return;
        }

        List<Schedule> interruptedSchedules = new ArrayList<>();
        for (Schedule schedule : system.getSchedules()) {
            if (selectedScheduleIds.contains(schedule.getId())) {
                continue;
            }
            if (isInterruptedByVacationSchedule(schedule, selectedPeriod.get())) {
                interruptedSchedules.add(schedule);
            }
        }

        if (interruptedSchedules.isEmpty()) {
            interruptedScheduleContainer.getChildren().add(createMutedLabel("No schedules for the same device and time are interrupted."));
            return;
        }

        for (Schedule schedule : interruptedSchedules) {
            interruptedScheduleContainer.getChildren().add(createInterruptedScheduleRow(schedule));
        }
    }

    private boolean isInterruptedByVacationSchedule(Schedule normalSchedule, Period period) {
        for (Schedule vacationSchedule : selectedVacationSchedules()) {
            if (!normalSchedule.getDeviceId().equals(vacationSchedule.getDeviceId())) {
                continue;
            }
            if (!normalSchedule.getExecutionTime().equals(vacationSchedule.getExecutionTime())) {
                continue;
            }
            if (hasOverlappingOccurrence(normalSchedule, vacationSchedule, period)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasOverlappingOccurrence(Schedule normalSchedule, Schedule vacationSchedule, Period period) {
        LocalDate date = period.startAt().toLocalDate();
        LocalDate endDate = period.endAt().toLocalDate();
        while (!date.isAfter(endDate)) {
            LocalDateTime occurrence = LocalDateTime.of(date, normalSchedule.getExecutionTime());
            boolean sameDay = normalSchedule.getRecurringDays().contains(date.getDayOfWeek())
                    && vacationSchedule.getRecurringDays().contains(date.getDayOfWeek());
            if (sameDay && !occurrence.isBefore(period.startAt()) && occurrence.isBefore(period.endAt())) {
                return true;
            }
            date = date.plusDays(1);
        }
        return false;
    }

    private List<String> actionLabelsForDevice(DeviceType deviceType) {
        return switch (deviceType) {
            case SWITCH -> List.of(SWITCH_ON_ACTION_LABEL, SWITCH_OFF_ACTION_LABEL);
            case BLIND -> List.of(BLIND_OPEN_ACTION_LABEL, BLIND_CLOSED_ACTION_LABEL);
            case DIMMER, THERMOSTAT, SENSOR -> List.of(SET_VALUE_ACTION_LABEL);
        };
    }

    private void updateValueFieldState(ComboBox<String> actionBox, TextField valueField, DeviceType deviceType) {
        boolean needsValue = needsNumericValue(deviceType, actionBox.getValue());
        valueField.setDisable(!needsValue);
        valueField.setVisible(needsValue);
        valueField.setManaged(needsValue);
    }

    private Map<DayOfWeek, CheckBox> createDayCheckBoxes() {
        Map<DayOfWeek, CheckBox> dayCheckBoxes = new java.util.LinkedHashMap<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            CheckBox checkBox = new CheckBox(day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
            checkBox.setSelected(true);
            dayCheckBoxes.put(day, checkBox);
        }
        return dayCheckBoxes;
    }

    private Set<DayOfWeek> collectRecurringDays(Map<DayOfWeek, CheckBox> dayCheckBoxes) {
        Set<DayOfWeek> recurringDays = EnumSet.noneOf(DayOfWeek.class);
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
            case THERMOSTAT -> "18";
            case BLIND -> "100";
            case SENSOR -> "1";
            case SWITCH -> "";
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

    private VBox createInterruptedScheduleRow(Schedule schedule) {
        Device device = system.findDeviceById(schedule.getDeviceId());
        Label scheduleName = new Label(schedule.getName());
        scheduleName.setStyle("-fx-font-weight: bold; -fx-text-fill: #2b2b2b;");
        Label scheduleDetails = new Label(buildScheduleDetails(schedule, device));
        scheduleDetails.setWrapText(true);
        scheduleDetails.setStyle("-fx-text-fill: #6e6257;");
        VBox row = new VBox(3, scheduleName, scheduleDetails);
        row.setStyle("-fx-background-color: #f7f5f2; -fx-background-radius: 10; -fx-padding: 10;");
        return row;
    }

    private List<Schedule> schedulesForDevice(String deviceId) {
        List<Schedule> matchingSchedules = new ArrayList<>();
        for (Schedule schedule : system.getVacationSchedules()) {
            if (schedule.getDeviceId().equals(deviceId)) {
                matchingSchedules.add(schedule);
            }
        }
        return matchingSchedules;
    }

    private List<Schedule> selectedVacationSchedules() {
        List<Schedule> selectedSchedules = new ArrayList<>();
        for (String scheduleId : selectedScheduleIds) {
            Schedule schedule = system.findScheduleById(scheduleId);
            if (schedule != null) {
                selectedSchedules.add(schedule);
            }
        }
        return selectedSchedules;
    }

    private Optional<Period> readPreviewPeriod() {
        try {
            LocalDateTime startAt = readVacationStartDateTime();
            LocalDateTime endAt = readVacationEndDateTime();
            if (!endAt.isAfter(startAt)) {
                return Optional.empty();
            }
            return Optional.of(new Period(startAt, endAt));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private String buildScheduleDetails(Schedule schedule, Device device) {
        String roomName = findRoomNameForDevice(schedule.getDeviceId());
        String deviceName = device == null ? "Unknown device" : device.getName();
        String dayText = schedule.getRecurringDays().stream()
                .sorted()
                .map(day -> day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                .reduce((left, right) -> left + ", " + right)
                .orElse("-");
        return String.format(
                Locale.ENGLISH,
                "%s / %s - %s at %s - %s",
                roomName,
                deviceName,
                dayText,
                schedule.getExecutionTime(),
                formatScheduleAction(schedule, device)
        );
    }

    private String buildCompactScheduleDetails(Schedule schedule, Device device) {
        String roomName = findRoomNameForDevice(schedule.getDeviceId());
        String deviceName = device == null ? "Unknown device" : device.getName();
        return String.format(
                Locale.ENGLISH,
                "%s / %s - %s - %s",
                roomName,
                deviceName,
                schedule.getExecutionTime(),
                formatScheduleAction(schedule, device)
        );
    }

    private String findRoomNameForDevice(String deviceId) {
        for (Room room : system.getRooms()) {
            if (room.findDeviceById(deviceId) != null) {
                return room.getName();
            }
        }
        return "Unknown room";
    }

    private String formatScheduleAction(Schedule schedule, Device device) {
        DeviceType deviceType = device == null ? DeviceType.SENSOR : device.getType();
        if (schedule.getActionType() == ScheduleActionType.TOGGLE) {
            return "Toggle";
        }
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

    private String formatScheduleValue(Double value) {
        if (value == null) {
            return "";
        }
        if (value == Math.rint(value)) {
            return String.format(Locale.ENGLISH, "%.0f", value);
        }
        return String.format(Locale.ENGLISH, "%.1f", value);
    }

    private String formatStatusDateTime(LocalDateTime dateTime) {
        return dateTime.format(STATUS_DATE_TIME_FORMATTER);
    }

    private Label createMutedLabel(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setStyle("-fx-text-fill: #8a6f5a;");
        return label;
    }

    private LocalDateTime readVacationStartDateTime() {
        return readVacationDateTime(
                vacationStartDatePicker,
                vacationStartHourSpinner,
                vacationStartMinuteSpinner,
                "start"
        );
    }

    private LocalDateTime readVacationEndDateTime() {
        return readVacationDateTime(
                vacationEndDatePicker,
                vacationEndHourSpinner,
                vacationEndMinuteSpinner,
                "end"
        );
    }

    private LocalDateTime readVacationDateTime(DatePicker datePicker, Spinner<Integer> hourSpinner,
                                               Spinner<Integer> minuteSpinner, String fieldName) {
        if (datePicker.getValue() == null) {
            throw new IllegalArgumentException("Please select a vacation " + fieldName + " date");
        }
        return LocalDateTime.of(datePicker.getValue(), LocalTime.of(hourSpinner.getValue(), minuteSpinner.getValue()));
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
        system.executeDueSchedules();
        refreshVacationModeOverview();
    }

    private void navigateTo(String resourcePath, String errorMessage) {
        try {
            stopSchedulePolling();
            FXMLLoader loader = new FXMLLoader(VacationModeController.class.getResource(resourcePath));
            Scene scene = new Scene(loader.load(), 1000, 600);
            Stage stage = (Stage) statusLabel.getScene().getWindow();
            stage.setScene(scene);
        } catch (IOException exception) {
            throw new IllegalStateException(errorMessage, exception);
        }
    }

    private void openAuthView() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    VacationModeController.class.getResource("/at/jku/se/smarthome/fxml/auth-view.fxml")
            );
            Scene scene = new Scene(loader.load(), 900, 600);
            if (statusLabel.getScene() == null) {
                Platform.runLater(this::openAuthView);
                return;
            }
            Stage stage = (Stage) statusLabel.getScene().getWindow();
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

    private static String formatDeviceTypeLabel(DeviceType deviceType) {
        return switch (deviceType) {
            case SWITCH -> "Switch";
            case DIMMER -> "Dimmer";
            case THERMOSTAT -> "Thermostat";
            case SENSOR -> "Sensor";
            case BLIND -> "Jalousie";
        };
    }

    private record VacationScheduleFormData(String name, ScheduleActionType actionType, Double targetValue,
                                            LocalTime executionTime, Set<DayOfWeek> recurringDays) {
    }

    private record Period(LocalDateTime startAt, LocalDateTime endAt) {
    }
}
