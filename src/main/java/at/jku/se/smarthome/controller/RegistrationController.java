package at.jku.se.smarthome.controller;

import at.jku.se.smarthome.model.SmartHomeSystem;
import at.jku.se.smarthome.model.UserRole;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

@SuppressWarnings({"PMD.CommentRequired", "PMD.AtLeastOneConstructor"})
public class RegistrationController {

    private final SmartHomeSystem system = SmartHomeSystem.createPersistentSystem();

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private CheckBox registerAsMemberCheckBox;

    @FXML
    private Label feedbackLabel;

    @FXML
    public void handleRegister() {
        try {
            system.registerUser(emailField.getText(), passwordField.getText(), resolveRegistrationRole());
            feedbackLabel.setStyle("-fx-text-fill: #2f7d32;");
            feedbackLabel.setText("Account created successfully.");
            emailField.clear();
            passwordField.clear();
            registerAsMemberCheckBox.setSelected(false);
        } catch (IllegalArgumentException exception) {
            feedbackLabel.setStyle("-fx-text-fill: #b04a2f;");
            feedbackLabel.setText(exception.getMessage());
        }
    }

    private UserRole resolveRegistrationRole() {
        return registerAsMemberCheckBox.isSelected() ? UserRole.MEMBER : UserRole.OWNER;
    }
}
