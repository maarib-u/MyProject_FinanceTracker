package com.example.financetracker;

import javafx.stage.Stage;

public class SceneManager {
    // static variable to hold the primary stage (main window) of the application
    private static Stage primaryStage;

    // sets the primary stage (main window) to be used throughout the application
    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;  // assign the provided stage to the static primaryStage
    }

    // returns the current primary stage (main window) of the application
    public static Stage getPrimaryStage() {
        return primaryStage;  // returns the currently set primaryStage
    }
}
