package client.controllers;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class SceneController {
    private static final double BASE_WIDTH = 1024;
    private static final double BASE_HEIGHT = 768;
    public static StackPane contentPane;

    public static void init(StackPane pane) {
        contentPane = pane;
    }

    public static void setContent(Parent root) {
        contentPane.getChildren().setAll(root);
    }

    public static void switchScene(String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(SceneController.class.getResource(fxmlPath));
            setContent(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
