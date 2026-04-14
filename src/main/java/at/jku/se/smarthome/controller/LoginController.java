package at.jku.se.smarthome.controller;

import at.jku.se.smarthome.model.SmartHomeSystem;
import at.jku.se.smarthome.model.User;
import javafx.fxml.FXMLLoader;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    private final SmartHomeSystem system = SmartHomeSystem.createPersistentSystem();

    @FXML
    private VBox loginPane;

    @FXML
    private VBox loggedInPane;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label feedbackLabel;

    @FXML
    private Label sessionLabel;

    @FXML
    public void initialize() {
        updateSessionState();
    }

    @FXML
    public void handleLogin() {
        System.out.println("Login attempt for: " + emailField.getText()); // TODO: remove
        try {
            User user = system.loginUser(emailField.getText(), passwordField.getText());
            feedbackLabel.setStyle("-fx-text-fill: #2f7d32;");
            feedbackLabel.setText("Login successful.");
            sessionLabel.setText("Logged in as " + user.getEmail());
            passwordField.clear();
            openDashboard();
        } catch (IllegalArgumentException exception) {
            feedbackLabel.setStyle("-fx-text-fill: #b04a2f;");
            feedbackLabel.setText(exception.getMessage());
        }
    }

    @FXML
    public void handleLogout() {
        system.logoutUser();
        feedbackLabel.setStyle("-fx-text-fill: #6e6257;");
        feedbackLabel.setText("You have been logged out.");
        sessionLabel.setText("No active session.");
        emailField.clear();
        passwordField.clear();
        updateSessionState();
    }

    private void updateSessionState() {
        boolean loggedIn = system.isUserLoggedIn();
        loginPane.setVisible(!loggedIn);
        loginPane.setManaged(!loggedIn);
        loggedInPane.setVisible(loggedIn);
        loggedInPane.setManaged(loggedIn);

        if (loggedIn) {
            sessionLabel.setText("Logged in as " + system.getLoggedInUser().getEmail());
        } else {
            sessionLabel.setText("No active session.");
        }
    }

    private void openDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    LoginController.class.getResource("/at/jku/se/smarthome/fxml/main-view.fxml")
            );
            Scene scene = new Scene(loader.load(), 1000, 600);
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(scene);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to open dashboard", exception);
        }
    }
}
