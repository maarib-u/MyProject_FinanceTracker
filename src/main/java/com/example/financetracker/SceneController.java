package com.example.financetracker;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class SceneController {
    private final Stage primaryStage;

    public SceneController(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public void switchToScene(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/" + fxmlFile));
            Parent root = loader.load();
            primaryStage.setScene(new Scene(root));
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("❌ Failed to load scene: " + fxmlFile);
        }
    }

    public void switchToSceneWithUser(String fxmlFile, int userId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/" + fxmlFile));
            Parent root = loader.load();
            Object controller = loader.getController();

            // ✅ Ensure user ID is passed to the correct controller
            if (controller instanceof MainController) {
                ((MainController) controller).setUserId(userId);
            } else if (controller instanceof ExpenseTrackerController) {
                ((ExpenseTrackerController) controller).setUserId(userId);
            } else if (controller instanceof BudgetTrackerController) {
                ((BudgetTrackerController) controller).setUserId(userId);
            } else if (controller instanceof CurrencyConverterController) { // ✅ Fix for currency converter
                ((CurrencyConverterController) controller).setUserId(userId);
            }

            primaryStage.setScene(new Scene(root));
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("❌ Failed to load scene: " + fxmlFile);
        }
    }
}
