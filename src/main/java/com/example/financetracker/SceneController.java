package com.example.financetracker;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class SceneController {
    private final Stage primaryStage;

    // constructor: accepts the primary stage (main window) for the application
    public SceneController(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    // switches to a new scene without passing any additional data
    public void switchToScene(String fxmlFile) {
        try {
            // load the FXML file
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/" + fxmlFile));
            Parent root = loader.load();

            // set the new scene on the primary stage
            primaryStage.setScene(new Scene(root));
            primaryStage.show();  // Show the new scene

        } catch (IOException e) {
            // print an error message if the scene fails to load
            e.printStackTrace();
            System.err.println("❌ Failed to load scene: " + fxmlFile);
        }
    }

    // switches to a new scene and passes the user ID to the appropriate controller
    public void switchToSceneWithUser(String fxmlFile, int userId) {
        try {
            // load the FXML file for the requested scene
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/" + fxmlFile));
            Parent root = loader.load();
            Object controller = loader.getController();

            // ensure the user ID is passed to the correct controller based on the scene
            if (controller instanceof MainController) {
                ((MainController) controller).setUserId(userId);  // pass user ID to MainController
            } else if (controller instanceof ExpenseTrackerController) {
                ((ExpenseTrackerController) controller).setUserId(userId);  // pass user ID to ExpenseTrackerController
            } else if (controller instanceof BudgetTrackerController) {
                ((BudgetTrackerController) controller).setUserId(userId);  // pass user ID to BudgetTrackerController
            } else if (controller instanceof CurrencyConverterController) {
                ((CurrencyConverterController) controller).setUserId(userId);  // pass user ID to CurrencyConverterController
            }

            // set the new scene on the primary stage and display it
            primaryStage.setScene(new Scene(root));
            primaryStage.show();  // show the new scene

        } catch (IOException e) {
            // print an error message if the scene fails to load
            e.printStackTrace();
            System.err.println("❌ Failed to load scene: " + fxmlFile);
        }
    }
}
