package client.controllers.sellerController;

import client.controllers.SceneController;
import client.controllers.Session;
import common.Request;
import common.models.BidTransaction;
import common.models.Room;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class SellerAuctionRoomController {
    private static final Logger logger = LoggerFactory.getLogger(SellerAuctionRoomController.class);
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
    @FXML private Label countdownLabel;
    @FXML private Label participantCountLabel;

    @FXML private LineChart<Number, Number> priceChart;
    @FXML private NumberAxis xAxis;

    private XYChart.Series<Number, Number> priceSeries;

    // Biến lưu thời điểm bắt đầu đấu giá (Mốc 0)
    private long auctionStartTime;

    private Room currentRoom;
    private long currentPrice;
    private Timeline countdownTimeline;
    private long remainingSeconds;

    private ObservableList<BidTransaction> bidHistoryList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        priceSeries = new XYChart.Series<>();
        if (priceChart != null) {
            priceChart.getData().add(priceSeries);
        }
        setupTableCols();
    }

    public void initRoom(Room room) {
        this.currentRoom = room;
        this.currentPrice = Math.max(room.getStartingPrice(), room.getWinPrice());
        joinRoom();
    }

    private void setupTableCols() {
        bidTimeCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatBidTime(cellData.getValue().getBidTime())));
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
                    Object[] data = (Object[]) response.getData();
                    Room room = (Room) data[0];
                    int participantCount = (Integer) data[1];
                    currentRoom = room;
                    currentPrice = Math.max(room.getStartingPrice(), room.getWinPrice());

                    Platform.runLater(() -> {
                        participantCountLabel.setText(Integer.toString(participantCount));
                        renderRoomInfo();
                    });
                }
        );
    }

    private void renderRoomInfo() {
        roomNameLabel.setText(currentRoom.getRoomName());
        roomIdLabel.setText(currentRoom.getRoomId());
        sellerNameLabel.setText(currentRoom.getSellerName());
        startingPriceLabel.setText(formatMoney(currentRoom.getStartingPrice()));
        currentPriceLabel.setText(formatMoney(currentPrice));

        if (currentRoom.getStatus().equals("ACTIVE")) {
            statusLabel.setText("Đang diễn ra");
            statusLabel.setStyle("-fx-text-fill: #2ecc71;");
        } else if (currentRoom.getStatus().equals("CLOSED")) {
            statusLabel.setText("Đã kết thúc");
            statusLabel.setStyle("-fx-text-fill: #7f8c8d;");
        }  else {
            statusLabel.setText(currentRoom.getStatus());
            statusLabel.setStyle("");
        }

        String winner = currentRoom.getWinnerUsername();
        if (winner == null || winner.isEmpty()) {
            winner = "Chưa có";
        }
        winnerLabel.setText(winner);

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
                statusLabel.setText("Đã kết thúc");
                statusLabel.setStyle("-fx-text-fill: #7f8c8d;");
                return;
            }

            if (countdownTimeline != null) {
                countdownTimeline.stop();
            }

            if (beginTimeStr != null && !beginTimeStr.isBlank()) {
                LocalDateTime beginTime = LocalDateTime.parse(beginTimeStr, formatter);
                if (now.isBefore(beginTime)) {
                    remainingSeconds = Duration.between(now, beginTime).toSeconds();
                    statusLabel.setText("Chưa đến giờ bắt đầu");
                    statusLabel.setStyle("-fx-text-fill: #f39c12;");

                    countdownTimeline = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), event -> {
                        remainingSeconds--;
                        if (remainingSeconds <= 0) {
                            countdownTimeline.stop();
                            statusLabel.setText("Đang diễn ra");
                            statusLabel.setStyle("-fx-text-fill: #2ecc71;");
                            initCountdown(null, endTimeStr);
                        } else {
                            long hours = remainingSeconds / 3600;
                            long minutes = (remainingSeconds % 3600) / 60;
                            long seconds = remainingSeconds % 60;
                            countdownLabel.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
                        }
                    }));
                    countdownTimeline.setCycleCount(Timeline.INDEFINITE);
                    countdownTimeline.play();
                    return;
                }
            }

            remainingSeconds = Duration.between(now, endTime).toSeconds();

            countdownTimeline = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), event -> {
                remainingSeconds--;
                if (remainingSeconds <= 0) {
                    countdownTimeline.stop();
                    countdownLabel.setText("00:00:00");
                    statusLabel.setText("Đã kết thúc");
                    statusLabel.setStyle("-fx-text-fill: #7f8c8d;");
                } else {
                    long hours = remainingSeconds / 3600;
                    long minutes = (remainingSeconds % 3600) / 60;
                    long seconds = remainingSeconds % 60;
                    countdownLabel.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
                }
            }));
            countdownTimeline.setCycleCount(Timeline.INDEFINITE);
            countdownTimeline.play();
        } catch (Exception e) {
            countdownLabel.setText("--:--:--");
            logger.error("Lỗi khi hiển thị thời gian", e);
        }
    }

    private void setupBidHistory(Room room) {
        Request req = new Request("GET_BID_HISTORY", room);
        Session.getInstance().sendRequest(req, response -> {
            if ("GET_BID_HISTORY_SUCCESS".equals(response.getAction())) {
                List<BidTransaction> list = (List<BidTransaction>) response.getData();
                Platform.runLater(() -> {
                    if (list != null && !list.isEmpty()) {
                        bidHistoryList.setAll(list);

                        priceSeries.getData().clear();
                        priceSeries.getData().add(new XYChart.Data<>(0, room.getStartingPrice()));

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
        registerRealtimeBidHistory();
    }

    private void registerRealtimeBidHistory() {
        Session.getInstance().setRealtimeBidCallback(response -> {
            if ("NEW_BID".equals(response.getAction())) {
                Object[] data = (Object[]) response.getData();
                BidTransaction bid = (BidTransaction) data[0];
                int participantCount = (Integer) data[1];

                Platform.runLater(() -> {
                    bidHistoryList.add(0, bid);
                    currentPriceLabel.setText(formatMoney(bid.getBidAmount()));
                    winnerLabel.setText(bid.getBidderUsername());
                    participantCountLabel.setText(Integer.toString(participantCount));

                    double elapsedMinutes = calculateElapsedMinutes(bid.getBidTime());
                    priceSeries.getData().add(new XYChart.Data<>(elapsedMinutes, bid.getBidAmount()));
                });
            } else if ("AUCTION_ENDED_WITH_WINNER".equals(response.getAction())) {
                String[] winInfo = (String[])response.getData();
                String winner = winInfo[0];
                long winnerPrice = Long.parseLong(winInfo[1]);
                String roomName = winInfo[2];
                Platform.runLater(() -> {
                    showAlert(0, winner, winnerPrice, roomName);
                });

            } else if ("AUCTION_ENDED_WITH_NO_BID".equals(response.getAction())) {
                Platform.runLater(() -> {
                    showAlert(1, null, 0l, null);
                });
            } else if ("END_TIME_EXTENDED".equals(response.getAction())) {
                String newEndTime = (String) response.getData();
                currentRoom.setEndTime(newEndTime);
                Platform.runLater(() -> {
                    statusLabel.setText("Phiên đấu giá được gia hạn thêm 1 phút!");
                    statusLabel.setStyle("-fx-text-fill: #f39c12;");
                    initCountdown(null, newEndTime);
                });
            }
        });
    }

    private void showAlert(int type,  String winner, long winnerPrice, String roomName) {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }

        countdownLabel.setText("00:00:00");
        statusLabel.setText("Phiên đấu giá đã kết thúc");

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

        if (type == 0) {
            titleText.setText("🎉 CHÚC MỪNG BẠN ĐÃ BÁN ĐƯỢC SẢN PHẨM! 🎉");
            titleText.setFill(Color.web("#2ecc71"));
            contentText.setText("Phòng [" + roomName + "] đã bán thành công sản phẩm\n"
                    + "cho [" + winner +  "] với mức giá chốt hạ là " + formatMoney(winnerPrice) + ".\n\n"
                    + "Số tiền trên đã được hệ thống tự động thanh toán vào tài khoản bạn.");
            dialogPane.setStyle("-fx-border-color: #2ecc71; -fx-border-width: 2px; -fx-background-color: #fafdfb;");
        } else {
            titleText.setText("🎯 Phiên đấu giá khép lại");
            titleText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
            titleText.setFill(Color.web("#34495e"));
            contentText.setText("Phòng '" + roomName + "' đã kết thúc.\n"
                    + "Chưa có người dùng nào tham gia đấu giá\n"
                    + "Bạn có thể thử tạo phòng khác với mức giá thấp hơn");
            contentText.setWrappingWidth(320);
            dialogPane.setStyle("-fx-border-color: #bdc3c7; -fx-border-width: 1px; -fx-background-color: #fffffff;");
        }

        root.getChildren().addAll(titleText, contentText);
        dialogPane.setContent(root);
        dialogPane.lookupButton(ButtonType.OK).setStyle("-fx-background-color: #34495e; -fx-text-fill: white; -fx-cursor: hand;");

        dialog.showAndWait();
    }

    @FXML
    public void switchToDashboard(ActionEvent event) {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
        Session.getInstance().clearRealtimeBidCallback();
        SceneController.switchScene("/client/views/seller/SellerDashboard.fxml");
    }

    @FXML
    private void forceEndRoom(ActionEvent event) {
        if (currentRoom == null) return;

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION,
                "Bạn có chắc chắn muốn kết thúc phiên đấu giá này ngay lập tức không?",
                ButtonType.YES, ButtonType.NO);
        confirmAlert.setTitle("Xác nhận kết thúc");
        confirmAlert.setHeaderText("Kết thúc phiên chủ động");

        confirmAlert.showAndWait().ifPresent(res -> {
            if (res == ButtonType.YES) {
                Request req = new Request("FORCE_END_ROOM", currentRoom);
                Session.getInstance().sendRequest(req, response -> {
                    if (response.getAction().equals("FORCE_END_ROOM_SUCCESS")) {
                        Platform.runLater(() -> {
                            statusLabel.setText("Đã kết thúc");
                            statusLabel.setStyle("-fx-text-fill: #7f8c8d;");
                        });
                    }
                });
            }
        });
    }

    private String formatMoney(long amount) {
        Locale vietnamLocale = new Locale("vi", "VN");
        NumberFormat vnFormat = NumberFormat.getInstance(vietnamLocale);
        return vnFormat.format(amount) + " VNĐ";
    }

    private String formatBidTime(String bidTimeStr) {
        if (bidTimeStr == null || bidTimeStr.trim().isEmpty()) {
            return "";
        }

        try {
            // Parse ISO-8601 format with nanoseconds (e.g., "2026-06-05T23:04:18.093442600")
            LocalDateTime dateTime = LocalDateTime.parse(bidTimeStr);
            // Format as "dd/MM/yyyy HH:mm:ss"
            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            return dateTime.format(outputFormatter);
        } catch (Exception e) {
            // If parsing fails, return the original string
            logger.error("Lỗi định dạng thời gian: " + bidTimeStr, e);
            return bidTimeStr;
        }
    }

    // --- CÁC HÀM XỬ LÝ THỜI GIAN ---
    private double calculateElapsedMinutes(String bidTimeStr) {
        long bidMillis = parseTimeToMillis(bidTimeStr);
        double elapsed = (bidMillis - auctionStartTime) / 60000.0;
        return Math.max(0, elapsed);
    }

    private long parseTimeToMillis(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) return System.currentTimeMillis();
        if (timeStr.matches("\\d+")) return Long.parseLong(timeStr);

        try {
            // 1. CHO JAVA TỰ ĐỌC ĐỊNH DẠNG MẶC ĐỊNH (ISO-8601) TRƯỚC
            // Định dạng này sẽ xử lý mượt mà chuỗi chứa 9 số nano-giây của bạn
            return java.time.LocalDateTime.parse(timeStr)
                    .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (Exception e) {
            // 2. NẾU KHÔNG PHẢI CHUẨN ISO, MỚI THỬ CÁC ĐỊNH DẠNG KHÁC (VD: Chuẩn Việt Nam)
            String[] formats = {
                    "yyyy-MM-dd HH:mm:ss.S", "yyyy-MM-dd HH:mm:ss.SSS", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm",
                    "dd/MM/yyyy HH:mm:ss", "dd-MM-yyyy HH:mm:ss", "HH:mm:ss", "HH:mm"
            };

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

        // --- NẾU VẪN LỖI THÌ IN RA MÀN HÌNH ---
        System.err.println("❌ LỖI ĐỊNH DẠNG: Không thể đọc được chuỗi thời gian -> [" + timeStr + "]");
        return System.currentTimeMillis();
    }
}