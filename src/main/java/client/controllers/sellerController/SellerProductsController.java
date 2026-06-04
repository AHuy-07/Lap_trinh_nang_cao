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
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;
import java.util.Optional;

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

        ContextMenu contextMenu = new ContextMenu();
        MenuItem createRoom = new MenuItem("Tạo phòng với sản phẩm");
        MenuItem delete = new MenuItem("Xóa sản phẩm");

        // logic tạo phòng vơi ssản phẩm
        createRoom.setOnAction(event -> {
            Product selected = tableMyProducts.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Session.getInstance().setCurrentProduct(selected);
                SceneController.switchScene("/client/views/seller/CreateRoom.fxml");
            } else {
                System.out.println("Vui lòng chọn 1 sản phẩm trước!");
            }

        });
        // logic xóa sản phẩm
        delete.setOnAction(event -> {
            Product selected = tableMyProducts.getSelectionModel().getSelectedItem();
            if(selected != null){
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle("Xác nhận xóa");
                confirmAlert.setHeaderText("Bạn đang yêu cầu xóa một sản phẩm");
                confirmAlert.setContentText("Bạn có chắc chắn muốn xóa sản phẩm này không?");

                Optional<ButtonType> result = confirmAlert.showAndWait();

                if (result.isPresent() && result.get() == ButtonType.OK) {
                    
                    // 1. Tạo gói tin chứa ID gửi lên Server thay vì gọi DAO trực tiếp
                    String productId = selected.getId();
                    Request req = new Request("DELETE_PRODUCT", productId);
                    
                    // 2. Gửi Request và chờ phản hồi (callback)
                    Session.getInstance().sendRequest(req, response -> {
                        Platform.runLater(() -> {
                            if ("DELETE_PRODUCT_SUCCESS".equals(response.getAction())) {
                                // Nếu DB xóa thành công, mới tiến hành xóa trên giao diện (bảng)
                                tableMyProducts.getItems().remove(selected);
                                
                                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                                successAlert.setHeaderText(null);
                                successAlert.setContentText("Đã xóa sản phẩm thành công!");
                                successAlert.show();
                            } else {
                                // Nếu thất bại (Lỗi hoặc bị cấm xóa do đang trong phòng)
                                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                                errorAlert.setHeaderText(null);
                                errorAlert.setContentText((String) response.getData());
                                errorAlert.show();
                            }
                        });
                    });

                }
            } else {
                Alert warning = new Alert(Alert.AlertType.WARNING, "Vui lòng chọn 1 sản phẩm trước!");
                warning.show();
            }
        });

        contextMenu.getItems().addAll(createRoom, delete);
        tableMyProducts.setRowFactory(tv -> {
            javafx.scene.control.TableRow<Product> row = new javafx.scene.control.TableRow<>();
            row.contextMenuProperty().bind(
                    javafx.beans.binding.Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(contextMenu)
            );
            return row;
        });
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
