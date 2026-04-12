package at.jku.se.smarthome.controller;

import at.jku.se.smarthome.model.SmartHomeSystem;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegistrationController {

    private final SmartHomeSystem system = SmartHomeSystem.createPersistentSystem();

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label feedbackLabel;

    @FXML
    public void handleRegister() {
        try {
            system.registerUser(emailField.getText(), passwordField.getText());
            feedbackLabel.setStyle("-fx-text-fill: #2f7d32;");
            feedbackLabel.setText("Account created successfully.");
            emailField.clear();
            passwordField.clear();
        } catch (IllegalArgumentException exception) {
            feedbackLabel.setStyle("-fx-text-fill: #b04a2f;");
            feedbackLabel.setText(exception.getMessage());
        }
    }
}
