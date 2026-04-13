package at.jku.se.smarthome.controller;

import at.jku.se.smarthome.model.Room;
import at.jku.se.smarthome.model.SmartHomeSystem;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
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

    private void refreshRoomOverview() {
        roomListContainer.getChildren().clear();
        for (Room room : system.getRooms()) {
            roomListContainer.getChildren().add(createRoomRow(room));
        }

        if (system.getRooms().isEmpty()) {
            Label emptyState = new Label("No rooms yet. Create your first room.");
            emptyState.setStyle("-fx-text-fill: #6e6257;");
            roomListContainer.getChildren().add(emptyState);
        }
    }

    private HBox createRoomRow(Room room) {
        Label roomName = new Label(room.getName());
        roomName.setStyle("-fx-font-size: 16; -fx-text-fill: #2b2b2b;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Button renameButton = new Button("Rename");
        renameButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #6e6257;");
        renameButton.setOnAction(event -> renameRoom(room));

        Button deleteButton = new Button("Delete");
        deleteButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #b04a2f;");
        deleteButton.setOnAction(event -> deleteRoom(room));

        HBox row = new HBox(8, roomName, spacer, renameButton, deleteButton);
        row.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 12 14 12 14;");
        return row;
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
