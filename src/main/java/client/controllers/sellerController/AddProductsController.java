package client.controllers.sellerController;

import client.controllers.SceneController;
import client.controllers.Session;
import common.Request;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class AddProductsController {
    @FXML private Label status;
    @FXML private TextField nameInput;
    @FXML private ChoiceBox<String> categoryBox;
    @FXML private TextArea descInput;

    // Định nghĩa sẵn CSS để code gọn gàng hơn
    private final String ERROR_STYLE = "-fx-padding: 10; -fx-background-radius: 5; -fx-border-color: red; -fx-border-radius: 5;";
    private final String NORMAL_STYLE = "-fx-padding: 10; -fx-background-radius: 5; -fx-border-color: #CED4DA; -fx-border-radius: 5;";

    // kiểm tra tên sản phẩm
    private boolean validName() {
        if (nameInput.getText().trim().isEmpty()) {
            nameInput.setStyle(ERROR_STYLE);
            return false;
        }
        nameInput.setStyle(NORMAL_STYLE);
        return true;
    }

    private void showError(String message) {
        status.setText(message);
        status.setVisible(true);
        status.setManaged(true);
    }

    private void hideError() {
        status.setVisible(false);
        status.setManaged(false);
    }

    @FXML
    public void handleAddProducts(ActionEvent event) throws Exception {
        hideError();


        boolean isNameValid = validName();

        if (!isNameValid) {
            showError("Vui lòng nhập tên sản phẩm!");
            return; // Dừng lại không lưu
        }


        String ProductName = nameInput.getText();
        String type = categoryBox.getValue();
        String details = descInput.getText();

        String[] info = {ProductName, type, details};
        Request addProductRequest = new Request("ADD_PRODUCTS", info);

        Session.getInstance().sendRequest(addProductRequest,
                response -> {
                    Platform.runLater(() -> {
                        if(response.getAction().equals("ADD_SUCCESS")){
//                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
//                            alert.setTitle("Thành công");
//                            alert.setHeaderText(null);
//                            alert.setContentText("Thêm sản phẩm thành công");
//                            alert.showAndWait();
                            status.setText("Thêm sản phẩm thành công!");
                            status.setStyle("-fx-text-fill: green;");
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            SceneController.switchScene("/client/views/seller/SellerManageProducts.fxml");
                        }else {
                            showError("Thất bại: " + response.getData());
                        }
                    });});
    }

    public void handleBackToDashboard(ActionEvent actionEvent) {
        SceneController.switchScene("/client/views/seller/SellerManageProducts.fxml");
    }
}
