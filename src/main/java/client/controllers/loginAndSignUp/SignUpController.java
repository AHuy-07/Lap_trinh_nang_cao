package client.controllers.loginAndSignUp;

import client.controllers.SceneController;
import client.controllers.Session;
import common.Request;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class SignUpController {
    @FXML
    private Label signUpStatusFail;

    @FXML
    private Label signUpStatusSuccess;

    @FXML
    private TextField usernameSignUp;

    @FXML
    private PasswordField passwordSignUp, re_passwordSignUp;

    @FXML
    private TextField roleSignUp;

    public void SignUp(ActionEvent event) throws Exception {
        String username = usernameSignUp.getText();
        String pwdSignUp = passwordSignUp.getText();
        String re_pwdSignUp = re_passwordSignUp.getText();
        String role = roleSignUp.getText();
        role = role.toUpperCase();

        if (!checkSpecialCase(username, pwdSignUp, re_pwdSignUp, role)) {
            return;
        }

        lockUI(true);

        String[] info = {username, pwdSignUp, role};
        Request signUpReq = new Request("SIGN_UP", info);

        Session.getInstance().sendRequest(
                signUpReq,
                response -> { // tham chiếu respone là đối tượng Request
                    lockUI(false);
                    if (response.getAction().equals("SIGN_UP_SUCCESS")) {
                        signUpStatusSuccess.setText("Đăng ký thành công!");
                        SceneController.switchScene("/client/views/Login.fxml");
                    } else if (response.getAction().equals("SIGN_UP_FAIL")) {
                        signUpStatusFail.setText("Tên tài khoản đã tồn tại. Hãy thử tên đăng nhập khác!");
                    }
                },
                error -> {
                    lockUI(false);
                    signUpStatusFail.setText("Không thể kết nối với Server");
                }
        );
    }

    private boolean checkSpecialCase(String username, String pwdSignUp, String re_pwdSignUp, String role) {
        if (username.isEmpty() || pwdSignUp.isEmpty() || re_pwdSignUp.isEmpty() || role.isEmpty()) {
            signUpStatusFail.setText("Bạn phải nhập đầy đủ thông tin!");
            return false;
        }
        if (!role.equals("BIDDER") && !role.equals("SELLER")) {
            signUpStatusFail.setText("Vai trò của bạn phải là BIDDER hoặc SELLER!"); // Kiểm tra role
            return false;
        }
        if (!pwdSignUp.equals(re_pwdSignUp)) {
            signUpStatusFail.setText("Hai mật khẩu không khớp. Vui lòng thử lại!");
            return false;
        }
        return true;
    }

    private void lockUI(boolean type) {
        usernameSignUp.setDisable(type);
        passwordSignUp.setDisable(type);
        re_passwordSignUp.setDisable(type);
        roleSignUp.setDisable(type);
    }
}
