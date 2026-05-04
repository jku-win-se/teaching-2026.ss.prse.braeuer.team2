package at.jku.se.smarthome.controller;

import at.jku.se.smarthome.model.CsvExportService;
import at.jku.se.smarthome.model.EnergyAggregationPeriod;
import at.jku.se.smarthome.model.EnergyDashboard;
import at.jku.se.smarthome.model.EnergyDeviceConsumption;
import at.jku.se.smarthome.model.EnergyRoomConsumption;
import at.jku.se.smarthome.model.SmartHomeSystem;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@SuppressWarnings("PMD")
public class EnergyController {
    private static final DateTimeFormatter WINDOW_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault());

    private final SmartHomeSystem system = SmartHomeSystem.createPersistentSystem();

    @FXML
    private ChoiceBox<EnergyAggregationPeriod> periodChoiceBox;

    @FXML
    private Label totalConsumptionLabel;

    @FXML
    private Label reportWindowLabel;

    @FXML
    private VBox roomConsumptionContainer;

    public void initialize() {
        if (!system.isUserLoggedIn()) {
            Platform.runLater(this::openAuthView);
            return;
        }
        periodChoiceBox.getItems().setAll(EnergyAggregationPeriod.DAY, EnergyAggregationPeriod.WEEK);
        periodChoiceBox.setValue(EnergyAggregationPeriod.DAY);
        periodChoiceBox.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(EnergyAggregationPeriod period) {
                return period == null ? "" : period.getDisplayName();
            }

            @Override
            public EnergyAggregationPeriod fromString(String value) {
                return EnergyAggregationPeriod.DAY.getDisplayName().equals(value)
                        ? EnergyAggregationPeriod.DAY
                        : EnergyAggregationPeriod.WEEK;
            }
        });
        periodChoiceBox.getSelectionModel().selectedItemProperty()
                .addListener((observable, previousPeriod, selectedPeriod) -> refreshEnergyDashboard());
        refreshEnergyDashboard();
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
    public void logout() {
        system.logoutUser();
        openAuthView();
    }

    @FXML
    public void exportEnergySummary() {
        File exportFile = chooseCsvFile("Export Energy Summary", "energy-summary.csv");
        if (exportFile != null) {
            try {
                EnergyDashboard dashboard = system.getEnergyDashboard(periodChoiceBox.getValue());
                CsvExportService.writeEnergyDashboardCsv(exportFile.toPath(), dashboard);
                showMessage("Export complete", "Energy summary CSV was exported successfully.");
            } catch (IllegalArgumentException | IllegalStateException exception) {
                showMessage("Export failed", exception.getMessage());
            }
        }
    }

    private void refreshEnergyDashboard() {
        EnergyAggregationPeriod selectedPeriod = periodChoiceBox.getValue();
        EnergyDashboard dashboard = system.getEnergyDashboard(selectedPeriod);

        totalConsumptionLabel.setText(formatKiloWattHours(dashboard.getTotalConsumptionKiloWattHours()));
        reportWindowLabel.setText(WINDOW_FORMATTER.format(dashboard.getStartInclusive())
                + " - " + WINDOW_FORMATTER.format(dashboard.getEndExclusive()));

        roomConsumptionContainer.getChildren().clear();
        if (dashboard.getRoomConsumptions().isEmpty()) {
            Label emptyState = new Label("No rooms available.");
            emptyState.setStyle("-fx-text-fill: #6e6257;");
            roomConsumptionContainer.getChildren().add(emptyState);
            return;
        }

        for (EnergyRoomConsumption roomConsumption : dashboard.getRoomConsumptions()) {
            roomConsumptionContainer.getChildren().add(createRoomConsumptionCard(roomConsumption));
        }
    }

    private VBox createRoomConsumptionCard(EnergyRoomConsumption roomConsumption) {
        Label roomNameLabel = new Label(roomConsumption.getRoomName());
        roomNameLabel.setStyle("-fx-font-size: 17; -fx-font-weight: bold; -fx-text-fill: #2b2b2b;");

        Label roomTotalLabel = new Label(formatKiloWattHours(roomConsumption.getTotalConsumptionKiloWattHours()));
        roomTotalLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #e8752e;");

        HBox header = new HBox(12, roomNameLabel, createSpacer(), roomTotalLabel);
        VBox deviceList = new VBox(8);
        for (EnergyDeviceConsumption deviceConsumption : roomConsumption.getDeviceConsumptions()) {
            deviceList.getChildren().add(createDeviceConsumptionRow(deviceConsumption));
        }
        if (roomConsumption.getDeviceConsumptions().isEmpty()) {
            Label emptyState = new Label("No devices in this room.");
            emptyState.setStyle("-fx-text-fill: #8a6f5a;");
            deviceList.getChildren().add(emptyState);
        }

        VBox card = new VBox(10, header, deviceList);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 14 16 14 16;");
        return card;
    }

    private HBox createDeviceConsumptionRow(EnergyDeviceConsumption deviceConsumption) {
        Label deviceNameLabel = new Label(deviceConsumption.getDeviceName());
        deviceNameLabel.setStyle("-fx-text-fill: #2b2b2b;");

        Label deviceTypeLabel = new Label(deviceConsumption.getDeviceType().name());
        deviceTypeLabel.setStyle("-fx-text-fill: #8a6f5a;");

        Label valueLabel = new Label(formatKiloWattHours(deviceConsumption.getConsumptionKiloWattHours()));
        valueLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2b2b2b;");

        HBox row = new HBox(10, deviceNameLabel, deviceTypeLabel, createSpacer(), valueLabel);
        row.setStyle("-fx-padding: 6 0 6 0;");
        return row;
    }

    private HBox createSpacer() {
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private String formatKiloWattHours(double value) {
        return String.format(Locale.ENGLISH, "%.3f kWh", value);
    }

    private File chooseCsvFile(String title, String initialFileName) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        fileChooser.setInitialFileName(initialFileName);
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        Stage stage = (Stage) roomConsumptionContainer.getScene().getWindow();
        return fileChooser.showSaveDialog(stage);
    }

    private void showMessage(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void navigateTo(String resourcePath, String errorMessage) {
        try {
            FXMLLoader loader = new FXMLLoader(EnergyController.class.getResource(resourcePath));
            Scene scene = new Scene(loader.load(), 1000, 600);
            Stage stage = (Stage) roomConsumptionContainer.getScene().getWindow();
            stage.setScene(scene);
        } catch (IOException exception) {
            throw new IllegalStateException(errorMessage, exception);
        }
    }

    private void openAuthView() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    EnergyController.class.getResource("/at/jku/se/smarthome/fxml/auth-view.fxml")
            );
            Scene scene = new Scene(loader.load(), 900, 600);
            if (roomConsumptionContainer.getScene() == null) {
                Platform.runLater(this::openAuthView);
                return;
            }
            Stage stage = (Stage) roomConsumptionContainer.getScene().getWindow();
            stage.setScene(scene);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to open login view", exception);
        }
    }
}
