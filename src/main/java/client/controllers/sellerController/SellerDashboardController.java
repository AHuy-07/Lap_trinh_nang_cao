package client.controllers.sellerController;

import client.controllers.SceneController;
import client.controllers.Session;
import common.Request;
import common.models.Room;
import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.List;

public class SellerDashboardController {
    private static final Logger logger = LoggerFactory.getLogger(SellerDashboardController.class);
    @FXML private TableView<Room> tableMyRooms;
    @FXML private TableColumn<Room, String> colRoomId;
    @FXML private TableColumn<Room, String> colRoomName;
    @FXML private TableColumn<Room, String> colStatus;
    @FXML private TableColumn<Room, Void> colAction;

    // Thành phần cho In-App Notification
    @FXML private HBox notificationBox;
    @FXML private Label lblNotification;

    private ObservableList<Room> myRoomsList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Đăng kí Controller với Session để nhận thông báo
        Session.getInstance().setSellerDashboardController(this);

        // Thiết lập các cột cho TableView
        colRoomId.setCellValueFactory(new PropertyValueFactory<>("roomId"));
        colRoomName.setCellValueFactory(new PropertyValueFactory<>("roomName"));

        setupStatusCol(); // Tô màu trạng thái
        setupActionButtons(); // Tạo nút "Vào phòng"

        tableMyRooms.setItems(myRoomsList);

        // Load dữ liệu lần đầu từ Database;
        loadMyRooms();
    }

    public void loadMyRooms() {
        Request req = new Request("GET_MY_ROOMS", null);
        Session.getInstance().sendRequest(req, response -> {
            if (response.getAction().equals("GET_MY_ROOMS_SUCCESS")) {
                List<Room> list = (List<Room>) response.getData();
                if (!list.isEmpty()) {
                    myRoomsList.setAll(list);
                }
            }
        });
    }

    public void showInAppNotification(String msg, String color) {
        lblNotification.setText(msg);
        notificationBox.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 8;");

        notificationBox.setVisible(true);
        notificationBox.setManaged(true); // Sẽ được bố trí hiển thị
        notificationBox.setOpacity(1.0);

        // Hiệu ứng mờ dần sau 3 giây
        FadeTransition fade = new FadeTransition(Duration.seconds(3), notificationBox);
        fade.setFromValue(1.0); // Độ mờ bắt đầu
        fade.setToValue(0.0); // Độ mờ kết thúc
        fade.setDelay(Duration.seconds(2)); // Hiển thị 2 giây trước khi biến mất trong 3s
        fade.setOnFinished(e -> {
            notificationBox.setVisible(false);
            notificationBox.setManaged(false);
        });
        fade.play();
    }

    private void setupStatusCol() {
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty); // Empty là boolean xác định xem có tồn tại dữ liệu ko. Nếu ko có thì sẽ là true
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.equals("PENDING")) {
                        setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;"); // Vàng
                    } else if (item.equals("ACTIVE")) {
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;"); // Xanh lá
                    } else {
                        setStyle("-fx-text-fill: #7f8c8d;"); // Xám cho REJECTED/CLOSED
                    }
                }
            }
        });
    }

    // Tạo nút vào phòng và thiết lập nút
    private void setupActionButtons() {
        Callback<TableColumn<Room, Void>, TableCell<Room, Void>> cellFactory = param -> new TableCell<>() {
            private final Button btnEnter = new Button("Vào phòng");
            {
                btnEnter.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-cursor: hand;");
                btnEnter.setOnAction(event -> {
                    Room room = getTableView().getItems().get(getIndex());
                    handleEnterRoom(room);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Room room = getTableView().getItems().get(getIndex());
                    btnEnter.setDisable(!room.getStatus().equals("ACTIVE"));
                    setGraphic(btnEnter);
                }
            }
        };
        colAction.setCellFactory(cellFactory);
    }

    private void handleEnterRoom(Room room) {
        // Vao phong dau gia
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/views/seller/SellerAuctionRoom.fxml"));

            Parent autionRoomRoot = loader.load();
            SellerAuctionRoomController controller = loader.getController();
            controller.initRoom(room);
            SceneController.contentGroup.getChildren().setAll(autionRoomRoot);
        } catch (IOException e) {
            logger.error("Lỗi khi seller {} vào phòng {}", room.getSellerName(), room.getRoomId(), e);
        }
    }

    @FXML
    public void reload(ActionEvent event) {
        loadMyRooms();
    }

    @FXML
    public void switchToCreateRoom(ActionEvent event) {
        SceneController.switchScene("/client/views/seller/CreateRoom.fxml");
    }

    @FXML
    public void switchToProductView(ActionEvent event) {
        SceneController.switchScene("/client/views/seller/SellerManageProducts.fxml");
    }
}