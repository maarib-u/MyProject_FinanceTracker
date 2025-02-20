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
import java.text.DecimalFormat;
import java.util.Optional;

public class BudgetTrackerController {
    @FXML private TextField budgetField;  // Input field for budget
    @FXML private Label budgetStatusLabel; // Label for budget status
    @FXML private ProgressBar budgetProgressBar;  // Progress bar for visualising budget progress
    @FXML private Label progressLabel;  // Label to show progress percentage
    @FXML private Label insightLabel;  // Label for insights and warnings

    private int userId;  // Stores the user ID

    // Formatter to ensure correct British currency format (£X.00)
    private static final DecimalFormat currencyFormat = new DecimalFormat("£#,##0.00");

    public void setUserId(int userId) {
        this.userId = userId;
        loadBudget();  // Load budget from database
        updateProgress();  // Update progress bar
    }

    @FXML
    public void initialize() {
        // Initial setup for the UI components
    }

    @FXML
    private void handleSetBudget() {
        String budgetText = budgetField.getText();

        if (budgetText.isEmpty() || !isValidBudget(budgetText)) {
            showAlert("❌ Error", "Please enter a valid budget amount.");
            return;
        }

        double budget = Double.parseDouble(budgetText);

        if (budget <= 0) {
            showAlert("❌ Error", "Budget must be a positive value.");
            return;
        }

        // Confirm budget update with the user
        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.setTitle("Confirm Budget Update");
        confirmationAlert.setHeaderText("Are you sure you want to update your budget?");
        confirmationAlert.setContentText("Your new budget will be set to: " + currencyFormat.format(budget));
        Optional<ButtonType> result = confirmationAlert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
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

                showAlert("✅ Success", "Budget updated successfully!");
                loadBudget();
                updateProgress();
            } catch (SQLException e) {
                showAlert("❌ Error", "Database error occurred.");
                e.printStackTrace();
            }
        }
    }

    private void loadBudget() {
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement("SELECT monthly_budget FROM Budgets WHERE user_id = ?");
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                double budget = rs.getDouble("monthly_budget");
                budgetField.setText(String.format("%.2f", budget));
                budgetStatusLabel.setText("Monthly Budget: " + currencyFormat.format(budget));
            } else {
                budgetField.setText("");
                budgetStatusLabel.setText("⚠️ No budget set.");
            }
        } catch (SQLException e) {
            showAlert("❌ Error", "Could not load budget.");
            e.printStackTrace();
        }
    }

    private void updateProgress() {
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT SUM(amount) as total_spent FROM Expenses WHERE user_id = ?");
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            double totalSpent = 0;
            if (rs.next()) {
                totalSpent = rs.getDouble("total_spent");
            }

            double totalBudget = Double.parseDouble(budgetField.getText());
            if (totalBudget > 0) {
                double progress = totalSpent / totalBudget;
                double progressPercentage = progress * 100;

                // Format values properly in £X.00 format
                String formattedSpent = currencyFormat.format(totalSpent);
                String formattedBudget = currencyFormat.format(totalBudget);

                // Update UI components
                budgetProgressBar.setProgress(progress);
                progressLabel.setText(String.format("Progress: %s / %s (%.2f%%)", formattedSpent, formattedBudget, progressPercentage));

                // Color-based budget warnings
                if (progress < 0.6) {
                    insightLabel.setText("");
                    budgetProgressBar.getStyleClass().removeAll("approaching", "exceeded", "warning");
                } else if (progress < 0.8) {
                    insightLabel.setText("⚠️ You are approaching your budget limit.");
                    budgetProgressBar.getStyleClass().removeAll("exceeded", "warning");
                    budgetProgressBar.getStyleClass().add("approaching");
                } else if (progress < 1) {
                    insightLabel.setText("❌ You are very close to your budget limit!");
                    budgetProgressBar.getStyleClass().remove("approaching");
                    budgetProgressBar.getStyleClass().add("warning");
                } else if (progress == 1) {
                    insightLabel.setText("❌ You have reached your budget limit!");
                    budgetProgressBar.getStyleClass().remove("warning");
                    budgetProgressBar.getStyleClass().add("exceeded");
                } else {
                    insightLabel.setText("❌ You have exceeded your budget!");
                    budgetProgressBar.getStyleClass().remove("warning");
                    budgetProgressBar.getStyleClass().add("exceeded");
                }
            }
        } catch (SQLException e) {
            showAlert("❌ Error", "Could not update progress.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBack() {
        SceneController sceneController = new SceneController(SceneManager.getPrimaryStage());
        sceneController.switchToSceneWithUser("main.fxml", userId);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }

    private boolean isValidBudget(String budgetText) {
        try {
            Double.parseDouble(budgetText);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
