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
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
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
import java.util.List;
import java.util.Locale;

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
    @FXML private AreaChart<Number, Number> priceChart;
    @FXML private NumberAxis xAxis;

    private XYChart.Series<Number, Number> priceSeries;
    //biến lưu thời điểm bắt đầu đấu giá;
    private long auctionStartTime;

    private Room currentRoom;
    private long currentPrice;
    private Timeline countdownTimeline;
    private long remainingSeconds;

    private ObservableList<BidTransaction> bidHistoryList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        //khởi tạo chart
        priceSeries = new XYChart.Series<>();
        priceChart.getData().add(priceSeries);

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


        startingPriceLabel.setText(formatMoney(currentRoom.getStartingPrice()));
        currentPriceLabel.setText(formatMoney(currentPrice));

        // bộ đếm bắt đầu hoạt động cho chart
        auctionStartTime = System.currentTimeMillis();
        priceSeries.getData().add(new XYChart.Data<>(0, currentPrice));

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

        // Kích hoạt đồng hồ
        if (currentRoom.getEndTime() != null && !currentRoom.getEndTime().isBlank()) {
            initCountdown(currentRoom.getBeginTime(), currentRoom.getEndTime());
        }

        setupBidHistory(currentRoom);
    }

    // Đồng hồ hiển thị thời gian còn lại
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

            // Nếu có đồng hồ cũ đang chạy (đề phòng lỗi lặp luồng), dừng nó lại trước
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

            // Đang trong khung giờ đấu giá
            remainingSeconds = Duration.between(now, endTime).toSeconds();

            countdownTimeline = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), event -> {
                remainingSeconds--;
                if (remainingSeconds <= 0) {
                    countdownTimeline.stop(); // Dừng đồng hồ khi về 0
                    countdownLabel.setText("00:00:00");
                    statusLabel.setText("Đã kết thúc");
                    statusLabel.setStyle("-fx-text-fill: #7f8c8d;");
                    // Gửi request tự động khóa phòng
                    //
                    //
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

                    currentPriceLabel.setText(formatMoney(bid.getBidAmount()));

                    winnerLabel.setText(bid.getBidderUsername());

                    //cập nhật cho biểu đồ
                    long elapsedMilis = System.currentTimeMillis() - auctionStartTime;
                    double elapsedMinutes = elapsedMilis / 60000.0;

                    if (elapsedMinutes >= xAxis.getUpperBound()) {
                        xAxis.setUpperBound(xAxis.getUpperBound() + 10);
                    }

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
            }
        });
    }

    private void showAlert(int type,  String winner, long winnerPrice, String roomName) {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }

        countdownLabel.setText("00:00:00");
        statusLabel.setText("Phiên đấu giá đã kết thúc");
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

        if (type == 0) {
            titleText.setText("🎉 CHÚC MỪNG BẠN ĐÃ BÁN ĐƯỢC SẢN PHẨM! 🎉");
            titleText.setFill(Color.web("#2ecc71")); // Màu xanh lá

            contentText.setText("Phòng [" + roomName + "] đã bán thành công sản phẩm\n"
                    + "cho [" + winner +  "] với mức giá chốt hạ là " + formatMoney(winnerPrice) + ".\n\n"
                    + "Số tiền trên đã được hệ thống tự động thanh toán vào tài khoản bạn.");

            // Style thêm cho hộp thoại
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
    }

    @FXML
    public void switchToDashboard(ActionEvent event) {
        // Dừng đồng hồ đếm ngược
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }

        Session.getInstance().clearRealtimeBidCallback();

        SceneController.switchScene("/client/views/seller/SellerDashboard.fxml");
    }

    private String formatMoney(long amount) {
        Locale vietnamLocale = new Locale("vi", "VN");
        NumberFormat vnFormat = NumberFormat.getInstance(vietnamLocale);
        return vnFormat.format(amount) + " VNĐ";
    }

}