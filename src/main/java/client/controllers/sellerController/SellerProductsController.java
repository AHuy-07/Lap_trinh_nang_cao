package client.controllers.sellerController;

import client.controllers.SceneController;
import client.controllers.Session;
import common.Request;
import common.models.Product;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

public class SellerProductsController {
    @FXML private TableView<Product> tableMyProducts;
    @FXML private TableColumn<Product, String> colProductId;
    @FXML private TableColumn<Product, String> colProductName;
    @FXML private TableColumn<Product, String> colProductType;
    @FXML private TableColumn<Product, Integer> colProductStatus;
    @FXML private TableColumn<Product, String> colProductWinPrice;

    private ObservableList<Product> myProductsList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colProductId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colProductName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colProductType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colProductWinPrice.setCellValueFactory(new PropertyValueFactory<>("winPrice"));

        colProductStatus.setCellValueFactory(new PropertyValueFactory<>("isSold"));

        colProductStatus.setCellFactory(col -> new TableCell<Product, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    switch (item) {
                        case 0:
                            setText("Sẵn sàng");
                            setStyle("-fx-text-fill: #7f8c8d; -fx-font-weight: bold;"); // Màu xám chuyên nghiệp
                            break;
                        case 1:
                            setText("Đang đấu giá");
                            setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold;"); // Màu cam nổi bật
                            break;
                        case 2:
                            setText("Đã bán");
                            setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;"); // Màu xanh lá "thành công"
                            break;
                        default:
                            setText("Không xác định");
                            setStyle("-fx-text-fill: black;");
                    }
                }
            }
        });

        tableMyProducts.setItems(myProductsList);
        loadMyProducts();
    }

    public void loadMyProducts() {
        Request req = new Request("GET_MY_PRODUCTS", null);
        Session.getInstance().sendRequest(req, response -> {
            if (response.getAction().equals("GET_MY_PRODUCTS_SUCCESS")) {
                List<Product> list = (List<Product>)response.getData();
                if (!list.isEmpty()) {
                    Platform.runLater(() -> {
                        myProductsList.setAll(list);
                    });
                }
            }
        });
    }

    @FXML
    public void switchToAddProduct(ActionEvent event) {
        SceneController.switchScene("/client/views/seller/AddProducts.fxml");
    }

    @FXML
    public void switchToDashboard(ActionEvent event) {
        SceneController.switchScene("/client/views/seller/SellerDashboard.fxml");
    }

}
