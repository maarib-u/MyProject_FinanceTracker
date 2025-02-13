package com.example.financetracker;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class MainController {
    @FXML private Button budgetButton;    // button to open the budget tracker screen
    @FXML private Button expenseButton;   // button to open the expense tracker screen
    @FXML private Button currencyButton;  // button to open the currency converter screen
    @FXML private Button logoutButton;    // button to log out of the application

    private int userId; // stores the user ID for this session

    // sets the user ID when logged in
    public void setUserId(int userId) {
        this.userId = userId;
        System.out.println("DEBUG: User ID set to " + userId); // print the user ID for debugging purposes
    }

    // open the budget tracker screen
    @FXML
    private void openBudgetTracker() {
        // create a new scene controller and switch to the budget tracker scene with the user ID
        SceneController sceneController = new SceneController(SceneManager.getPrimaryStage());
        sceneController.switchToSceneWithUser("budget_tracker.fxml", userId);
    }

    // open the expense tracker screen
    @FXML
    private void openExpenseTracker() {
        // create a new scene controller and switch to the expense tracker scene with the user ID
        SceneController sceneController = new SceneController(SceneManager.getPrimaryStage());
        sceneController.switchToSceneWithUser("expense_tracker.fxml", userId);
    }

    // open the currency converter screen
    @FXML
    private void openCurrencyConverter() {
        // create a new scene controller and switch to the currency converter scene with the user ID
        SceneController sceneController = new SceneController(SceneManager.getPrimaryStage());
        sceneController.switchToSceneWithUser("currency_converter.fxml", userId);
    }

    // handle user logout and return to the login screen
    @FXML
    private void handleLogout() {
        // create a new scene controller and switch to the login screen
        SceneController sceneController = new SceneController(SceneManager.getPrimaryStage());
        sceneController.switchToScene("login.fxml");
    }
}
