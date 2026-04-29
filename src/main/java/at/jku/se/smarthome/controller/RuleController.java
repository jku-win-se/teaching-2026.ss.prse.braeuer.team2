package at.jku.se.smarthome.controller;

import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.model.DeviceType;
import at.jku.se.smarthome.model.Rule;
import at.jku.se.smarthome.model.RuleActionType;
import at.jku.se.smarthome.model.RuleTriggerType;
import at.jku.se.smarthome.model.SmartHomeSystem;
import at.jku.se.smarthome.model.ThresholdOperator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@SuppressWarnings("PMD")
public class RuleController {
    private static final String SWITCH_ON_LABEL = "On";
    private static final String SWITCH_OFF_LABEL = "Off";
    private static final String BLIND_OPEN_LABEL = "Open";
    private static final String BLIND_CLOSED_LABEL = "Closed";
    private static final String VALUE_EQUALS_LABEL = "Value equals";
    private static final String VALUE_ABOVE_LABEL = "Value above";
    private static final String VALUE_BELOW_LABEL = "Value below";
    private static final String TIME_AT_LABEL = "At time";

    private final SmartHomeSystem system = SmartHomeSystem.createPersistentSystem();

    @FXML
    private VBox ruleListContainer;

    @FXML
    private Button createRuleButton;

    public void initialize() {
        if (!system.isUserLoggedIn()) {
            Platform.runLater(this::openAuthView);
            return;
        }
        if (!system.isCurrentUserOwner()) {
            Platform.runLater(this::openDashboard);
            return;
        }
        configureRoleAccess();
        refreshRuleOverview();
    }

    @FXML
    public void openDashboard() {
        navigateTo("/at/jku/se/smarthome/fxml/main-view.fxml", 1000, 600, "Failed to open dashboard view");
    }

    @FXML
    public void openSchedules() {
        navigateTo("/at/jku/se/smarthome/fxml/schedules-view.fxml", 1000, 600, "Failed to open schedules view");
    }

    @FXML
    public void openActivity() {
        navigateTo("/at/jku/se/smarthome/fxml/activity-view.fxml", 1000, 600, "Failed to open activity view");
    }

    @FXML
    public void openEnergy() {
        navigateTo("/at/jku/se/smarthome/fxml/energy-view.fxml", 1000, 600, "Failed to open energy view");
    }

    @FXML
    public void logout() {
        system.logoutUser();
        openAuthView();
    }

    @FXML
    public void createRule() {
        if (!system.isCurrentUserOwner()) {
            showMessage("Rules unavailable", "Members can control devices, but cannot manage rules.");
            return;
        }

        Optional<RuleFormData> formData = showRuleDialog(null);
        if (formData.isEmpty()) {
            return;
        }

        RuleFormData ruleFormData = formData.get();
        try {
            createRule(ruleFormData);
            refreshRuleOverview();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showMessage("Invalid rule", exception.getMessage());
        }
    }

    private void refreshRuleOverview() {
        ruleListContainer.getChildren().clear();

        for (Rule rule : system.getRules()) {
            ruleListContainer.getChildren().add(createRuleCard(rule));
        }

        if (system.getRules().isEmpty()) {
            Label emptyState = new Label("No rules yet. Create your first automation rule.");
            emptyState.setStyle("-fx-text-fill: #6e6257;");
            ruleListContainer.getChildren().add(emptyState);
        }
    }

    private void configureRoleAccess() {
        createRuleButton.setVisible(system.isCurrentUserOwner());
        createRuleButton.setManaged(system.isCurrentUserOwner());
    }

    private VBox createRuleCard(Rule rule) {
        Device sourceDevice = system.findDeviceById(rule.getTrigger().getSourceDeviceId());
        Device targetDevice = system.findDeviceById(rule.getAction().getTargetDeviceId());

        Label ruleName = new Label(rule.getName());
        ruleName.setStyle("-fx-font-size: 14; -fx-text-fill: #2b2b2b;");

        Label ruleDetails = new Label(buildRuleDetails(rule, sourceDevice, targetDevice));
        ruleDetails.setStyle("-fx-text-fill: #8a6f5a;");
        ruleDetails.setWrapText(true);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button editButton = new Button("Edit");
        editButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #6e6257;");
        editButton.setOnAction(event -> editRule(rule));

        Button deleteButton = new Button("Delete");
        deleteButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #b04a2f;");
        deleteButton.setOnAction(event -> deleteRule(rule));

        HBox header = new HBox(8, ruleName, spacer, editButton, deleteButton);

        VBox ruleCard = new VBox(8, header, ruleDetails);
        ruleCard.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 12 14 12 14;");
        return ruleCard;
    }

