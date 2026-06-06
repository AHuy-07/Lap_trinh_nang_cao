package client.controllers.bidderController;

import client.controllers.SceneController;
import client.controllers.Session;
import common.Request;
import common.models.BidTransaction;
import common.models.Room;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class AuctionRoomController {
    private static final Logger logger = LoggerFactory.getLogger(AuctionRoomController.class);
    @FXML private Label username;
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
    @FXML private TextField autoBidLimitField;
    @FXML private Button btnToggleAutoBid;

    @FXML private LineChart<Number, Number> priceChart;
    @FXML private NumberAxis xAxis;
    private XYChart.Series<Number, Number> priceSeries;

    // Biến lưu mốc thời gian bắt đầu phòng (Mốc 0)
    private long auctionStartTime;

    private boolean isAutoBidOn = false;

    private javafx.animation.Timeline countdownTimeline;
    private long remainingSeconds;

    private Room currentRoom;
    private long currentPrice;
    private long bidStep;

    @FXML
    public void initialize() {
        priceSeries = new XYChart.Series<>();
        if (priceChart != null) {
            priceChart.getData().add(priceSeries);
        }
        username.setText(Session.getInstance().getCurrentUsername());
    }

    public void initRoom(Room room) {
        this.currentRoom = room;
        this.currentPrice = Math.max(room.getStartingPrice(), room.getWinPrice());
        this.bidStep = Room.calculateDefaultBidStep(room.getStartingPrice());

        joinRoom();
        registerRealtimeBidCallback();
    }

    private void joinRoom() {
        statusLabel.setText("Đang vào phòng...");
        countdownLabel.setText("--:--:--");

        Session.getInstance().sendRequest(
                new Request("JOIN_ROOM", currentRoom.getRoomId()),
                response -> {
                    if ("JOIN_ROOM_SUCCESS".equals(response.getAction())) {
                        Object[] data = (Object[]) response.getData();
                        Room latestRoom = (Room) data[0];

                        currentRoom = latestRoom;
                        currentPrice = Math.max(latestRoom.getStartingPrice(), latestRoom.getWinPrice());
                        bidStep = Room.calculateDefaultBidStep(latestRoom.getStartingPrice());

                        String[] autoBidStatus = new String[]{currentRoom.getRoomId(), Session.getInstance().getCurrentUsername()};

                        Session.getInstance().sendRequest(
                                new Request("CHECK_AUTO_BID_STATUS", autoBidStatus),
                                res -> {
                                    if ("CHECK_AUTO_BID_TRUE".equals(res.getAction())) {
                                        logger.info("Lay thong tin thanh cong");
                                        long savedMaxPrice = ((Number) res.getData()).longValue();
                                        Platform.runLater(() -> {
                                            isAutoBidOn = true;
                                            autoBidLimitField.setText(String.valueOf(savedMaxPrice));
                                            autoBidLimitField.setDisable(true);
                                            btnToggleAutoBid.setText("Hủy Auto-Bid");
                                            btnToggleAutoBid.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-cursor: hand;");
                                        });
                                    } else {
                                        Platform.runLater(() -> {
                                            isAutoBidOn = false;
                                            autoBidLimitField.setDisable(false);
                                            btnToggleAutoBid.setText("Kích hoạt Auto-Bid");
                                            btnToggleAutoBid.setStyle("-fx-background-color: #4f46e5; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-cursor: hand;");
                                        });
                                    }
                                    Platform.runLater(() -> {
                                        renderRoomInfo();
                                        appendBidHistory("Bạn đã vào phòng đấu giá " + latestRoom.getRoomName());
                                    });
                                }
                        );
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

        auctionStartTime = parseTimeToMillis(currentRoom.getBeginTime());

        if (currentRoom.getEndTime() != null && !currentRoom.getEndTime().isBlank()) {
            initCountdown(currentRoom.getBeginTime(), currentRoom.getEndTime());
        }

        setupBidHistory(currentRoom);
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
                    bidAmountField.setDisable(true);
                    long secondsUntilStart = java.time.Duration.between(now, beginTime).toSeconds();
                    remainingSeconds = secondsUntilStart;

                    countdownTimeline = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), event -> {
                        remainingSeconds--;
                        if (remainingSeconds <= 0) {
                            countdownTimeline.stop();
                            statusLabel.setText("Phiên đấu giá đã bắt đầu!");
                            bidAmountField.setDisable(false);
                            initCountdown(null, endTimeStr);
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

            bidAmountField.setDisable(false);
            remainingSeconds = java.time.Duration.between(now, endTime).toSeconds();
            countdownTimeline = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), event -> {
                remainingSeconds--;

                if (remainingSeconds <= 0) {
                    countdownTimeline.stop();
                    countdownLabel.setText("00:00:00");
                    statusLabel.setText("Phiên đấu giá đã kết thúc");
                    bidAmountField.setDisable(true);
                } else {
                    long hours = remainingSeconds / 3600;
                    long minutes = (remainingSeconds % 3600) / 60;
                    long seconds = remainingSeconds % 60;
                    countdownLabel.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
                    if (statusLabel.getText().equals("Đang vào phòng...")) {
                        statusLabel.setText("Đã vào phòng. Bạn có thể bắt đầu đấu giá.");
                    }
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
                    Object[] data = (Object[]) response.getData();
                    BidTransaction latestBid = (BidTransaction) data[0];

                    currentPrice = latestBid.getBidAmount();
                    currentPriceLabel.setText(formatMoney(currentPrice));
                    winnerLabel.setText(latestBid.getBidderUsername());

                    appendBidHistory(latestBid.getBidderUsername() + " vừa đặt giá " + formatMoney(latestBid.getBidAmount()));

                    long nextRecommendedPrice = currentPrice + bidStep;
                    bidAmountField.setText(String.valueOf(nextRecommendedPrice));
                    statusLabel.setText("Có lượt đặt giá mới");

                    double elapsedMinutes = calculateElapsedMinutes(latestBid.getBidTime());
                    priceSeries.getData().add(new XYChart.Data<>(elapsedMinutes, latestBid.getBidAmount()));

                } else if ("AUCTION_ENDED_WITH_WINNER".equals(response.getAction())) {
                    if (countdownTimeline != null) countdownTimeline.stop();
                    countdownLabel.setText("00:00:00");
                    bidAmountField.setDisable(true);
                    statusLabel.setText("Phiên đấu giá đã kết thúc");

                    String[] winInfo = (String[])response.getData();
                    String winner = winInfo[0];
                    long winnerPrice = Long.parseLong(winInfo[1]);
                    String roomName = winInfo[2];
                    String myUsername = Session.getInstance().getCurrentUsername();

                    showAlert(myUsername.equals(winner), roomName, winner, winnerPrice);
                } else if ("AUCTION_ENDED_WITH_NO_BID".equals(response.getAction())) {
                    if (countdownTimeline != null) countdownTimeline.stop();
                    countdownLabel.setText("00:00:00");
                    bidAmountField.setDisable(true);
                    statusLabel.setText("Phiên đấu giá đã kết thúc");
                } else if ("END_TIME_EXTENDED".equals(response.getAction())) {
                    String newEndTime = (String) response.getData();
                    currentRoom.setEndTime(newEndTime);
                    appendBidHistory("Thời gian đấu giá được gia hạn thêm 1 phút!");
                    statusLabel.setText("Phiên đấu giá được gia hạn thêm 1 phút!");
                    initCountdown(null, newEndTime);
                } else if ("AUTO_BID_DISABLED_NO_MONEY".equals(response.getAction())
                        || ("AUTO_BID_DISABLED_LIMIT_MONEY".equals(response.getAction()))) {
                    Room bidDisabledRoom = (Room)response.getData();
                    if (currentRoom.getRoomId().equals(bidDisabledRoom.getRoomId())) {
                        isAutoBidOn = false;
                        btnToggleAutoBid.setText("Kích hoạt Auto-Bid");
                        btnToggleAutoBid.setStyle("-fx-background-color: #4f46e5; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-cursor: hand;");
                        autoBidLimitField.setDisable(false);
                        statusLabel.setText("Hệ thống tự động ngắt Auto-bid");
                    }
                }
            });
        });
    }

    private void showAlert(boolean isWinner, String roomName, String winner, long winnerPrice) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Kết quả phiên đấu giá");
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().add(ButtonType.OK);

        VBox root = new VBox(12);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20, 30, 20, 30));

        Text titleText = new Text();
        titleText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));

        Text contentText = new Text();
        contentText.setFont(Font.font("Segoe UI", 14));
        contentText.setWrappingWidth(360);

        if (isWinner) {
            titleText.setText("🎉 CHÚC MỪNG BẠN ĐÃ THẮNG CUỘC! 🎉");
            titleText.setFill(Color.web("#2ecc71"));
            contentText.setText("Bạn đã sở hữu thành công sản phẩm tại phòng '" + roomName
                    + "' với mức giá chốt hạ là " + formatMoney(winnerPrice) + " VNĐ.\n\n"
                    + "Số tiền trên đã được hệ thống tự động kết chuyển thanh toán.");
            dialogPane.setStyle("-fx-border-color: #2ecc71; -fx-border-width: 2px; -fx-background-color: #fafdfb;");
        } else {
            titleText.setText("🎯 Phiên đấu giá khép lại");
            titleText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
            titleText.setFill(Color.web("#34495e"));
            contentText.setText("Phòng '" + roomName + "' đã kết thúc thành công.\n"
                    + "Người mua chốt đỉnh: [" + winner + "] với giá " + formatMoney(winnerPrice) + " VNĐ.");
            contentText.setWrappingWidth(320);
            dialogPane.setStyle("-fx-border-color: #bdc3c7; -fx-border-width: 1px; -fx-background-color: #fffffff;");
        }

        root.getChildren().addAll(titleText, contentText);
        dialogPane.setContent(root);
        dialogPane.lookupButton(ButtonType.OK).setStyle("-fx-background-color: #34495e; -fx-text-fill: white; -fx-cursor: hand;");

        dialog.showAndWait();
    }

    @FXML private void handleRecommendedBid() { setRecommendedBid(1); }
    @FXML private void handleBidStep2() { setRecommendedBid(2); }
    @FXML private void handleBidStep5() { setRecommendedBid(5); }

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

            BidTransaction transaction = new BidTransaction(null, currentRoom.getRoomId(), Session.getInstance().getCurrentUsername(), bidAmount, null);
            statusLabel.setText("Đang gửi giá đấu...");

            Session.getInstance().sendRequest(
                    new Request("PLACE_BID", transaction),
                    response -> Platform.runLater(() -> {
                        if ("PLACE_BID_SUCCESS".equals(response.getAction())) {
                            statusLabel.setText("Đặt giá thành công, đang cập nhật realtime...");
                        } else if ("PLACE_BID_FAIL".equals(response.getAction())) {
                            statusLabel.setText(String.valueOf(response.getData()));
                        }
                    })
            );
        } catch (NumberFormatException e) {
            statusLabel.setText("Giá nhập không hợp lệ");
        }
    }

    @FXML
    private void handleBackToDashboard() {
        if (countdownTimeline != null) countdownTimeline.stop();
        Session.getInstance().clearRealtimeBidCallback();

        Session.getInstance().sendRequest(
                new Request("LEAVE_ROOM", currentRoom.getRoomId()),
                response -> Platform.runLater(() -> SceneController.switchScene("/client/views/bidder/BidderDashboard.fxml"))
        );
    }

    @FXML
    private void handleToggleAutoBid(ActionEvent event) {
        if (!isAutoBidOn) {
            String limitedPriceText = autoBidLimitField.getText().trim();
            if (limitedPriceText.isEmpty()) {
                statusLabel.setText("Vui lòng nhập giá tối đa muốn Auto-bid!");
                return;
            }

            try {
                long maxPrice = Long.parseLong(limitedPriceText);
                if ((maxPrice <= currentPrice + bidStep)) {
                    statusLabel.setText("Giá tối đa phải lớn hơn giá tiếp theo 1 bước giá!");
                    return;
                }

                long createAt = System.currentTimeMillis() - 1770000000000L;

                String[] data = new String[]{currentRoom.getRoomId(), Long.toString(maxPrice), "ON", Long.toString(createAt)};
                statusLabel.setText("Đang đăng ký Auto-bid...");
                Request req = new Request("TOGGLE_AUTO_BID", data);
                Session.getInstance().sendRequest(req, response -> Platform.runLater(() -> {
                    if ("TOGGLE_AUTO_BID_SUCCESS".equals(response.getAction())) {
                        isAutoBidOn = true;
                        btnToggleAutoBid.setText("Hủy Auto-bid");
                        btnToggleAutoBid.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-cursor: hand;");
                        autoBidLimitField.setDisable(true);
                        statusLabel.setText("Đã kích hoạt chế độ đấu giá tự động thành công!");
                    } else {
                        statusLabel.setText("Bật Auto-bid thất bại: " + response.getData());
                    }
                }));
            } catch (NumberFormatException e) {
                statusLabel.setText("Số tiền giới hạn nhập vào không hợp lệ");
            }
        } else {
            String[] data = new String[]{currentRoom.getRoomId(), "0", "OFF"};
            statusLabel.setText("Đang hủy Auto-bid");
            Request req = new Request("TOGGLE_AUTO_BID", data);
            Session.getInstance().sendRequest(req, response -> Platform.runLater(() -> {
                if ("TOGGLE_AUTO_BID_SUCCESS".equals(response.getAction())) {
                    isAutoBidOn = false;
                    btnToggleAutoBid.setText("Kích hoạt Auto-Bid");
                    btnToggleAutoBid.setStyle("-fx-background-color: #4f46e5; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-cursor: hand;");
                    autoBidLimitField.setDisable(false);
                    statusLabel.setText("Đã tắt chế độ đấu giá tự động.");
                }
            }));
        }
    }

    private void appendBidHistory(String message) {
        bidHistoryArea.appendText(message + "\n");
    }

    private String formatMoney(long amount) {
        Locale vietnamLocale = new Locale("vi", "VN");
        NumberFormat vnFormat = NumberFormat.getInstance(vietnamLocale);
        return vnFormat.format(amount) + " VNĐ";
    }

    //chuyển lịch sử bid lên chart
    private void setupBidHistory(Room room) {
        Request req = new Request("GET_BID_HISTORY", room);
        Session.getInstance().sendRequest(req, response -> {
            if ("GET_BID_HISTORY_SUCCESS".equals(response.getAction())) {
                List<BidTransaction> list = (List<BidTransaction>) response.getData();
                Platform.runLater(() -> {
                    priceSeries.getData().clear();
                    priceSeries.getData().add(new XYChart.Data<>(0, room.getStartingPrice()));

                    if (list != null && !list.isEmpty()) {
                        List<XYChart.Data<Number, Number>> chartData = list.stream()
                                .map(bid -> new XYChart.Data<Number, Number>(
                                        calculateElapsedMinutes(bid.getBidTime()), bid.getBidAmount()))
                                .collect(Collectors.toList());

                        Collections.reverse(chartData);
                        priceSeries.getData().addAll(chartData);
                    }
                });
            }
        });
    }

    private double calculateElapsedMinutes(String bidTimeStr) {
        long bidMillis = parseTimeToMillis(bidTimeStr);
        double elapsed = (bidMillis - auctionStartTime) / 60000.0;
        return Math.max(0, elapsed);
    }

    private long parseTimeToMillis(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) return System.currentTimeMillis();
        if (timeStr.matches("\\d+")) return Long.parseLong(timeStr);

        try {
            // CHO JAVA TỰ ĐỌC ĐỊNH DẠNG MẶC ĐỊNH (ISO-8601) TRƯỚC để tránh bị giới hạn bởi cái format
            return java.time.LocalDateTime.parse(timeStr)
                    .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (Exception e) {
            String[] formats = {
                    "yyyy-MM-dd HH:mm:ss.S", "yyyy-MM-dd HH:mm:ss.SSS", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm",
                    "dd/MM/yyyy HH:mm:ss", "dd-MM-yyyy HH:mm:ss", "HH:mm:ss", "HH:mm"
            };
            // NẾU KHÔNG PHẢI CHUẨN ISO, MỚI THỬ CÁC ĐỊNH DẠNG KHÁC
            for (String format : formats) {
                try {
                    java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern(format);
                    if (format.startsWith("HH")) {
                        java.time.LocalTime time = java.time.LocalTime.parse(timeStr, formatter);
                        return java.time.LocalDateTime.of(java.time.LocalDateTime.now().toLocalDate(), time)
                                .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                    } else {
                        return java.time.LocalDateTime.parse(timeStr, formatter)
                                .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                    }
                } catch (Exception ignored) {}
            }
        }

        return System.currentTimeMillis();
    }
}