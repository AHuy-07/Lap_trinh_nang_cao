package client.controllers;

import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import java.io.IOException;

public class SceneController {
    private static final double BASE_WIDTH = 1024;
    private static final double BASE_HEIGHT = 768;
    public static Group contentGroup;

    public static void init(Group group) {
        contentGroup = group;
    }

    public static void switchScene(String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(SceneController.class.getResource(fxmlPath));

            if (root instanceof Region) {
                ((Region) root).setPrefSize(BASE_WIDTH, BASE_HEIGHT);
            }

            contentGroup.getChildren().setAll(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
