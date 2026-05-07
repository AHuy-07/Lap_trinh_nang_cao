package client.controllers.loginAndSignUp;

import client.controllers.SceneController;
import client.controllers.Session;
import common.Request;
import common.models.User;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {
    private static final int SERVER_PORT = 8080;
    private static final String SERVER_ADDRESS = "localhost";
    @FXML
    private Label loginStatusLabel;

    @FXML
    private TextField txtUsername;

    @FXML
    private PasswordField txtPassword;

    public void Login(ActionEvent event) throws Exception {
        String username = txtUsername.getText();
        String password = txtPassword.getText();

        if (username.isEmpty() || password.isEmpty()) {
            loginStatusLabel.setText("Hay nhap day du thong tin");
            return;
        }

        loginStatusLabel.setText("Đang kết nối...");
        lockUI(true);

        String[] info = {username, password};
        Request loginReq = new Request("LOGIN", info);

        Session.getInstance().sendRequest(
                loginReq,
                response -> {
                    Platform.runLater(() -> {
                        lockUI(false);
                        if (response.getAction().equals("LOGIN_SUCCESS")) {
                            loginStatusLabel.setText("Đăng nhập thành công!");
                            User user = (User) response.getData();
                            Session.getInstance().setCurrentUser(user);
                            // Phần này sẽ có SceneController
                            if (user.getRole().equals("SELLER")) {
                                SceneController.switchScene("/client/views/seller/createRoom.fxml");
                            } else if (user.getRole().equals("ADMIN")) {
                                SceneController.switchScene("/client/views/admin/AdminDashboard.fxml");
                            } else {
                                loginStatusLabel.setText("Vai trò " + user.getRole() + " không được hỗ trợ!");
                            }
                        }
                    });

                },
                error -> {
                    lockUI(false);
                    loginStatusLabel.setText("Không thể đăng nhập. Vui lòng thử lại sau");
                }
        );
    }



    private void lockUI(boolean type) {
        txtUsername.setDisable(type);
        txtPassword.setDisable(type);
    }

    public void SignUp(ActionEvent event) {
        SceneController.switchScene("/client/views/SignUp.fxml");
    }
}
