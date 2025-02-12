package com.example.financetracker;

import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BudgetTrackerController {
    @FXML private TextField budgetField;
    @FXML private Label budgetStatusLabel;
    @FXML private PieChart budgetChart;

    private int userId;

    public void setUserId(int userId) {
        this.userId = userId;
        System.out.println("DEBUG: BudgetTrackerController - User ID set to " + userId);
        loadBudget();
        updateChart(); // ✅ Ensure chart updates when loading budget
    }

    @FXML
    public void initialize() {
        System.out.println("DEBUG: BudgetTrackerController initialized.");
    }

    @FXML
    private void handleSetBudget() {
        try (Connection conn = DatabaseManager.getConnection()) {
            double budget = Double.parseDouble(budgetField.getText());

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
            updateChart(); // ✅ Refresh PieChart after setting budget
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
                budgetField.setText(String.valueOf(rs.getDouble("monthly_budget")));
                budgetStatusLabel.setText("✅ Budget loaded successfully.");
            } else {
                budgetField.setText("");
                budgetStatusLabel.setText("⚠️ No budget set.");
            }
        } catch (SQLException e) {
            showAlert("❌ Error", "Could not load budget.");
            e.printStackTrace();
        }
    }

    private void updateChart() {
        budgetChart.getData().clear();

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
                PieChart.Data spentData = new PieChart.Data("Spent", totalSpent);
                PieChart.Data remainingData = new PieChart.Data("Remaining", totalBudget - totalSpent);

                budgetChart.getData().addAll(spentData, remainingData);

                // ✅ Directly set Pie Chart colors manually
                spentData.getNode().setStyle("-fx-pie-color: #e74c3c;");  // Red for spent
                remainingData.getNode().setStyle("-fx-pie-color: #2ecc71;");  // Green for remaining
            }
        } catch (SQLException | NumberFormatException e) {
            showAlert("❌ Error", "Could not update budget chart.");
            e.printStackTrace();
        }

        budgetChart.setLabelsVisible(true); // ✅ Ensure labels are visible
        budgetChart.setLegendVisible(true); // ✅ Ensure legend is visible

        budgetChart.layout(); // ✅ Force UI refresh
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
