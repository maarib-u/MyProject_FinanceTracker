package com.example.financetracker;

import javafx.fxml.FXML;
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

    private int userId;

    public void setUserId(int userId) {
        this.userId = userId;
        System.out.println("DEBUG: User ID set to " + userId);

        if (budgetStatusLabel == null) {
            System.err.println("❌ ERROR: budgetStatusLabel is NULL! Initialization failed.");
        } else {
            loadBudget();
        }
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

    @FXML
    private void handleBack() {
        SceneController sceneController = new SceneController(SceneManager.getPrimaryStage());
        sceneController.switchToScene("main.fxml");
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.show();
    }
}
