package client.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class SignUpController {
    @FXML
    private Label signUpError;

    @FXML
    private TextField usernameSignUp;

    @FXML
    private PasswordField passwordSignUp, re_passwordSignUp;

    public void SignUp(ActionEvent event) throws Exception {
        if (false) { // Da ton tai username
            signUpError.setText("Username is already taken. Try another name");
        }else if (!passwordSignUp.getText().equals(re_passwordSignUp.getText())) {
            signUpError.setText("Passwords didn't match");
        }else {
            /*
            Tao mot tai khoan moi trong database
             */

            // Doi sang login
            SceneController.switchScene((Stage)signUpError.getScene().getWindow(), "/client/views/Login.fxml");
        }
    }
}
