package client.controllers;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;

public class SceneController {
    public static void switchScene(Stage primaryStage, String fxmlPath) throws Exception {
        Parent root = FXMLLoader.load(SceneController.class.getResource(fxmlPath));
        primaryStage.getScene().setRoot(root);
    }
}
