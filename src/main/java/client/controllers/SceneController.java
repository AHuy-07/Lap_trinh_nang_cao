package client.controllers;

import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.io.IOException;

public class SceneController {
    public static Group contentGroup;

    public static void init(Group group) {
        contentGroup = group;
    }

    public static void switchScene(String fxmlPath) {
        try {
            AnchorPane root = FXMLLoader.load(SceneController.class.getResource(fxmlPath));
            contentGroup.getChildren().setAll(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
