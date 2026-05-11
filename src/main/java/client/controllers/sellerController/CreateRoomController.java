package client.controllers.sellerController;

import client.controllers.SceneController;
import client.controllers.Session;
import common.Request;
import common.models.Room;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;

import javax.swing.*;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.function.UnaryOperator;

public class CreateRoomController {
    private static final int SERVER_PORT = 8080;
    private static final String SERVER_ADDRESS = "localhost";

    @FXML private TextField txtRoomId;
    @FXML private TextField txtRoomName;
    @FXML private TextField txtProductId;
    @FXML private TextField txtStartingPrice;
    @FXML private TextField txtDetails;
    @FXML private DatePicker beginDate;
    @FXML private ComboBox<String> beginHour;
    @FXML private ComboBox<String> beginMinute;
    @FXML private Label status;
    @FXML private Button btnSubmit;

    @FXML
    public void initialize() {
        for (int i = 0; i <= 23; i++) {
            beginHour.getItems().add(String.format("%02d", i));
        }
        for (int i = 0; i <= 59; i++) {
            beginMinute.getItems().add(String.format("%02d", i));
        }
        beginHour.setValue("12");
        beginMinute.setValue("00");

        txtRoomId.textProperty().addListener((observableValue, oldValue, newValue) -> {
            if (newValue.isEmpty() || !newValue.startsWith("R_")) {
                txtRoomId.setStyle("-fx-border-color: red; -fx-border-width: 2px");
                status.setText("Lỗi! Mã phòng phải bắt đầu bằng 'R_'");
                btnSubmit.setDisable(true);
            }else {
                txtRoomId.setStyle("-fx-border-color: green");
                status.setText("");
                btnSubmit.setDisable(false);
            }
        });

        txtProductId.textProperty().addListener((observableValue, oldValue, newValue) -> {
            if (newValue.isEmpty() || !newValue.startsWith("P_")) {
                txtProductId.setStyle("-fx-border-color: red; -fx-border-width: 2px");
                status.setText("Lỗi! Mã sản phẩm phải bắt đầu bằng 'P_'");
                btnSubmit.setDisable(true);
            }else {
                txtProductId.setStyle("-fx-border-color: green");
                status.setText("");
                btnSubmit.setDisable(false);
            }
        });



        // Tạo bộ lọc: Chỉ cho phép các ký tự là số
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String text = change.getControlNewText();
            if (text.matches("\\d*")) { // d* la chi chap nhan cac chu so
                return change;
            }
            return null;
        };
        TextFormatter<String> numberFormatter = new TextFormatter<>(filter);
        txtStartingPrice.setTextFormatter(numberFormatter);

        // Thêm listener cho từng phím
        switchKeyAddListener(txtRoomId);
        switchKeyAddListener(txtRoomName);
        switchKeyAddListener(txtProductId);
        switchKeyAddListener(txtStartingPrice);
    }

    private void switchKeyAddListener(TextField tf) {
        tf.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene != null) {
                setupKeyboardEvents(newScene);
            }
        });
    }

    private void switchKey(TextField textField1, TextField textField2) {
        textField1.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                textField2.requestFocus();
            }
        });
    }

    // Khi bấm Enter thì sẽ tự động sang ô khác
    public void setupKeyboardEvents(Scene scene) {
        switchKey(txtRoomId, txtRoomName);
        switchKey(txtRoomName, txtProductId);
        switchKey(txtProductId, txtStartingPrice);
        switchKey(txtStartingPrice, txtDetails);
    }

    private boolean check(String roomId, String roomName, String productId, String startingPrice) {
        if (roomId == null || roomId.trim().isEmpty()) return false;
        if (roomName == null || roomName.trim().isEmpty()) return false;
        if (productId == null || productId.trim().isEmpty()) return false;
        if (startingPrice == null || startingPrice.trim().isEmpty()) return false;

        return true;
    }

    private boolean isValidDateTime(String dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        try {
            LocalDateTime inputTime = LocalDateTime.parse(dateTime, formatter);
            LocalDateTime now = LocalDateTime.now();

            if (inputTime.isBefore(now)) {
                status.setText("Lỗi! Thời gian bắt đầu phải ở tương lai.");
                return false;
            }
        } catch (DateTimeParseException e) {
            status.setText("Lỗi! Ngày giờ không tồn tại hoặc sai định dạng.");
            return false;
        }
        return true;
    }

    // Xử lí phần gửi yêu cầu
    public void checkRoom(ActionEvent event) {
        if (beginDate.getValue() == null || beginHour.getValue() == null || beginMinute.getValue() == null) {
            status.setText("Vui lòng chọn đầy đủ ngày và giờ!");
            return;
        }
        String roomId = txtRoomId.getText();
        String roomName = txtRoomName.getText();
        String productId = txtProductId.getText();
        String sellerName = Session.getInstance().getCurrentUsername();
        String startingPrice = txtStartingPrice.getText();
        String dateStr = beginDate.getValue().toString();
        String beginTime = dateStr + " " + beginHour.getValue() + ":" + beginMinute.getValue();
        if (!check(roomId, roomName, productId, startingPrice)) {
            status.setText("Hãy nhập đầy đủ thông tin cần thiết!");
            return;
        }

        if (!isValidDateTime(beginTime)) {
            status.setText("Thời gian không hợp lệ!");
            return;
        }
        Room roomRequest = new Room(roomId, roomName, productId, sellerName, Long.parseLong(startingPrice), beginTime);
        Request createRoomRequest = new Request("CREATE_ROOM", roomRequest);

        Session.getInstance().sendRequest(
                createRoomRequest,
                response -> {
                    if (response.getAction().equals("SEND_CREATE_ROOM_SUCCESS")) {
                        status.setText((String)response.getData());
                        status.setStyle("-fx-text-fill: green;");
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        SceneController.switchScene("/client/views/seller/SellerDashboard.fxml");
                    } else if (response.getAction().equals("CREATE_ROOM_FAIL")) {
                        status.setText((String)response.getData());
                        status.setStyle("-fx-text-fill: red;");
                    }
                }
        );
    }

    @FXML
    public void returnFromCreateRoom(ActionEvent event) {
        SceneController.switchScene("/client/views/seller/SellerDashboard.fxml");
    }

}
