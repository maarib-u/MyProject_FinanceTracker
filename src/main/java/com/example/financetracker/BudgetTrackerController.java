// ✅ ❌ ⚠️ ❗
package com.example.financetracker;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class BudgetTrackerController {
    @FXML private TextField budgetField;  // input field for budget
    @FXML private Label budgetStatusLabel; // label for budget status
    @FXML private ProgressBar budgetProgressBar;  // progress bar for visualising budget progress
    @FXML private Label progressLabel;  // label to show progress percentage
    @FXML private Label insightLabel;  // label for insights and warnings

    private int userId;  // stores the user id

    public void setUserId(int userId) {
        this.userId = userId;
        loadBudget();  // load budget from database
        updateProgress();  // update progress bar
    }

    @FXML
    public void initialize() {
        // initial setup for the UI components
    }

    @FXML
    private void handleSetBudget() {
        String budgetText = budgetField.getText();  // get budget text

        if (budgetText.isEmpty() || !isValidBudget(budgetText)) {
            showAlert("❌ Error", "Please enter a valid budget amount.");  // show error if budget is invalid
            return;
        }

        double budget = Double.parseDouble(budgetText);  // convert budget to double

        if (budget <= 0) {
            showAlert("❌ Error", "Budget must be a positive value.");  // show error if budget is less than or equal to zero
            return;
        }

        // confirm budget update with user
        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.setTitle("Confirm Budget Update");
        confirmationAlert.setHeaderText("Are you sure you want to update your budget?");
        confirmationAlert.setContentText("Your new budget will be set to: £" + budget);
        Optional<ButtonType> result = confirmationAlert.showAndWait();  // get user's response

        if (result.isPresent() && result.get() == ButtonType.OK) {
            // update the budget in the database
            try (Connection conn = DatabaseManager.getConnection()) {
                PreparedStatement pstmt = conn.prepareStatement(
                        "MERGE INTO Budgets AS target " +
                                "USING (SELECT ? AS user_id, ? AS budget) AS source " +
                                "ON target.user_id = source.user_id " +
                                "WHEN MATCHED THEN UPDATE SET monthly_budget = source.budget " +
                                "WHEN NOT MATCHED THEN INSERT (user_id, monthly_budget) VALUES (source.user_id, source.budget);");

                pstmt.setInt(1, userId);
                pstmt.setDouble(2, budget);
                pstmt.executeUpdate();

                showAlert("✅ Success", "Budget updated successfully!");  // show success message
                loadBudget();  // reload the budget
                updateProgress();  // update progress bar
            } catch (SQLException e) {
                showAlert("❌ Error", "Database error occurred.");  // show error if database operation fails
                e.printStackTrace();
            }
        }
    }

    private void loadBudget() {
        // load the budget from the database
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement("SELECT monthly_budget FROM Budgets WHERE user_id = ?");
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                double budget = rs.getDouble("monthly_budget");  // get the budget value
                budgetField.setText(String.valueOf(budget));  // set the budget in the text field
                budgetStatusLabel.setText("Monthly Budget: £" + budget);  // display the budget status
            } else {
                budgetField.setText("");
                budgetStatusLabel.setText("⚠️ No budget set.");  // show warning if no budget is set
            }
        } catch (SQLException e) {
            showAlert("❌ Error", "Could not load budget.");  // show error if loading fails
            e.printStackTrace();
        }
    }

    private void updateProgress() {
        // update the progress bar based on spending
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT SUM(amount) as total_spent FROM Expenses WHERE user_id = ?");
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            double totalSpent = 0;
            if (rs.next()) {
                totalSpent = rs.getDouble("total_spent");  // get total spent amount
            }

            double totalBudget = Double.parseDouble(budgetField.getText());  // get the total budget value
            if (totalBudget > 0) {
                double progress = totalSpent / totalBudget;  // calculate progress
                double progressPercentage = progress * 100;

                // update the progress bar and label with current values
                budgetProgressBar.setProgress(progress);
                progressLabel.setText(String.format("Progress: £%.2f / £%.2f (%.2f%%)", totalSpent, totalBudget, progressPercentage));

                // update progress bar color based on budget status
                if (progress < 0.6) {
                    // Green - Below 60% of the budget
                    insightLabel.setText("");
                    budgetProgressBar.getStyleClass().remove("approaching");
                    budgetProgressBar.getStyleClass().remove("exceeded");
                    budgetProgressBar.getStyleClass().remove("warning");
                } else if (progress < 0.8) {
                    // Orange - Between 60% and 80% of the budget
                    insightLabel.setText("⚠️ You are approaching your budget limit.");
                    budgetProgressBar.getStyleClass().remove("exceeded");
                    budgetProgressBar.getStyleClass().add("approaching");
                } else if (progress < 1) {
                    // Red - Between 80% and 100% of the budget
                    insightLabel.setText("❌ You are very close to your budget limit!");
                    budgetProgressBar.getStyleClass().remove("approaching");
                    budgetProgressBar.getStyleClass().add("warning");
                } else if (progress == 1) {
                    // Exactly at the budget limit (100%)
                    insightLabel.setText("❌ You have reached your budget limit!");
                    budgetProgressBar.getStyleClass().remove("warning");
                    budgetProgressBar.getStyleClass().add("exceeded");
                } else {
                    // Exceeded 100% of the budget
                    insightLabel.setText("❌ You have exceeded your budget!");
                    budgetProgressBar.getStyleClass().remove("warning");
                    budgetProgressBar.getStyleClass().add("exceeded");
                }
            }
        } catch (SQLException e) {
            showAlert("❌ Error", "Could not update progress.");  // show error if progress update fails
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBack() {
        // navigate back to the main screen
        SceneController sceneController = new SceneController(SceneManager.getPrimaryStage());
        sceneController.switchToSceneWithUser("main.fxml", userId);
    }

    private void showAlert(String title, String message) {
        // display an alert message to the user
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }

    private boolean isValidBudget(String budgetText) {
        // check if the budget input is a valid number
        try {
            Double.parseDouble(budgetText);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
