package client.controllers;

import common.Request;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import common.models.Room;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

import java.awt.event.ActionEvent;
import java.util.List;

public class AdminDashboardController {
    @FXML private TableView<Room> tablePendingRooms;
    /*
    Tham số đầu là kiểu dữ liệu của mỗi phòng, tham số thứ hai là thuộc tính
    thuộc về Room của cột đấy
    */
    @FXML private TableColumn<Room, String> colRoomId;
    @FXML private TableColumn<Room, String> colRoomName;
    @FXML private TableColumn<Room, String> colProductId;
    @FXML private TableColumn<Room, Long> colPrice;
    @FXML private TableColumn<Room, Void> colAction;

    /*
    Đây là list có khả năng quan sát, giao diện bảng sẽ tự
    động cập nhật (như thêm dòng, xóa phòng) mà không phải load lại trang.
     */
    private ObservableList<Room> pendingRooms = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        // Ánh xạ các thuộc tính của Room vào các cột
        colRoomId.setCellValueFactory(new PropertyValueFactory<>("roomId"));
        colRoomName.setCellValueFactory(new PropertyValueFactory<>("roomName"));
        colProductId.setCellValueFactory(new PropertyValueFactory<>("productId"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("startingPrice"));

        // Cài đặt dữ liệu lên bảng
        tablePendingRooms.setItems(pendingRooms);

        // Tạo các nút duyệt, từ chối
        setupActionButtons();

        refreshData();
    }

    @FXML
    private void refreshData() {
        Request req = new Request("GET_PENDING_ROOMS", null);

        Session.getInstance().sendRequest(
                req,
                response -> {
                    if (response.getAction().equals("GET_PENDING_ROOMS_SUCCESS")) {
                        List<Room> pendingRoomList = (List<Room>) response.getData();
                        Platform.runLater(() -> {
                            pendingRooms.setAll(pendingRoomList);
                        });
                    } else if (response.getAction().equals("NOT_EXIST_PENDING_ROOM")) {
                        pendingRooms.clear();
                    }
                },
                error -> {
                }
        );
    }

    @FXML
    private void setupActionButtons() {
        Callback<TableColumn<Room, Void>, TableCell<Room, Void>> cellFactory = param -> new TableCell<>() {
            private final Button btnApprove = new Button("Duyệt");
            private final Button btnReject = new Button("Từ chối");
            private final HBox hbox = new HBox(10, btnApprove, btnReject); // Số 10 là khoảng cách 2 nút

            {
                btnApprove.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-cursor: hand;");
                btnReject.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand;");

                btnApprove.setOnAction(event -> {
                    Room room = getTableView().getItems().get(getIndex());
                    handleDecision(room, "APPROVE_ROOM");
                });

                btnReject.setOnAction(event -> {
                    Room room = getTableView().getItems().get(getIndex());
                    handleDecision(room, "REJECTED_ROOM");
                });
            }

            // Cập nhật các phòng mới liên tục
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : hbox);
            }
        };
        colAction.setCellFactory(cellFactory);
    }

    // Hàm xử lí gửi quyết định của Admin lên Server
    private void handleDecision(Room room, String actionType) {
        Request req = new Request(actionType, room.getRoomId());

        Session.getInstance().sendRequest(
                req,
                response -> {
                    if (response.getAction().equals("SUCCESS")) {
                        Platform.runLater(() -> {
                            pendingRooms.remove(room);
                        });
                    }
                },
                error -> {

                }
        );
    }
}