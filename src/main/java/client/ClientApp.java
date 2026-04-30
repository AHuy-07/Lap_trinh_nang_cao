package client;

import client.controllers.SceneController;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class ClientApp extends Application {
    private static final double BASE_WIDTH = 600;
    private static final double BASE_HEIGHT = 400;
    public void start(Stage primaryStage) throws Exception {


        // Lấy phần AnchorPane của bên file fxml về
        AnchorPane ui = FXMLLoader.load(getClass().getResource("/client/views/Login.fxml"));

        // Set up trước Width và Height của cửa sổ khi chạy chương trình
        ui.setPrefWidth(BASE_WIDTH);
        ui.setPrefHeight(BASE_HEIGHT);

        /*
        - Tạo một Group để bao trọn tất cả vật con ở trong
        - Tác dụng của Group: Giúp scale mọi thứ ở trong khi kích thước thay đổi
         */
        Group group = new Group(ui);

        /*
        ** LƯU Ý: Cần thiết có dòng này
        * Giúp khởi tạo Group trong sceneController, từ đó mới chuyển scene được
         */
        SceneController.init(group);

        /*
        - Bọc StackPane ở ngoài Group
        - Tác dụng: StackPane giúp mọi thứ luôn ở giữa
         */
        StackPane root = new StackPane(group);

        // Khởi tạo scene, đây là cái sẽ chỉ đạo mọi thứ
        Scene scene = new Scene(root, BASE_WIDTH, BASE_HEIGHT);

        // Listener cập nhật scale khi kích thước scene thay đổi
        // Định nghĩa hàm phương thức scaler
        ChangeListener<Number> scaler = (observableValue, number, t1) -> {
            double scaleX = scene.getWidth() / BASE_WIDTH;
            double scaleY = scene.getHeight() / BASE_HEIGHT;
            double scale = Math.min(scaleX, scaleY);
            group.setScaleX(scale);
            group.setScaleY(scale);
        };

        // Định nghĩa widthProperty và heightProperty của Scene
        scene.heightProperty().addListener(scaler);
        scene.widthProperty().addListener(scaler);

        primaryStage.setScene(scene);
        primaryStage.setFullScreen(true);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
