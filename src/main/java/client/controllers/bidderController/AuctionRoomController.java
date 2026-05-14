package client.controllers.bidderController;

import client.controllers.SceneController;
import client.controllers.Session;
import common.Request;
import common.models.BidTransaction;
import common.models.Room;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class AuctionRoomController {
    @FXML private Label roomNameLabel;
    @FXML private Label roomIdLabel;
    @FXML private Label sellerNameLabel;
    @FXML private Label startingPriceLabel;
    @FXML private Label currentPriceLabel;
    @FXML private Label bidStepLabel;
    @FXML private Label winnerLabel;
    @FXML private Label statusLabel;
    @FXML private TextField bidAmountField;
    @FXML private TextArea bidHistoryArea;

    private Room currentRoom;
    private long currentPrice;
    private long bidStep;

    public void initRoom(Room room) {
        this.currentRoom = room;
        this.currentPrice = Math.max(room.getStartingPrice(), room.getWinPrice());
        this.bidStep = Room.calculateDefaultBidStep(room.getStartingPrice());

        /*
        renderRoomInfo();
        registerRealtimeBidCallback();
        joinRoom();
        */
        joinRoom();
        registerRealtimeBidCallback();
    }

    private void renderRoomInfo() {
        roomNameLabel.setText(currentRoom.getRoomName());
        roomIdLabel.setText(currentRoom.getRoomId());
        sellerNameLabel.setText(currentRoom.getSellerName());
        startingPriceLabel.setText(formatMoney(currentRoom.getStartingPrice()));
        currentPriceLabel.setText(formatMoney(currentPrice));
        bidStepLabel.setText(formatMoney(bidStep));

        String winner = currentRoom.getWinnerUsername();

        if (winner == null || winner.isBlank()) {
            winner = "Chưa có";
        }

        winnerLabel.setText(winner);

        long recommendedPrice = currentPrice + bidStep;
        bidAmountField.setText(String.valueOf(recommendedPrice));
    }

    private void registerRealtimeBidCallback() {
        Session.getInstance().setRealtimeBidCallback(response -> {
            if ("NEW_BID".equals(response.getAction())) {
                BidTransaction latestBid = (BidTransaction) response.getData();

                currentPrice = latestBid.getBidAmount();
                currentPriceLabel.setText(formatMoney(currentPrice));
                winnerLabel.setText(latestBid.getBidderUsername());

                appendBidHistory(
                        latestBid.getBidderUsername()
                                + " vừa đặt giá "
                                + formatMoney(latestBid.getBidAmount())
                );

                long nextRecommendedPrice = currentPrice + bidStep;
                bidAmountField.setText(String.valueOf(nextRecommendedPrice));
                statusLabel.setText("Có lượt đặt giá mới");
            } else if ("AUCTION_ENDED".equals(response.getAction())) {
                statusLabel.setText("Phiên đấu giá đã kết thúc");
                bidAmountField.setDisable(true);
            }
        });
    }

    private void joinRoom() {
        statusLabel.setText("Đang vào phòng đấu giá...");

        Session.getInstance().sendRequest(
                new Request("JOIN_ROOM", currentRoom.getRoomId()),
                response -> {
                    if ("JOIN_ROOM_SUCCESS".equals(response.getAction())) {
                        Room latestRoom = (Room) response.getData();

                        currentRoom = latestRoom;
                        currentPrice = Math.max(latestRoom.getStartingPrice(), latestRoom.getWinPrice());
                        bidStep = Room.calculateDefaultBidStep(latestRoom.getStartingPrice());

                        renderRoomInfo();
                        appendBidHistory("Bạn đã vào phòng đấu giá " + latestRoom.getRoomName());
                        statusLabel.setText("Đã vào phòng. Bạn có thể bắt đầu đấu giá.");
                    } else if ("JOIN_ROOM_FAIL".equals(response.getAction())) {
                        statusLabel.setText(String.valueOf(response.getData()));
                    }
                }
        );
    }

    @FXML
    private void handleRecommendedBid() {
        setRecommendedBid(1);
    }

    @FXML
    private void handleBidStep2() {
        setRecommendedBid(2);
    }

    @FXML
    private void handleBidStep5() {
        setRecommendedBid(5);
    }

    private void setRecommendedBid(int multiplier) {
        long recommendedPrice = currentPrice + bidStep * multiplier;
        bidAmountField.setText(String.valueOf(recommendedPrice));
        statusLabel.setText("Đã chọn giá khuyến nghị +" + multiplier + " bước");
    }

    @FXML
    private void handlePlaceBid() {
        try {
            long bidAmount = Long.parseLong(bidAmountField.getText().trim());
            long minimumPrice = currentPrice + bidStep;

            if (bidAmount < minimumPrice) {
                statusLabel.setText("Giá tối thiểu phải là " + formatMoney(minimumPrice));
                return;
            }

            BidTransaction transaction = new BidTransaction(
                    null,
                    currentRoom.getRoomId(),
                    Session.getInstance().getCurrentUsername(),
                    bidAmount,
                    null
            );

            statusLabel.setText("Đang gửi giá đấu...");

            Session.getInstance().sendRequest(
                    new Request("PLACE_BID", transaction),
                    response -> {
                        if ("PLACE_BID_SUCCESS".equals(response.getAction())) {
                            statusLabel.setText("Đặt giá thành công, đang cập nhật realtime...");
                        } else if ("PLACE_BID_FAIL".equals(response.getAction())) {
                            statusLabel.setText(String.valueOf(response.getData()));
                        }
                    }
            );
        } catch (NumberFormatException e) {
            statusLabel.setText("Giá nhập không hợp lệ");
        }
    }

    @FXML
    private void handleBackToDashboard() {
        Session.getInstance().clearRealtimeBidCallback();

        Session.getInstance().sendRequest(
                new Request("LEAVE_ROOM", currentRoom.getRoomId()),
                response -> SceneController.switchScene("/client/views/bidder/BidderDashboard.fxml")
        );
    }

    private void appendBidHistory(String message) {
        bidHistoryArea.appendText(message + "\n");
    }

    private String formatMoney(long amount) {
        return String.format("%,d", amount);
    }
}