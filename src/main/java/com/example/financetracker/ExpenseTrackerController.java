package com.example.financetracker;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.scene.chart.PieChart;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ExpenseTrackerController {
    @FXML private TextField amountField;
    @FXML private ComboBox<String> categoryBox;
    @FXML private ListView<String> expenseList;
    @FXML private PieChart expenseChart;

    private int userId;

    public void setUserId(int userId) {
        this.userId = userId;
        System.out.println("DEBUG: ExpenseTracker - User ID set to " + userId);
        loadCategories(); // ✅ Load categories dynamically
        loadExpenses();
        updateChart();
    }

    @FXML
    private void handleAddExpense() {
        String category = categoryBox.getValue();
        String amountText = amountField.getText();

        if (category == null || amountText.isEmpty()) {
            showAlert("❌ Error", "Please enter an amount and select a category.");
            return;
        }

        try {
            double amount = Double.parseDouble(amountText);
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                         "INSERT INTO Expenses (user_id, amount, category, date) VALUES (?, ?, ?, GETDATE())")) {

                pstmt.setInt(1, userId);
                pstmt.setDouble(2, amount);
                pstmt.setString(3, category);
                pstmt.executeUpdate();

                showAlert("✅ Success", "Expense added successfully!");
                loadExpenses();
                updateChart();
            }
        } catch (NumberFormatException e) {
            showAlert("❌ Error", "Invalid amount entered.");
        } catch (SQLException e) {
            showAlert("❌ Database Error", "Could not add expense.");
            e.printStackTrace();
        }
    }

    @FXML
    public void handleShowExpenses() { // ✅ Ensure correct method signature
        System.out.println("DEBUG: handleShowExpenses triggered.");
        loadExpenses();
        updateChart();
        showAlert("✅ Success", "Expenses updated.");
    }

    @FXML
    private void handleBack() {
        SceneController sceneController = new SceneController(SceneManager.getPrimaryStage());
        sceneController.switchToSceneWithUser("main.fxml", userId);
    }

    private void loadExpenses() {
        expenseList.getItems().clear();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT amount, category, date FROM Expenses WHERE user_id = ? ORDER BY date DESC")) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String expenseEntry = "£" + rs.getDouble("amount") + " - " + rs.getString("category") + " - " + rs.getString("date");
                expenseList.getItems().add(expenseEntry);
            }
        } catch (SQLException e) {
            showAlert("❌ Error", "Could not fetch expenses.");
        }
    }

    private void updateChart() {
        expenseChart.getData().clear();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT category, SUM(amount) as total FROM Expenses WHERE user_id = ? GROUP BY category")) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                PieChart.Data data = new PieChart.Data(rs.getString("category"), rs.getDouble("total"));
                expenseChart.getData().add(data);
            }
        } catch (SQLException e) {
            showAlert("❌ Error", "Could not update chart.");
        }
    }

    private void loadCategories() { // ✅ Load categories dynamically from DB
        categoryBox.getItems().clear();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT DISTINCT category FROM Expenses")) {

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                categoryBox.getItems().add(rs.getString("category"));
            }

            if (categoryBox.getItems().isEmpty()) {
                categoryBox.getItems().addAll("Food", "Transport", "Rent", "Shopping", "Other"); // Default categories
            }
        } catch (SQLException e) {
            showAlert("❌ Error", "Could not load categories.");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.show();
    }
}
