package client.controllers.bidderController;

import client.controllers.SceneController;
import client.controllers.Session;
import common.Request;
import common.models.Room;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.io.IOException;
import java.util.List;

public class BidderDashboardController {
    @FXML private Label statusLabel;
    @FXML private TableView<Room> activeRoomsTable;
    @FXML private TableColumn<Room, String> roomNameColumn;
    @FXML private TableColumn<Room, String> productNameColumn;
    @FXML private TableColumn<Room, String> sellerNameColumn;
    @FXML private TableColumn<Room, String> beginTimeColumn;
    @FXML private TableColumn<Room, String> statusColumn;
    @FXML private TableColumn<Room, Number> winPriceColumn;
    @FXML private TableColumn<Room, String> winnerColumn;

    @FXML
    private void initialize() {
        setupTableColumns();
        loadActiveRooms();
    }

    private void setupTableColumns() {
        roomNameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getRoomName())
        );
        productNameColumn.setCellValueFactory(cellData -> {
            String name = cellData.getValue().getProductName();
            return new SimpleStringProperty(name != null ? name : "");
        });
        sellerNameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getSellerName())
        );
        beginTimeColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getBeginTime())
        );
        statusColumn.setCellValueFactory(cellData -> {
            Room room = cellData.getValue();
            String status = room.getStatus();
            if ("CLOSED".equals(status)) return new SimpleStringProperty("Closed");
            if ("ACTIVE".equals(status)) {
                if (!isRoomStarted(room)) return new SimpleStringProperty("Upcoming");
                return new SimpleStringProperty("Active");
            }
            return new SimpleStringProperty(status);
        });
        winPriceColumn.setCellValueFactory(cellData ->
                new SimpleLongProperty(cellData.getValue().getWinPrice())
        );
        winnerColumn.setCellValueFactory(cellData -> {
            String winner = cellData.getValue().getWinnerUsername();
            if (winner == null || winner.isBlank()) {
                winner = "Chưa có";
            }
            return new SimpleStringProperty(winner);
        });

        activeRoomsTable.setRowFactory(tv -> new javafx.scene.control.TableRow<Room>() {
            @Override
            protected void updateItem(Room room, boolean empty) {
                super.updateItem(room, empty);
                if (empty || room == null) {
                    setStyle("");
                    setTooltip(null);
                } else if ("CLOSED".equals(room.getStatus())) {
                    setStyle("-fx-text-fill: #95a5a6;");
                    setTooltip(new javafx.scene.control.Tooltip("Phòng đã kết thúc"));
                } else if (!isRoomStarted(room)) {
                    setStyle("-fx-text-fill: #f39c12;");
                    setTooltip(new javafx.scene.control.Tooltip("Phòng bắt đầu lúc: " + room.getBeginTime()));
                } else {
                    setStyle("");
                    setTooltip(null);
                }
            }
        });
    }

    private boolean isRoomStarted(Room room) {
        if (room.getBeginTime() == null || room.getBeginTime().isBlank()) return true;
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            LocalDateTime beginTime = LocalDateTime.parse(room.getBeginTime(), formatter);
            return !LocalDateTime.now().isBefore(beginTime);
        } catch (Exception e) {
            return true;
        }
    }

    private void loadActiveRooms() {
        statusLabel.setText("Đang tải danh sách phòng...");

        Session.getInstance().sendRequest(
                new Request("GET_ACTIVE_ROOMS", null),
                response -> {
                    if ("GET_ACTIVE_ROOMS_SUCCESS".equals(response.getAction())) {
                        List<Room> activeRooms = (List<Room>) response.getData();
                        activeRoomsTable.setItems(FXCollections.observableArrayList(activeRooms));
                        statusLabel.setText("Có " + activeRooms.size() + " phòng đang hoạt động");
                    } else {
                        statusLabel.setText("Không thể tải danh sách phòng");
                    }
                }
        );
    }


    @FXML
    private void handleRefreshRooms() {
        loadActiveRooms();
    }

    @FXML
    private void handleJoinSelectedRoom() {
        Room selectedRoom = activeRoomsTable.getSelectionModel().getSelectedItem();
        if (selectedRoom == null) {
            statusLabel.setText("Vui lòng chọn một phòng để vào đấu giá");
            return;
        }
        if ("CLOSED".equals(selectedRoom.getStatus())) {
            statusLabel.setText("Phòng đã kết thúc, không thể vào");
            return;
        }
        // Cảnh báo nếu phòng chưa đến giờ
        if (!isRoomStarted(selectedRoom)) {
            statusLabel.setText("⚠ Phòng chưa đến giờ bắt đầu! Bắt đầu lúc: " + selectedRoom.getBeginTime());
            return;
        }
        openAuctionRoom(selectedRoom);
    }

    private void openAuctionRoom(Room room) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/client/views/bidder/AuctionRoom.fxml")
            );

            Parent auctionRoomRoot = loader.load();

            AuctionRoomController controller = loader.getController();


            client.controllers.SceneController.contentGroup.getChildren().setAll(auctionRoomRoot);
            controller.initRoom(room);
            /*
            - Phần Parent auctionRoomRoot là để lấy khung cây của file fxml, bao gồm các thuộc
            tính như AnchorPane, hay các thành phần javafx
            - AuctionRoomController controller = loader.getController(); là để lấy về phần
            controller của file fxml đó. Sau đó controller đó sẽ thêm các giá trị vào
            - Hiểu Parent auctionRoomRoot là 1 cái cây, còn controller sẽ giúp thêm phần
            nội dung vào
             */
        } catch (IOException e) {
            statusLabel.setText("Không thể mở phòng đấu giá");
            e.printStackTrace();
        }
    }

    @FXML
    public void switchToWalletView(ActionEvent event){
        SceneController.switchScene("/client/views/Wallet.fxml");
    }

    public void switchToWonProductsView(ActionEvent event) {
        SceneController.switchScene("/client/views/bidder/WonProduct.fxml");
    }
}