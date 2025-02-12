package com.example.financetracker;

import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BudgetTrackerController {
    @FXML private TextField budgetField;
    @FXML private Label budgetStatusLabel;
    @FXML private ProgressBar budgetProgressBar;
    @FXML private PieChart budgetChart;
    @FXML private VBox alertBox;

    private int userId;
    private double currentExpenses = 0;
    private double monthlyBudget = 0;

    public void setUserId(int userId) {
        this.userId = userId;
        System.out.println("DEBUG: BudgetTracker - User ID set to " + userId);

        loadBudget();
        loadExpenses();
    }

    @FXML
    public void initialize() {
        System.out.println("DEBUG: BudgetTrackerController initialized.");
    }

    @FXML
    private void handleSetBudget() {
        try (Connection conn = DatabaseManager.getConnection()) {
            monthlyBudget = Double.parseDouble(budgetField.getText());

            PreparedStatement pstmt = conn.prepareStatement(
                    "MERGE INTO Budgets AS target " +
                            "USING (SELECT ? AS user_id, ? AS budget) AS source " +
                            "ON target.user_id = source.user_id " +
                            "WHEN MATCHED THEN UPDATE SET monthly_budget = source.budget " +
                            "WHEN NOT MATCHED THEN INSERT (user_id, monthly_budget) VALUES (source.user_id, source.budget);");

            pstmt.setInt(1, userId);
            pstmt.setDouble(2, monthlyBudget);
            pstmt.executeUpdate();

            showAlert("✅ Success", "Budget updated successfully!");
            loadBudget();
            loadExpenses();
        } catch (NumberFormatException e) {
            showAlert("❌ Error", "Enter a valid budget amount.");
        } catch (SQLException e) {
            showAlert("❌ Error", "Database error occurred.");
            e.printStackTrace();
        }
    }

    private void loadBudget() {
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement("SELECT monthly_budget FROM Budgets WHERE user_id = ?");
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                monthlyBudget = rs.getDouble("monthly_budget");
                budgetField.setText(String.valueOf(monthlyBudget));
                budgetStatusLabel.setText("✅ Budget loaded successfully.");
            } else {
                budgetField.setText("");
                budgetStatusLabel.setText("⚠️ No budget set.");
            }
        } catch (SQLException e) {
            showAlert("❌ Error", "Could not load budget.");
            e.printStackTrace();
        }
        updateProgressBar();
    }

    private void loadExpenses() {
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT SUM(amount) as total_expenses FROM Expenses WHERE user_id = ?");
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                currentExpenses = rs.getDouble("total_expenses");
            } else {
                currentExpenses = 0;
            }
        } catch (SQLException e) {
            showAlert("❌ Error", "Could not load expenses.");
            e.printStackTrace();
        }
        updateProgressBar();
        updateAlerts();
        updateChart();
    }

    private void updateProgressBar() {
        if (monthlyBudget > 0) {
            double progress = currentExpenses / monthlyBudget;
            budgetProgressBar.setProgress(progress);
        }
    }

    private void updateAlerts() {
        alertBox.getChildren().clear();

        double budgetUsage = (currentExpenses / monthlyBudget) * 100;
        if (budgetUsage >= 80 && budgetUsage < 100) {
            alertBox.getChildren().add(new Label("⚠️ Warning: You have used over 80% of your budget!"));
        } else if (budgetUsage >= 100) {
            alertBox.getChildren().add(new Label("❌ Alert: You have exceeded your budget!"));
        }
    }

    private void updateChart() {
        budgetChart.getData().clear();
        boolean hasData = false;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT category, SUM(amount) as total FROM Expenses WHERE user_id = ? GROUP BY category")) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                double amount = rs.getDouble("total");
                if (amount > 0) { // ensure only non-zero values are added
                    PieChart.Data data = new PieChart.Data(rs.getString("category"), amount);
                    budgetChart.getData().add(data);
                    hasData = true;
                }
            }

        } catch (SQLException e) {
            showAlert("❌ Error", "Could not update chart.");
        }

        // ensure the chart is visible even if no data is available
        if (!hasData) {
            budgetChart.getData().add(new PieChart.Data("No Expenses", 1));
        }
    }

    @FXML
    private void handleBack() {
        System.out.println("DEBUG: Returning to main with user ID: " + userId);
        SceneController sceneController = new SceneController(SceneManager.getPrimaryStage());
        sceneController.switchToSceneWithUser("main.fxml", userId);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.show();
    }
}
