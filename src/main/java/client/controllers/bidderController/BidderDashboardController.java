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

import java.io.IOException;
import java.util.List;

public class BidderDashboardController {
    @FXML private Label statusLabel;
    @FXML private TableView<Room> activeRoomsTable;
    @FXML private TableColumn<Room, String> roomIdColumn;
    @FXML private TableColumn<Room, String> roomNameColumn;
    @FXML private TableColumn<Room, String> productIdColumn;
    @FXML private TableColumn<Room, String> sellerNameColumn;
    @FXML private TableColumn<Room, Number> currentPriceColumn;
    @FXML private TableColumn<Room, Number> bidStepColumn;
    @FXML private TableColumn<Room, String> winnerColumn;

    @FXML
    private void initialize() {
        setupTableColumns();
        loadActiveRooms();
    }

    private void setupTableColumns() {
        roomIdColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getRoomId())
        );

        roomNameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getRoomName())
        );

        productIdColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getProductId())
        );

        sellerNameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getSellerName())
        );

        currentPriceColumn.setCellValueFactory(cellData -> {
            Room room = cellData.getValue();
            long currentPrice = Math.max(room.getStartingPrice(), room.getWinPrice());
            return new SimpleLongProperty(currentPrice);
        });

        bidStepColumn.setCellValueFactory(cellData ->
                new SimpleLongProperty(Room.calculateDefaultBidStep(cellData.getValue().getStartingPrice()))
        );

        winnerColumn.setCellValueFactory(cellData -> {
            String winner = cellData.getValue().getWinnerUsername();

            if (winner == null || winner.isBlank()) {
                winner = "Chưa có";
            }

            return new SimpleStringProperty(winner);
        });
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

        openAuctionRoom(selectedRoom);
    }

    private void openAuctionRoom(Room room) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/client/views/bidder/AuctionRoom.fxml")
            );

            Parent auctionRoomRoot = loader.load();

            AuctionRoomController controller = loader.getController();
            controller.initRoom(room);

            client.controllers.SceneController.contentGroup.getChildren().setAll(auctionRoomRoot);
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
}