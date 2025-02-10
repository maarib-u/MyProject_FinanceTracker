package com.example.financetracker;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class SceneController {
    private final Stage primaryStage;

    public SceneController(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    // Switch to an FXML scene by filename
    public void switchToScene(String fxmlFile) {
        try {
            URL fxmlPath = getClass().getResource("/fxml/" + fxmlFile);
            if (fxmlPath == null) {
                throw new IllegalArgumentException("Cannot find FXML file: " + fxmlFile);
            }
            Parent root = FXMLLoader.load(fxmlPath);
            primaryStage.setScene(new Scene(root));
            primaryStage.show();
        } catch (IOException | IllegalArgumentException e) {
            e.printStackTrace();
            System.err.println("Failed to load scene: " + fxmlFile);
        }
    }
}