    private void editRule(Rule rule) {
        Optional<RuleFormData> formData = showRuleDialog(rule);
        if (formData.isEmpty()) {
            return;
        }

        RuleFormData ruleFormData = formData.get();
        try {
            updateRule(rule, ruleFormData);
            refreshRuleOverview();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showMessage("Invalid rule", exception.getMessage());
        }
    }

    private void deleteRule(Rule rule) {
        system.removeRule(rule.getId());
        refreshRuleOverview();
    }

    private Optional<RuleFormData> showRuleDialog(Rule existingRule) {
        List<Device> devices = collectDevices();
        if (devices.isEmpty()) {
            showMessage("No devices", "Create devices first before adding a rule.");
            return Optional.empty();
        }

        Dialog<RuleFormData> dialog = new Dialog<>();
        dialog.setTitle(existingRule == null ? "Create Rule" : "Edit Rule");
        dialog.setHeaderText(existingRule == null ? "Create automation rule" : "Edit automation rule");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField nameField = new TextField(existingRule == null ? "" : existingRule.getName());

        ComboBox<RuleTriggerType> triggerTypeBox = new ComboBox<>();
        triggerTypeBox.getItems().addAll(RuleTriggerType.DEVICE_STATE_CHANGE, RuleTriggerType.THRESHOLD, RuleTriggerType.TIME);
        triggerTypeBox.setMaxWidth(Double.MAX_VALUE);
        triggerTypeBox.getSelectionModel().select(existingRule == null
                ? RuleTriggerType.DEVICE_STATE_CHANGE
                : existingRule.getTrigger().getTriggerType());

        ComboBox<DeviceOption> sourceDeviceBox = new ComboBox<>();
        sourceDeviceBox.getItems().addAll(createDeviceOptions(devices));
        sourceDeviceBox.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> triggerConditionBox = new ComboBox<>();
        triggerConditionBox.setMaxWidth(Double.MAX_VALUE);
        TextField triggerValueField = new TextField();

        ComboBox<DeviceOption> targetDeviceBox = new ComboBox<>();
        targetDeviceBox.getItems().addAll(createDeviceOptions(devices));
        targetDeviceBox.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> actionBox = new ComboBox<>();
        actionBox.setMaxWidth(Double.MAX_VALUE);
        TextField actionValueField = new TextField();

        if (existingRule != null) {
            sourceDeviceBox.getSelectionModel().select(findDeviceOption(sourceDeviceBox.getItems(),
                    existingRule.getTrigger().getSourceDeviceId()));
            targetDeviceBox.getSelectionModel().select(findDeviceOption(targetDeviceBox.getItems(),
                    existingRule.getAction().getTargetDeviceId()));
        } else {
            sourceDeviceBox.getSelectionModel().selectFirst();
            targetDeviceBox.getSelectionModel().selectFirst();
        }

        updateTriggerControls(triggerTypeBox, sourceDeviceBox, triggerConditionBox, triggerValueField, existingRule);
        updateActionControls(targetDeviceBox, actionBox, actionValueField, existingRule);

        triggerTypeBox.valueProperty().addListener((observable, oldValue, newValue) ->
                updateTriggerControls(triggerTypeBox, sourceDeviceBox, triggerConditionBox, triggerValueField, null));
        sourceDeviceBox.valueProperty().addListener((observable, oldValue, newValue) ->
                updateTriggerControls(triggerTypeBox, sourceDeviceBox, triggerConditionBox, triggerValueField, null));
        triggerConditionBox.valueProperty().addListener((observable, oldValue, newValue) ->
                updateTriggerValueFieldState(triggerTypeBox.getValue(), triggerValueField, sourceDeviceBox.getValue(), newValue));
        targetDeviceBox.valueProperty().addListener((observable, oldValue, newValue) ->
                updateActionControls(targetDeviceBox, actionBox, actionValueField, null));
        actionBox.valueProperty().addListener((observable, oldValue, newValue) ->
                updateRuleValueFieldState(actionValueField, targetDeviceBox.getValue(), newValue));

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.add(new Label("Name"), 0, 0);
        form.add(nameField, 1, 0);
        form.add(new Label("Trigger type"), 0, 1);
        form.add(triggerTypeBox, 1, 1);
        form.add(new Label("When device"), 0, 2);
        form.add(sourceDeviceBox, 1, 2);
        form.add(new Label("Trigger"), 0, 3);
        form.add(triggerConditionBox, 1, 3);
        form.add(new Label("Trigger value"), 0, 4);
        form.add(triggerValueField, 1, 4);
        form.add(new Label("Then device"), 0, 5);
        form.add(targetDeviceBox, 1, 5);
        form.add(new Label("Action"), 0, 6);
        form.add(actionBox, 1, 6);
        form.add(new Label("Action value"), 0, 7);
        form.add(actionValueField, 1, 7);

        dialog.getDialogPane().setContent(form);
        dialog.setResultConverter(buttonType -> {
            if (buttonType != ButtonType.OK) {
                return null;
            }

            DeviceOption sourceDevice = sourceDeviceBox.getValue();
            DeviceOption targetDevice = targetDeviceBox.getValue();
            if (sourceDevice == null || targetDevice == null) {
                throw new IllegalArgumentException("Please select both a source and a target device");
            }

            return new RuleFormData(
                    nameField.getText(),
                    triggerTypeBox.getValue(),
                    sourceDevice.deviceId(),
                    parseTriggerValue(triggerTypeBox.getValue(), sourceDevice.deviceType(),
                            triggerConditionBox.getValue(), triggerValueField.getText()),
                    parseThresholdOperator(triggerTypeBox.getValue(), triggerConditionBox.getValue()),
                    parseTriggerTime(triggerTypeBox.getValue(), triggerValueField.getText()),
                    targetDevice.deviceId(),
                    parseRuleValue(targetDevice.deviceType(), actionBox.getValue(), actionValueField.getText())
            );
        });

        try {
            return dialog.showAndWait();
        } catch (IllegalArgumentException exception) {
            showMessage("Invalid rule", exception.getMessage());
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

    private void updateTriggerControls(ComboBox<RuleTriggerType> triggerTypeBox, ComboBox<DeviceOption> sourceDeviceBox,
                                       ComboBox<String> triggerConditionBox,
                                       TextField triggerValueField, Rule existingRule) {
        RuleTriggerType triggerType = triggerTypeBox.getValue();
        if (triggerType == RuleTriggerType.TIME) {
            sourceDeviceBox.setDisable(true);
            triggerConditionBox.getItems().setAll(TIME_AT_LABEL);
            triggerConditionBox.getSelectionModel().select(TIME_AT_LABEL);
            triggerValueField.setDisable(false);
            triggerValueField.setText(existingRule == null || existingRule.getTrigger().getTriggerTime() == null
                    ? "07:00"
                    : existingRule.getTrigger().getTriggerTime().toString());
            return;
        }
        sourceDeviceBox.setDisable(false);
        if (triggerType == RuleTriggerType.THRESHOLD) {
            triggerConditionBox.getItems().setAll(VALUE_ABOVE_LABEL, VALUE_BELOW_LABEL);
            triggerConditionBox.getSelectionModel().select(determineInitialThresholdLabel(existingRule));
            triggerValueField.setDisable(false);
            triggerValueField.setText(existingRule == null || existingRule.getTrigger().getExpectedValue() == null
                    ? defaultValueForDeviceType(DeviceType.SENSOR)
                    : formatValue(existingRule.getTrigger().getExpectedValue()));
            return;
        }
        updateRuleControls(sourceDeviceBox, triggerConditionBox, triggerValueField, existingRule, true);
    }

    private void updateActionControls(ComboBox<DeviceOption> targetDeviceBox, ComboBox<String> actionBox,
                                      TextField actionValueField, Rule existingRule) {
        updateRuleControls(targetDeviceBox, actionBox, actionValueField, existingRule, false);
    }

    private void updateRuleControls(ComboBox<DeviceOption> deviceBox, ComboBox<String> conditionBox,
                                    TextField valueField, Rule existingRule, boolean triggerControl) {
        DeviceOption selectedDevice = deviceBox.getValue();
        if (selectedDevice == null) {
            conditionBox.getItems().clear();
            valueField.clear();
            valueField.setDisable(true);
            return;
        }

        conditionBox.getItems().setAll(getSupportedConditionLabels(selectedDevice.deviceType()));
        String selectedLabel = determineInitialConditionLabel(selectedDevice.deviceType(), existingRule, triggerControl);
        conditionBox.getSelectionModel().select(selectedLabel);

        Double existingValue = extractExistingValue(existingRule, triggerControl);
        if (existingValue != null) {
            valueField.setText(formatValue(existingValue));
        } else if (needsNumericValue(selectedDevice.deviceType(), selectedLabel)) {
            valueField.setText(defaultValueForDeviceType(selectedDevice.deviceType()));
        } else {
            valueField.clear();
        }
        updateRuleValueFieldState(valueField, selectedDevice, selectedLabel);
    }

    private List<String> getSupportedConditionLabels(DeviceType deviceType) {
        return switch (deviceType) {
            case SWITCH -> List.of(SWITCH_ON_LABEL, SWITCH_OFF_LABEL);
            case BLIND -> List.of(BLIND_OPEN_LABEL, BLIND_CLOSED_LABEL);
            case DIMMER, THERMOSTAT, SENSOR -> List.of(VALUE_EQUALS_LABEL);
        };
    }

    private String determineInitialConditionLabel(DeviceType deviceType, Rule existingRule, boolean triggerControl) {
        Double existingValue = extractExistingValue(existingRule, triggerControl);
        if (existingValue != null) {
            return mapValueToLabel(deviceType, existingValue);
        }
        return switch (deviceType) {
            case SWITCH -> SWITCH_ON_LABEL;
            case BLIND -> BLIND_OPEN_LABEL;
            case DIMMER, THERMOSTAT, SENSOR -> VALUE_EQUALS_LABEL;
        };
    }

    private Double extractExistingValue(Rule existingRule, boolean triggerControl) {
        if (existingRule == null) {
            return null;
        }
        return triggerControl ? existingRule.getTrigger().getExpectedValue() : existingRule.getAction().getTargetValue();
    }

    private String mapValueToLabel(DeviceType deviceType, double value) {
        return switch (deviceType) {
            case SWITCH -> value == 1.0 ? SWITCH_ON_LABEL : SWITCH_OFF_LABEL;
            case BLIND -> value == 100.0 ? BLIND_OPEN_LABEL : BLIND_CLOSED_LABEL;
            case DIMMER, THERMOSTAT, SENSOR -> VALUE_EQUALS_LABEL;
        };
    }

    private void updateRuleValueFieldState(TextField valueField, DeviceOption deviceOption, String label) {
        boolean needsValue = deviceOption != null && needsNumericValue(deviceOption.deviceType(), label);
        valueField.setDisable(!needsValue);
        if (needsValue && valueField.getText().isBlank()) {
            valueField.setText(defaultValueForDeviceType(deviceOption.deviceType()));
        }
        if (!needsValue) {
            valueField.clear();
        }
    }

    private boolean needsNumericValue(DeviceType deviceType, String label) {
        return switch (deviceType) {
            case DIMMER, THERMOSTAT, SENSOR -> VALUE_EQUALS_LABEL.equals(label);
            case SWITCH, BLIND -> false;
        };
    }

    private Double parseRuleValue(DeviceType deviceType, String label, String rawValue) {
        return switch (deviceType) {
            case SWITCH -> SWITCH_ON_LABEL.equals(label) ? 1.0 : 0.0;
            case BLIND -> BLIND_OPEN_LABEL.equals(label) ? 100.0 : 0.0;
            case DIMMER, THERMOSTAT, SENSOR -> parseNumericValue(rawValue);
        };
    }

    private Double parseTriggerValue(RuleTriggerType triggerType, DeviceType deviceType, String label, String rawValue) {
        if (triggerType == RuleTriggerType.TIME) {
            return null;
        }
        if (triggerType == RuleTriggerType.THRESHOLD) {
            return parseNumericValue(rawValue);
        }
        return parseRuleValue(deviceType, label, rawValue);
    }

    private ThresholdOperator parseThresholdOperator(RuleTriggerType triggerType, String label) {
        if (triggerType != RuleTriggerType.THRESHOLD) {
            return null;
        }
        return VALUE_BELOW_LABEL.equals(label) ? ThresholdOperator.BELOW : ThresholdOperator.ABOVE;
    }

    private LocalTime parseTriggerTime(RuleTriggerType triggerType, String rawValue) {
        if (triggerType != RuleTriggerType.TIME) {
            return null;
        }
        try {
            return LocalTime.parse(rawValue.trim());
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Please enter a time in HH:mm format", exception);
        }
    }

    private void updateTriggerValueFieldState(RuleTriggerType triggerType, TextField valueField,
                                              DeviceOption deviceOption, String label) {
        if (triggerType == RuleTriggerType.TIME || triggerType == RuleTriggerType.THRESHOLD) {
            valueField.setDisable(false);
            return;
        }
        updateRuleValueFieldState(valueField, deviceOption, label);
    }

    private String determineInitialThresholdLabel(Rule existingRule) {
        if (existingRule != null && existingRule.getTrigger().getThresholdOperator() == ThresholdOperator.BELOW) {
            return VALUE_BELOW_LABEL;
        }
        return VALUE_ABOVE_LABEL;
    }

    private Double parseNumericValue(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException("Please enter a numeric value");
        }
        try {
            return Double.parseDouble(rawValue.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Please enter a numeric value", exception);
        }
    }

    private String defaultValueForDeviceType(DeviceType deviceType) {
        return switch (deviceType) {
            case DIMMER -> "50";
            case THERMOSTAT -> "21";
            case SENSOR -> "1";
            case SWITCH, BLIND -> "";
        };
    }

    private String buildRuleDetails(Rule rule, Device sourceDevice, Device targetDevice) {
        String sourceName = sourceDevice == null ? "Unknown device" : sourceDevice.getName();
        String targetName = targetDevice == null ? "Unknown device" : targetDevice.getName();

        if (rule.getTrigger().getTriggerType() == RuleTriggerType.TIME) {
            return String.format(
                    Locale.ENGLISH,
                    "WHEN time is %s THEN set %s to %s",
                    rule.getTrigger().getTriggerTime(),
                    targetName,
                    formatConditionValue(targetDevice == null ? null : targetDevice.getType(), rule.getAction().getTargetValue())
            );
        }
        if (rule.getTrigger().getTriggerType() == RuleTriggerType.THRESHOLD) {
            return String.format(
                    Locale.ENGLISH,
                    "WHEN %s is %s %s THEN set %s to %s",
                    sourceName,
                    rule.getTrigger().getThresholdOperator() == ThresholdOperator.BELOW ? "below" : "above",
                    formatValue(rule.getTrigger().getExpectedValue()),
                    targetName,
                    formatConditionValue(targetDevice == null ? null : targetDevice.getType(), rule.getAction().getTargetValue())
            );
        }
        return String.format(
                Locale.ENGLISH,
                "WHEN %s is %s THEN set %s to %s",
                sourceName,
                formatConditionValue(sourceDevice == null ? null : sourceDevice.getType(), rule.getTrigger().getExpectedValue()),
                targetName,
                formatConditionValue(targetDevice == null ? null : targetDevice.getType(), rule.getAction().getTargetValue())
        );
    }

    private String formatConditionValue(DeviceType deviceType, Double value) {
        if (deviceType == null || value == null) {
            return "unknown";
        }
        return switch (deviceType) {
            case SWITCH -> value == 1.0 ? "On" : "Off";
            case BLIND -> value == 100.0 ? "Open" : "Closed";
            case DIMMER -> formatValue(value) + " %";
            case THERMOSTAT -> formatValue(value) + " °C";
            case SENSOR -> formatValue(value);
        };
    }

    private void createRule(RuleFormData ruleFormData) {
        if (ruleFormData.triggerType() == RuleTriggerType.TIME) {
            system.createTimeRule(
                    ruleFormData.name(),
                    ruleFormData.triggerTime(),
                    RuleActionType.SET_DEVICE_STATE,
                    ruleFormData.targetDeviceId(),
                    ruleFormData.targetActionValue()
            );
            return;
        }
        if (ruleFormData.triggerType() == RuleTriggerType.THRESHOLD) {
            system.createThresholdRule(
                    ruleFormData.name(),
                    ruleFormData.sourceDeviceId(),
                    ruleFormData.thresholdOperator(),
                    ruleFormData.expectedTriggerValue(),
                    RuleActionType.SET_DEVICE_STATE,
                    ruleFormData.targetDeviceId(),
                    ruleFormData.targetActionValue()
            );
            return;
        }
        system.createRule(
                ruleFormData.name(),
                RuleTriggerType.DEVICE_STATE_CHANGE,
                ruleFormData.sourceDeviceId(),
                ruleFormData.expectedTriggerValue(),
                RuleActionType.SET_DEVICE_STATE,
                ruleFormData.targetDeviceId(),
                ruleFormData.targetActionValue()
        );
    }

    private void updateRule(Rule rule, RuleFormData ruleFormData) {
        if (ruleFormData.triggerType() == RuleTriggerType.TIME) {
            system.updateTimeRule(
                    rule.getId(),
                    ruleFormData.name(),
                    ruleFormData.triggerTime(),
                    RuleActionType.SET_DEVICE_STATE,
                    ruleFormData.targetDeviceId(),
                    ruleFormData.targetActionValue()
            );
            return;
        }
        if (ruleFormData.triggerType() == RuleTriggerType.THRESHOLD) {
            system.updateThresholdRule(
                    rule.getId(),
                    ruleFormData.name(),
                    ruleFormData.sourceDeviceId(),
                    ruleFormData.thresholdOperator(),
                    ruleFormData.expectedTriggerValue(),
                    RuleActionType.SET_DEVICE_STATE,
                    ruleFormData.targetDeviceId(),
                    ruleFormData.targetActionValue()
            );
            return;
        }
        system.updateRule(
                rule.getId(),
                ruleFormData.name(),
                RuleTriggerType.DEVICE_STATE_CHANGE,
                ruleFormData.sourceDeviceId(),
                ruleFormData.expectedTriggerValue(),
                RuleActionType.SET_DEVICE_STATE,
                ruleFormData.targetDeviceId(),
                ruleFormData.targetActionValue()
        );
    }

    private String formatValue(double value) {
        if (value == Math.rint(value)) {
            return String.format(Locale.ENGLISH, "%.0f", value);
        }
        return String.format(Locale.ENGLISH, "%.1f", value);
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
            FXMLLoader loader = new FXMLLoader(RuleController.class.getResource(resourcePath));
            Scene scene = new Scene(loader.load(), width, height);
            Stage stage = (Stage) ruleListContainer.getScene().getWindow();
            stage.setScene(scene);
        } catch (IOException exception) {
            throw new IllegalStateException(errorMessage, exception);
        }
    }

    private void openAuthView() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    RuleController.class.getResource("/at/jku/se/smarthome/fxml/auth-view.fxml")
            );
            Scene scene = new Scene(loader.load(), 900, 600);
            if (ruleListContainer.getScene() == null) {
                Platform.runLater(this::openAuthView);
                return;
            }
            Stage stage = (Stage) ruleListContainer.getScene().getWindow();
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

    private record RuleFormData(String name, RuleTriggerType triggerType, String sourceDeviceId,
                                Double expectedTriggerValue, ThresholdOperator thresholdOperator,
                                LocalTime triggerTime, String targetDeviceId, Double targetActionValue) {
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
