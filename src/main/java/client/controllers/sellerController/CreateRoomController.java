package client.controllers.sellerController;

import client.controllers.SceneController;
import client.controllers.Session;
import common.Request;
import common.models.Product;
import common.models.Room;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;

import javax.swing.*;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.function.UnaryOperator;

public class CreateRoomController {
    private static final int SERVER_PORT = 8080;
    private static final String SERVER_ADDRESS = "localhost";

    @FXML private TextField txtRoomName;
    @FXML private TextField txtProductId;
    @FXML private TextField txtStartingPrice;
    @FXML private TextField txtDetails;
    @FXML private DatePicker beginDate;
    @FXML private ComboBox<String> beginHour;
    @FXML private ComboBox<String> beginMinute;
    @FXML private DatePicker endDate;
    @FXML private ComboBox<String> endHour;
    @FXML private ComboBox<String> endMinute;
    @FXML private Label status;
    @FXML private Button btnSubmit;

    @FXML
    public void initialize() {
        // Khởi tạo thời gian ban đầu
        for (int i = 0; i <= 23; i++) {
            beginHour.getItems().add(String.format("%02d", i));
        }
        for (int i = 0; i <= 59; i++) {
            beginMinute.getItems().add(String.format("%02d", i));
        }
        beginHour.setValue("12");
        beginMinute.setValue("00");

        for (int i = 0; i <= 23; i++) {
            endHour.getItems().add(String.format("%02d", i));
        }
        for (int i = 0; i <= 59; i++) {
            endMinute.getItems().add(String.format("%02d", i));
        }
        endHour.setValue("12");
        endMinute.setValue("00");

//        txtRoomId.textProperty().addListener((observableValue, oldValue, newValue) -> {
//            if (newValue.isEmpty() || !newValue.startsWith("R_")) {
//                txtRoomId.setStyle("-fx-border-color: red; -fx-border-width: 2px");
//                status.setText("Lỗi! Mã phòng phải bắt đầu bằng 'R_'");
//                btnSubmit.setDisable(true);
//            }else {
//                txtRoomId.setStyle("-fx-border-color: green");
//                status.setText("");
//                btnSubmit.setDisable(false);
//            }
//        });

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

        //tạo phòng bằng cách bấm chuột phải trong ds sản phẩm
        Product selectedProduct = Session.getInstance().getCurrentProduct();
        if (selectedProduct != null) {
            txtProductId.setText(selectedProduct.getId());
            txtProductId.setEditable(false);
            Session.getInstance().setCurrentProduct(null);

        } else {
            txtProductId.setText("");
            txtProductId.setEditable(true);
        }

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
        switchKey(txtRoomName, txtProductId);
        switchKey(txtProductId, txtStartingPrice);
        switchKey(txtStartingPrice, txtDetails);
    }



    private boolean isValidDateTime(String beginTimeStr, String endTimeStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        try {
            LocalDateTime beginTime = LocalDateTime.parse(beginTimeStr, formatter);
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime endTime = LocalDateTime.parse(endTimeStr, formatter);

            if (beginTime.isBefore(now)) {
                status.setText("Lỗi! Thời gian bắt đầu phải ở tương lai.");
                status.setStyle("-fx-text-fill: #e74c3c;");
                return false;
            }

            if (!beginTime.isBefore(endTime) || endTime.isEqual(beginTime)) {
                status.setText("Lỗi! Thời gian kết thúc phải sau thời gian bắt đầu");
                status.setStyle("-fx-text-fill: #e74c3c;");
                return false;
            }

            // Kiểm tra thời gian đấu giá không được vượt quá 7 ngày, kéo dài ít nhất 10 phút
            Duration durationRoom = Duration.between(beginTime, endTime);
            long minuteDurationRoom = durationRoom.toMinutes();
            long maxMinutes = 7 * 24 * 60; // 7 ngày
            long minMinutes = 10;

            if (minuteDurationRoom > maxMinutes) {
                status.setText("Lỗi! Thời gian đấu giá không được vượt quá 7 ngày.");
                status.setStyle("-fx-text-fill: #e74c3c;");
                return false;
            }

            if (minuteDurationRoom < minMinutes) {
                status.setText("Lỗi! Thời gian đấu giá phải kéo dài ít nhất 10 phút.");
                status.setStyle("-fx-text-fill: #e74c3c;");
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
        if (beginDate.getValue() == null || beginHour.getValue() == null || beginMinute.getValue() == null
            || endDate.getValue() == null || endHour.getValue() == null || endMinute.getValue() == null) {
            status.setText("Vui lòng chọn đầy đủ ngày và giờ!");
            return;
        }

        String roomName = txtRoomName.getText();
        String productId = txtProductId.getText();
        String sellerName = Session.getInstance().getCurrentUsername();
        String startingPrice = txtStartingPrice.getText();
        String dateStr1 = beginDate.getValue().toString();
        String dateStr2 = endDate.getValue().toString();
        String beginTime = dateStr1 + " " + beginHour.getValue() + ":" + beginMinute.getValue();
        String endTime = dateStr2 + " " + endHour.getValue() + ":" + endMinute.getValue();
        if (!check(roomName, productId, startingPrice)) {
            status.setText("Hãy nhập đầy đủ thông tin cần thiết!");
            return;
        }

        if (!isValidDateTime(beginTime, endTime)) {
            //status.setText("Thời gian không hợp lệ!");
            return;
        }
        Room roomRequest = new Room(null, roomName, productId, sellerName, Long.parseLong(startingPrice), beginTime, endTime);
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
    private boolean check(String roomName, String productId, String startingPrice) {
        if (roomName == null || roomName.trim().isEmpty()) return false;
        if (productId == null || productId.trim().isEmpty()) return false;
        if (startingPrice == null || startingPrice.trim().isEmpty()) return false;

        return true;
    }
    @FXML
    public void returnFromCreateRoom(ActionEvent event) {
        SceneController.switchScene("/client/views/seller/SellerDashboard.fxml");
    }

}
