package client.controllers.bidderController;

import common.Request;
import common.models.Room;
import client.controllers.Session;
import client.controllers.SceneController;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.application.Platform;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class WonProductsController {

    @FXML private TableView<Room> wonProductsTable;
    @FXML private TableColumn<Room, String> productIdColumn;
    @FXML private TableColumn<Room, String> productNameColumn;
    @FXML private TableColumn<Room, String> sellerNameColumn;
    @FXML private TableColumn<Room, String> winningPriceColumn;
    @FXML private TableColumn<Room, String> wonTimeColumn;

    @FXML
    public void initialize() {
        setupTableColumns();
        loadMyWonProducts();
    }

    private void setupTableColumns() {
        productIdColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getProductId()));

        productNameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getProductName()));

        sellerNameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getSellerName()));

        winningPriceColumn.setCellValueFactory(cellData -> {
            long price = cellData.getValue().getWinPrice();
            return new SimpleStringProperty(formatMoney(price));
        });
        wonTimeColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getEndTime()));
    }

    private void loadMyWonProducts() {
        String myUsername = Session.getInstance().getCurrentUsername();
        Request getWonProductsRequest = new Request("GET_MY_WON_PRODUCTS", myUsername);

        Session.getInstance().sendRequest(getWonProductsRequest,
                response -> {
                    Platform.runLater(() -> {
                        if ("GET_MY_WON_PRODUCTS_SUCCESS".equals(response.getAction())) {
                            List<Room> myWonRooms = (List<Room>) response.getData();
                            // Đổ data vào bảng
                            wonProductsTable.setItems(FXCollections.observableArrayList(myWonRooms));
                        } else {
                            System.out.println("Lỗi khi tải danh sách: " + response.getData());
                        }
                    });
                }
        );
    }

    private String formatMoney(long amount) {
        Locale vietnamLocale = new Locale("vi", "VN");
        NumberFormat vnFormat = NumberFormat.getInstance(vietnamLocale);
        return vnFormat.format(amount) + " VNĐ";
    }

    @FXML
    public void handleRefreshList(ActionEvent event) {
        loadMyWonProducts();
    }

    @FXML
    public void handleBackToDashboard(ActionEvent event) {
        SceneController.switchScene("/client/views/bidder/BidderDashboard.fxml");
    }
}