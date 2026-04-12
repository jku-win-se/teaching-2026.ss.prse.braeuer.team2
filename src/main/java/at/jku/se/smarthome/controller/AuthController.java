package at.jku.se.smarthome.controller;

import at.jku.se.smarthome.model.SmartHomeSystem;
import at.jku.se.smarthome.model.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

public class AuthController {

    private final SmartHomeSystem system = SmartHomeSystem.createPersistentSystem();

    @FXML
    private VBox loginPane;

    @FXML
    private VBox registrationPane;

    @FXML
    private TextField loginEmailField;

    @FXML
    private PasswordField loginPasswordField;

    @FXML
    private Label loginFeedbackLabel;

    @FXML
    private TextField registrationEmailField;

    @FXML
    private PasswordField registrationPasswordField;

    @FXML
    private Label registrationFeedbackLabel;

    @FXML
    public void initialize() {
        showLogin();
    }

    @FXML
    public void handleLogin() {
        try {
            User user = system.loginUser(loginEmailField.getText(), loginPasswordField.getText());
            loginFeedbackLabel.setStyle("-fx-text-fill: #2f7d32;");
            loginFeedbackLabel.setText("Login successful.");
            loginPasswordField.clear();
            openDashboard(user);
        } catch (IllegalArgumentException exception) {
            loginFeedbackLabel.setStyle("-fx-text-fill: #b04a2f;");
            loginFeedbackLabel.setText(exception.getMessage());
        }
    }

    @FXML
    public void handleRegister() {
        try {
            system.registerUser(registrationEmailField.getText(), registrationPasswordField.getText());
            registrationFeedbackLabel.setStyle("-fx-text-fill: #2f7d32;");
            registrationFeedbackLabel.setText("Account created successfully. You can now log in.");
            registrationEmailField.clear();
            registrationPasswordField.clear();
            showLogin();
        } catch (IllegalArgumentException exception) {
            registrationFeedbackLabel.setStyle("-fx-text-fill: #b04a2f;");
            registrationFeedbackLabel.setText(exception.getMessage());
        }
    }

    @FXML
    public void showRegistration() {
        loginPane.setVisible(false);
        loginPane.setManaged(false);
        registrationPane.setVisible(true);
        registrationPane.setManaged(true);
        loginFeedbackLabel.setText("");
    }

    @FXML
    public void showLogin() {
        registrationPane.setVisible(false);
        registrationPane.setManaged(false);
        loginPane.setVisible(true);
        loginPane.setManaged(true);
        registrationFeedbackLabel.setText("");
    }

    private void openDashboard(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    AuthController.class.getResource("/at/jku/se/smarthome/fxml/main-view.fxml")
            );
            Scene scene = new Scene(loader.load(), 1000, 600);
            Stage stage = (Stage) loginEmailField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("homeE - " + user.getEmail());
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to open dashboard", exception);
        }
    }
}
