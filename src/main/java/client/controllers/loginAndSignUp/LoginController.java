package client.controllers.loginAndSignUp;

import client.controllers.SceneController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {
    private LoginModel loginModel = new LoginModel();

    @FXML
    private Label loginStatusLabel;

    @FXML
    private TextField txtUsername;

    @FXML
    private PasswordField txtPassword;

    public void Login(ActionEvent event) throws Exception {
        if (loginModel.isLogin(txtUsername.getText(), txtPassword.getText())) {
            loginStatusLabel.setText("Login successful");
        } else {
            loginStatusLabel.setText("Incorrect username or password!");
        }
    }

    public void SignUp(ActionEvent event) {
        SceneController.switchScene("/client/views/SignUp.fxml");
    }
}
