package com.example.financetracker;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class MainController {
    @FXML private Button budgetButton;
    @FXML private Button expenseButton;
    @FXML private Button currencyButton;
    @FXML private Button logoutButton;

    private int userId;

    public void setUserId(int userId) {
        this.userId = userId;
        System.out.println("DEBUG: User ID set to " + userId); // ✅ Debugging
    }

    @FXML
    private void openBudgetTracker() {
        SceneController sceneController = new SceneController(SceneManager.getPrimaryStage());
        sceneController.switchToSceneWithUser("budget_tracker.fxml", userId);
    }

    @FXML
    private void openExpenseTracker() {
        SceneController sceneController = new SceneController(SceneManager.getPrimaryStage());
        sceneController.switchToSceneWithUser("expense_tracker.fxml", userId);
    }

    @FXML
    private void openCurrencyConverter() {
        SceneController sceneController = new SceneController(SceneManager.getPrimaryStage());
        sceneController.switchToSceneWithUser("currency_converter.fxml", userId);
    }

    @FXML
    private void handleLogout() {
        SceneController sceneController = new SceneController(SceneManager.getPrimaryStage());
        sceneController.switchToScene("login.fxml");
    }
}
