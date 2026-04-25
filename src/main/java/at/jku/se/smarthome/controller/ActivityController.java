package at.jku.se.smarthome.controller;

import at.jku.se.smarthome.model.ActivityActorType;
import at.jku.se.smarthome.model.ActivityLogEntry;
import at.jku.se.smarthome.model.SmartHomeSystem;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextBoundsType;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;

@SuppressWarnings("PMD")
public class ActivityController {
    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss").withZone(ZoneId.systemDefault());

    private final SmartHomeSystem system = SmartHomeSystem.createPersistentSystem();

    @FXML
    private VBox activityListContainer;

    public void initialize() {
        if (!system.isUserLoggedIn()) {
            Platform.runLater(this::openAuthView);
            return;
        }
        refreshActivityOverview();
    }

    @FXML
    public void openDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    ActivityController.class.getResource("/at/jku/se/smarthome/fxml/main-view.fxml")
            );
            Scene scene = new Scene(loader.load(), 1000, 600);
            Stage stage = (Stage) activityListContainer.getScene().getWindow();
            stage.setScene(scene);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to open dashboard view", exception);
        }
    }

    @FXML
    public void logout() {
        system.logoutUser();
        openAuthView();
    }

    private void refreshActivityOverview() {
        activityListContainer.getChildren().clear();

        if (system.getActivityLog().isEmpty()) {
            Label emptyState = new Label("No activity recorded yet.");
            emptyState.setStyle("-fx-text-fill: #6e6257;");
            activityListContainer.getChildren().add(emptyState);
            return;
        }

        system.getActivityLog().stream()
                .sorted(Comparator.comparing(ActivityLogEntry::getTimestamp).reversed())
                .map(this::createActivityCard)
                .forEach(activityListContainer.getChildren()::add);
    }

    private VBox createActivityCard(ActivityLogEntry entry) {
        Label timestampLabel = new Label(TIMESTAMP_FORMATTER.format(entry.getTimestamp()));
        timestampLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #8a6f5a;");

        Label deviceLabel = new Label(entry.getDeviceName());
        deviceLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #2b2b2b;");

        Label actorLabel = new Label(formatActor(entry));
        actorLabel.setStyle("-fx-text-fill: #6e6257;");

        Text stateChangeText = new Text(entry.getPreviousState() + " -> " + entry.getNewState());
        stateChangeText.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        stateChangeText.setBoundsType(TextBoundsType.VISUAL);
        stateChangeText.setStyle("-fx-fill: #e8752e;");

        VBox card = new VBox(6, timestampLabel, deviceLabel, stateChangeText, actorLabel);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 14 16 14 16;");
        return card;
    }

    private String formatActor(ActivityLogEntry entry) {
        String actorPrefix = entry.getActorType() == ActivityActorType.USER ? "User" : "Rule";
        return actorPrefix + ": " + entry.getActorName();
    }

    private void openAuthView() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    ActivityController.class.getResource("/at/jku/se/smarthome/fxml/auth-view.fxml")
            );
            Scene scene = new Scene(loader.load(), 900, 600);
            if (activityListContainer.getScene() == null) {
                Platform.runLater(this::openAuthView);
                return;
            }
            Stage stage = (Stage) activityListContainer.getScene().getWindow();
            stage.setScene(scene);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to open login view", exception);
        }
    }
}
