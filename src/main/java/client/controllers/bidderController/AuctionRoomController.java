package client.controllers.bidderController;

import client.controllers.SceneController;
import client.controllers.Session;
import common.Request;
import common.models.BidTransaction;
import common.models.Room;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class AuctionRoomController {
    private static final Logger logger = LoggerFactory.getLogger(AuctionRoomController.class);
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
    @FXML private Label countdownLabel;


    @FXML private AreaChart<Number, Number> priceChart;
    @FXML private NumberAxis xAxis;
    private XYChart.Series<Number, Number> priceSeries;
    private long auctionStartTime;

    private javafx.animation.Timeline countdownTimeline;
    private long remainingSeconds;

    private Room currentRoom;
    private long currentPrice;
    private long bidStep;

    @FXML
    public void initialize() {
        // Khởi tạo đồ thị
        priceSeries = new XYChart.Series<>();
        if (priceChart != null) {
            priceChart.getData().add(priceSeries);
        }
    }

    public void initRoom(Room room) {
        this.currentRoom = room;
        this.currentPrice = Math.max(room.getStartingPrice(), room.getWinPrice());
        this.bidStep = Room.calculateDefaultBidStep(room.getStartingPrice());

        joinRoom();
        registerRealtimeBidCallback();
    }

    private void joinRoom() {
        statusLabel.setText("Đang vào phòng đấu giá...");
        countdownLabel.setText("--:--:--");

        Session.getInstance().sendRequest(
                new Request("JOIN_ROOM", currentRoom.getRoomId()),
                response -> {
                    if ("JOIN_ROOM_SUCCESS".equals(response.getAction())) {
                        Room latestRoom = (Room) response.getData();

                        currentRoom = latestRoom;
                        currentPrice = Math.max(latestRoom.getStartingPrice(), latestRoom.getWinPrice());
                        bidStep = Room.calculateDefaultBidStep(latestRoom.getStartingPrice());

                        Platform.runLater(() -> {
                            renderRoomInfo();
                            appendBidHistory("Bạn đã vào phòng đấu giá " + latestRoom.getRoomName());
                        });
                    } else if ("JOIN_ROOM_FAIL".equals(response.getAction())) {
                        Platform.runLater(() -> statusLabel.setText(String.valueOf(response.getData())));
                    }
                }
        );
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

        // khởi tạo chart
        auctionStartTime = System.currentTimeMillis();
        if (priceSeries != null) {
            priceSeries.getData().clear();
            priceSeries.getData().add(new XYChart.Data<>(0, currentPrice));
        }

        if (currentRoom.getEndTime() != null && !currentRoom.getEndTime().isBlank()) {
            initCountdown(currentRoom.getBeginTime(), currentRoom.getEndTime());
        }
    }

    private void initCountdown(String beginTimeStr, String endTimeStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        try {
            LocalDateTime endTime = LocalDateTime.parse(endTimeStr, formatter);
            LocalDateTime now = LocalDateTime.now();

            if (now.isAfter(endTime)) {
                countdownLabel.setText("00:00:00");
                bidAmountField.setDisable(true);
                statusLabel.setText("Phiên đấu giá đã kết thúc");
                return;
            }

            if (countdownTimeline != null) {
                countdownTimeline.stop();
            }

            if (beginTimeStr != null && !beginTimeStr.isBlank()) {
                LocalDateTime beginTime = LocalDateTime.parse(beginTimeStr, formatter);
                if (now.isBefore(beginTime)) {
                    // Khóa đặt giá, đếm ngược đến khi bắt đầu
                    bidAmountField.setDisable(true);
                    long secondsUntilStart = java.time.Duration.between(now, beginTime).toSeconds();
                    remainingSeconds = secondsUntilStart;

                    countdownTimeline = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), event -> {
                        remainingSeconds--;
                        if (remainingSeconds <= 0) {
                            // Đã đến giờ bắt đầu → chuyển sang đếm ngược endTime
                            countdownTimeline.stop();
                            statusLabel.setText("Phiên đấu giá đã bắt đầu!");
                            bidAmountField.setDisable(false);
                            initCountdown(null, endTimeStr); // Không cần beginTime nữa
                        } else {
                            long hours = remainingSeconds / 3600;
                            long minutes = (remainingSeconds % 3600) / 60;
                            long seconds = remainingSeconds % 60;
                            countdownLabel.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
                            statusLabel.setText("Phiên sắp bắt đầu, vui lòng chờ...");
                        }
                    }));

                    countdownTimeline.setCycleCount(Timeline.INDEFINITE);
                    countdownTimeline.play();
                    return;
                }
            }

            // Đang trong khung giờ đấu giá → đếm ngược đến endTime
            bidAmountField.setDisable(false);

            remainingSeconds = java.time.Duration.between(now, endTime).toSeconds();

            countdownTimeline = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), event -> {
                remainingSeconds--;
                if (remainingSeconds <= 0) {
                    countdownTimeline.stop();
                    countdownLabel.setText("00:00:00");
                    statusLabel.setText("Phiên đấu giá đã kết thúc");
                    bidAmountField.setDisable(true); // Khóa ô nhập giá khi hết giờ real-time
                } else {

                    long hours = remainingSeconds / 3600;
                    long minutes = (remainingSeconds % 3600) / 60;
                    long seconds = remainingSeconds % 60;

                    countdownLabel.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
                    statusLabel.setText("Đã vào phòng. Bạn có thể bắt đầu đấu giá.");
                }
            }));

            countdownTimeline.setCycleCount(Timeline.INDEFINITE);
            countdownTimeline.play();
        } catch (Exception e) {
            countdownLabel.setText("--:--:--");
            logger.error("Lỗi khi hiển thị thời gian", e);
        }
    }

    private void registerRealtimeBidCallback() {
        Session.getInstance().setRealtimeBidCallback(response -> {
            Platform.runLater(() -> {
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

                    //cập nhạt đồ thị
                    if (priceSeries != null) {
                        long elapsedMilis = System.currentTimeMillis() - auctionStartTime;
                        double elapsedMinutes = elapsedMilis / 60000.0;

                        if (xAxis != null && elapsedMinutes >= xAxis.getUpperBound()) {
                            xAxis.setUpperBound(xAxis.getUpperBound() + 10);
                        }

                        priceSeries.getData().add(new XYChart.Data<>(elapsedMinutes, latestBid.getBidAmount()));
                    }

                } else if ("AUCTION_ENDED_WITH_WINNER".equals(response.getAction())) {
                    if (countdownTimeline != null) {
                        countdownTimeline.stop();
                    }

                    countdownLabel.setText("00:00:00");
                    bidAmountField.setDisable(true);
                    statusLabel.setText("Phiên đấu giá đã kết thúc");

                    String[] winInfo = (String[])response.getData();
                    String winner = winInfo[0];
                    long winnerPrice = Long.parseLong(winInfo[1]);
                    String roomName = winInfo[2];

                    String myUsername = Session.getInstance().getCurrentUsername();

                    boolean isMeWinner = myUsername.equals(winner);

                    showAlert(isMeWinner, roomName, winner, winnerPrice);
                } else if ("AUCTION_ENDED_WITH_NO_BID".equals(response.getAction())) {
                    if (countdownTimeline != null) {
                        countdownTimeline.stop();
                    }
                    countdownLabel.setText("00:00:00");
                    bidAmountField.setDisable(true);
                    statusLabel.setText("Phiên đấu giá đã kết thúc");
                }
            });
        });
    }

    private void showAlert(boolean isWinner, String roomName, String winner, long winnerPrice) {
        // Cửa sổ hộp thoại mới
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Kết quả phiên đấu giá");

        // DialogPane: Nội dung hộp thoại
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().add(ButtonType.OK); // Bấm đóng kích hoạt

        // Container chính chứa nội dung
        VBox root = new VBox(12);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20, 30, 20, 30));

        // Phần text tiêu đề chính
        Text titleText = new Text();
        titleText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));

        // Phần text nội dung chi tiết
        Text contentText = new Text();
        contentText.setFont(Font.font("Segoe UI", 14));
        contentText.setWrappingWidth(360); // Tự động xuống dòng nếu text quá dài

        if (isWinner) {
            titleText.setText("🎉 CHÚC MỪNG BẠN ĐÃ THẮNG CUỘC! 🎉");
            titleText.setFill(Color.web("#2ecc71")); // Màu xanh lá

            contentText.setText("Bạn đã sở hữu thành công sản phẩm tại phòng '" + roomName
                    + "' với mức giá chốt hạ là " + formatMoney(winnerPrice) + ".\n\n"
                    + "Số tiền trên đã được hệ thống tự động kết chuyển thanh toán.");

            // Style thêm cho hộp thoại
            dialogPane.setStyle("-fx-border-color: #2ecc71; -fx-border-width: 2px; -fx-background-color: #fafdfb;");
        } else {

            titleText.setText("🎯 Phiên đấu giá khép lại");
            titleText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
            titleText.setFill(Color.web("#34495e"));

            contentText.setText("Phòng '" + roomName + "' đã kết thúc thành công.\n"
                    + "Người mua chốt đỉnh: [" + winner + "] với giá " + formatMoney(winnerPrice) + ".");
            contentText.setWrappingWidth(320);

            dialogPane.setStyle("-fx-border-color: #bdc3c7; -fx-border-width: 1px; -fx-background-color: #fffffff;");
        }

        root.getChildren().addAll(titleText, contentText);
        dialogPane.setContent(root);

        // Style lại nút OK
        dialogPane.lookupButton(ButtonType.OK).setStyle("-fx-background-color: #34495e; -fx-text-fill: white; -fx-cursor: hand;");

        dialog.showAndWait();
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
                        Platform.runLater(() -> {
                            if ("PLACE_BID_SUCCESS".equals(response.getAction())) {
                                statusLabel.setText("Đặt giá thành công, đang cập nhật realtime...");
                            } else if ("PLACE_BID_FAIL".equals(response.getAction())) {
                                statusLabel.setText(String.valueOf(response.getData()));
                            }
                        });
                    }
            );
        } catch (NumberFormatException e) {
            statusLabel.setText("Giá nhập không hợp lệ");
        }
    }

    @FXML
    private void handleBackToDashboard() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }

        Session.getInstance().clearRealtimeBidCallback();

        Session.getInstance().sendRequest(
                new Request("LEAVE_ROOM", currentRoom.getRoomId()),
                response -> Platform.runLater(() -> SceneController.switchScene("/client/views/bidder/BidderDashboard.fxml"))
        );
    }



    private void appendBidHistory(String message) {
        bidHistoryArea.appendText(message + "\n");
    }

    //format cho tiền
    private String formatMoney(long amount) {
        Locale vietnamLocale = new Locale("vi", "VN");
        NumberFormat vnFormat = NumberFormat.getInstance(vietnamLocale);
        return vnFormat.format(amount) + " VNĐ";
    }
}