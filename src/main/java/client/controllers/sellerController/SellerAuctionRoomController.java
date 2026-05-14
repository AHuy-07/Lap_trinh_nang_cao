package client.controllers.sellerController;

import client.controllers.SceneController;
import client.controllers.Session;
import common.Request;
import common.models.BidTransaction;
import common.models.Room;
import javafx.application.Platform;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;

public class SellerAuctionRoomController {
    @FXML private Label roomNameLabel;
    @FXML private Label roomIdLabel;
    @FXML private Label sellerNameLabel;
    @FXML private Label startingPriceLabel;
    @FXML private Label currentPriceLabel;
    @FXML private Label winnerLabel;
    @FXML private Label statusLabel;
    @FXML private TableView<BidTransaction> tableBidHistory;
    @FXML private TableColumn<BidTransaction, String> bidTimeCol;
    @FXML private TableColumn<BidTransaction, String> bidNameCol;
    @FXML private TableColumn<BidTransaction, Number> bidAmountCol;

    private Room currentRoom;
    private long currentPrice;

    private ObservableList<BidTransaction> bidHistoryList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTableCols();
    }

    public void initRoom(Room room) {
        this.currentRoom = room;
        this.currentPrice = Math.max(room.getStartingPrice(), room.getWinPrice());
        joinRoom();
    }

    private void setupTableCols() {
        bidTimeCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getBidTime()));
        bidNameCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getBidderUsername()));
        bidAmountCol.setCellValueFactory(cellData ->
                new SimpleLongProperty(cellData.getValue().getBidAmount()));

        tableBidHistory.setItems(bidHistoryList);

    }

    private void joinRoom() {
        Session.getInstance().sendRequest(
                new Request("JOIN_ROOM", currentRoom.getRoomId()),
                response -> {
                    Room room = (Room) response.getData();
                    currentRoom = room;
                    currentPrice = Math.max(room.getStartingPrice(), room.getWinPrice());

                    renderRoomInfo();
                }
        );
    }

    private void renderRoomInfo() {
        roomNameLabel.setText(currentRoom.getRoomName());
        roomIdLabel.setText(currentRoom.getRoomId());
        sellerNameLabel.setText(currentRoom.getSellerName());
        startingPriceLabel.setText(Long.toString(currentRoom.getStartingPrice()));
        currentPriceLabel.setText(Long.toString(currentPrice));
        if (currentRoom.getStatus().equals("ACTIVE")) {
            statusLabel.setText("Đang diễn ra");
            statusLabel.setStyle("-fx-text-fill: #2ecc71;");
        } else if (currentRoom.getStatus().equals("CLOSED")) {
            statusLabel.setText("Đã kết thúc");
        }
        statusLabel.setText(currentRoom.getStatus());
        String winner = currentRoom.getWinnerUsername();

        if (winner == null || winner.isEmpty()) {
            winner = "Chưa có";
        }

        winnerLabel.setText(winner);
        setupBidHistory(currentRoom);
    }

    private void setupBidHistory(Room room) {
        Request req = new Request("GET_BID_HISTORY", room);
        Session.getInstance().sendRequest(req, response -> {
            String action = response.getAction();
            if (action.equals("GET_BID_HISTORY_SUCCESS")) {
                List<BidTransaction> list = (List<BidTransaction>) response.getData();
                if (!list.isEmpty()) {
                    Platform.runLater(() -> {
                        bidHistoryList.setAll(list);
                    });
                }
            }
        });
        registerRealtimeBidHistory();
    }

    private void registerRealtimeBidHistory() {
        Session.getInstance().setRealtimeBidCallback(response -> {
            if ("NEW_BID".equals(response.getAction())) {
                BidTransaction bid = (BidTransaction) response.getData();

                Platform.runLater(() -> {
                    bidHistoryList.add(0, bid);
                    currentPriceLabel.setText(Long.toString(bid.getBidAmount()));
                    winnerLabel.setText(bid.getBidderUsername());
                });
            }
        });
    }

    @FXML
    public void switchToDashboard(ActionEvent event) {
        SceneController.switchScene("/client/views/seller/SellerDashboard.fxml");
    }
}
