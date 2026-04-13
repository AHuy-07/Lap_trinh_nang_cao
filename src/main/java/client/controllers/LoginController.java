package client.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {
    @FXML
    private Label loginStatusLabel;

    @FXML
    private TextField txtUsername;

    @FXML
    private PasswordField txtPassword;

    public void Login(ActionEvent event) throws Exception {
        if (txtUsername.getText().equals("huyngu123") && txtPassword.getText().equals("pass")) {
            loginStatusLabel.setText("Login successful");
        } else {
            loginStatusLabel.setText("Incorrect username or password!");
        }
    }

    public void SignUp(ActionEvent event) throws Exception {
        SceneController.switchScene((Stage)loginStatusLabel.getScene().getWindow(), "/client/views/SignUp.fxml");
    }
}
