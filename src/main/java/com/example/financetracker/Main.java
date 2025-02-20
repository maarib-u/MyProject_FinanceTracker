package com.example.financetracker;

import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        // save the primary stage (main window) in SceneManager for later use
        SceneManager.setPrimaryStage(primaryStage);

        // initialize the scene controller and switch to the login screen
        SceneController sceneController = new SceneController(primaryStage);
        sceneController.switchToScene("login.fxml");

        // set the title of the main window (primary stage)
        primaryStage.setTitle("Finance Tracker");

        // show the primary stage (main window)
        primaryStage.show();

        primaryStage.setResizable(false); // Disables maximise button

    }

    // main method to launch the application
    public static void main(String[] args) {
        launch(args);  // launch the JavaFX application
    }
}
