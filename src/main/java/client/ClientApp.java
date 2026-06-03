package client;

import client.controllers.SceneController;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class ClientApp extends Application {
    private static final double BASE_WIDTH = 1280;
    private static final double BASE_HEIGHT = 800;
    public void start(Stage primaryStage) throws Exception {


        // Lấy phần AnchorPane của bên file fxml về
        AnchorPane ui = FXMLLoader.load(getClass().getResource("/client/views/Login.fxml"));

        primaryStage.setFullScreen(true);

        /*
        - Tạo một StackPane cố định kích thước để chứa nội dung
        - StackPane (Region) có layoutBounds ổn định, không thay đổi khi con bên trong relayout
        - Khắc phục lỗi zoom/nhảy màn hình khi sort TableView (Group tính lại bounds từ con)
         */
        StackPane contentPane = new StackPane(ui);
        contentPane.setPrefSize(BASE_WIDTH, BASE_HEIGHT);
        contentPane.setMinSize(BASE_WIDTH, BASE_HEIGHT);
        contentPane.setMaxSize(BASE_WIDTH, BASE_HEIGHT);

        /*
        ** LƯU Ý: Cần thiết có dòng này
        * Giúp khởi tạo contentPane trong sceneController, từ đó mới chuyển scene được
         */
        SceneController.init(contentPane);

        /*
        - Bọc StackPane ở ngoài contentPane
        - Tác dụng: StackPane giúp mọi thứ luôn ở giữa
         */
        StackPane root = new StackPane(contentPane);

        // Khởi tạo scene, đây là cái sẽ chỉ đạo mọi thứ
        Scene scene = new Scene(root, BASE_WIDTH, BASE_HEIGHT);


//        String css = this.getClass().getResource("/style.css").toExternalForm();
//        scene.getStylesheets().add(css);

        // Listener cập nhật scale khi kích thước scene thay đổi
        // Định nghĩa hàm phương thức scaler
        ChangeListener<Number> scaler = (observableValue, number, t1) -> {
            double scaleX = scene.getWidth() / BASE_WIDTH;
            double scaleY = scene.getHeight() / BASE_HEIGHT;
            double scale = Math.min(scaleX, scaleY);
            contentPane.setScaleX(scale);
            contentPane.setScaleY(scale);
        };

        // Định nghĩa widthProperty và heightProperty của Scene
        scene.heightProperty().addListener(scaler);
        scene.widthProperty().addListener(scaler);

        primaryStage.setScene(scene);
        primaryStage.setTitle("Auction System");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
