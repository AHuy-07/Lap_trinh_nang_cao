package client.controllers.loginAndSignUp;

import client.controllers.SceneController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class SignUpController {
    private SignUpModel signUpModel = new SignUpModel();

    @FXML
    private Label signUpError;

    @FXML
    private TextField usernameSignUp;

    @FXML
    private PasswordField passwordSignUp, re_passwordSignUp;

    @FXML
    private TextField roleSignUp;

    public void SignUp(ActionEvent event) throws Exception {
        roleSignUp.setText(roleSignUp.getText().toUpperCase());
        if (!passwordSignUp.getText().equals(re_passwordSignUp.getText())) {
            signUpError.setText("Passwords didn't match");
        }else if (!roleSignUp.getText().equals("SELLER") && !roleSignUp.getText().equals("BIDDER")) {
            signUpError.setText("Your role must be SELLER or BIDDER");
        }else {
            boolean signUpStatus = signUpModel.signUpStatus(usernameSignUp.getText(), passwordSignUp.getText(), roleSignUp.getText());
            if (!signUpStatus) {
                signUpError.setText("Username is already taken. Try another name");
            }else {
                signUpError.setText("Sign Up successful");
                SceneController.switchScene("/client/views/Login.fxml");
                // Chuyen sang scene cua Login
            }
        }
    }
}
