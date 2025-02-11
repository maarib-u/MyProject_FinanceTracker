package com.example.financetracker;

import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Save the primary stage in SceneManager
        SceneManager.setPrimaryStage(primaryStage);

        // Initialize and show the login screen
        SceneController sceneController = new SceneController(primaryStage);
        sceneController.switchToScene("login.fxml");

        primaryStage.setTitle("Finance Tracker");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
