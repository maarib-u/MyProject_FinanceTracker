package com.example.financetracker;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.PieChart;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class ExpenseTrackerController {
    @FXML private TextField amountField;
    @FXML private ComboBox<String> categoryBox;
    @FXML private ListView<String> expenseList;
    @FXML private PieChart expenseChart;
    @FXML private TextField customCategoryField;  // Text field for adding a custom category
    @FXML private Button addCategoryButton;  // Button to add custom category
    @FXML private Button deleteCategoryButton;  // Button to delete custom category
    @FXML private Button deleteExpenseButton;  // Button to delete selected expense
    @FXML private Label totalAmountLabel; // Label to show total expenses

    private int userId;

    public void setUserId(int userId) {
        this.userId = userId;
        System.out.println("DEBUG: ExpenseTracker - User ID set to " + userId);
        loadCategories();
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
    private void handleDeleteExpense() {
        String selectedExpense = expenseList.getSelectionModel().getSelectedItem();

        if (selectedExpense == null) {
            showAlert("❌ Error", "Please select an expense to delete.");
            return;
        }

        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.setTitle("Confirm Deletion");
        confirmationAlert.setHeaderText("Are you sure you want to delete the expense?");
        confirmationAlert.setContentText("This action cannot be undone.");
        Optional<ButtonType> result = confirmationAlert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                         "DELETE FROM Expenses WHERE user_id = ? AND amount = ? AND category = ? AND date = ?")) {

                String[] expenseDetails = selectedExpense.split(" - ");
                double amount = Double.parseDouble(expenseDetails[0].replace("£", ""));
                String category = expenseDetails[1];
                String date = expenseDetails[2];

                pstmt.setInt(1, userId);
                pstmt.setDouble(2, amount);
                pstmt.setString(3, category);
                pstmt.setString(4, date);

                int rowsAffected = pstmt.executeUpdate();

                if (rowsAffected > 0) {
                    showAlert("✅ Success", "Expense deleted successfully!");
                    loadExpenses();
                    updateChart();
                } else {
                    showAlert("❌ Error", "Could not delete expense.");
                }
            } catch (SQLException e) {
                showAlert("❌ Error", "Failed to delete expense.");
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleBack() {
        SceneController sceneController = new SceneController(SceneManager.getPrimaryStage());
        sceneController.switchToSceneWithUser("main.fxml", userId);
    }

    @FXML
    private void handleAddCustomCategory() {
        String customCategory = customCategoryField.getText().trim();

        if (customCategory.isEmpty()) {
            showAlert("❌ Error", "Please enter a valid category.");
            return;
        }

        // Save custom category to the database
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO UserCategories (user_id, category_name) VALUES (?, ?)")) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, customCategory);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            showAlert("❌ Error", "Could not save custom category.");
            e.printStackTrace();
            return;
        }

        // Add the custom category to the ComboBox
        categoryBox.getItems().add(customCategory);
        customCategoryField.clear();
        showAlert("✅ Success", "Custom category added: " + customCategory);
    }

    @FXML
    private void handleDeleteCustomCategory() {
        String selectedCategory = categoryBox.getValue();

        if (selectedCategory == null || !categoryBox.getItems().contains(selectedCategory)) {
            showAlert("❌ Error", "Please select a category to delete.");
            return;
        }

        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.setTitle("Confirm Deletion");
        confirmationAlert.setHeaderText("Are you sure you want to delete this custom category?");
        confirmationAlert.setContentText("This action cannot be undone.");
        Optional<ButtonType> result = confirmationAlert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                         "DELETE FROM UserCategories WHERE category_name = ? AND user_id = ?")) {

                pstmt.setString(1, selectedCategory);
                pstmt.setInt(2, userId);
                int rowsAffected = pstmt.executeUpdate();

                if (rowsAffected > 0) {
                    categoryBox.getItems().remove(selectedCategory);  // Remove from ComboBox
                    showAlert("✅ Success", "Category deleted: " + selectedCategory);
                } else {
                    showAlert("❌ Error", "Could not delete category.");
                }
            } catch (SQLException e) {
                showAlert("❌ Error", "Failed to delete category from the database.");
                e.printStackTrace();
            }
        }
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

            double totalExpenses = 0;  // Variable to hold total expenses

            while (rs.next()) {
                String category = rs.getString("category");
                double totalAmount = rs.getDouble("total");
                totalExpenses += totalAmount;

                PieChart.Data data = new PieChart.Data(category, totalAmount);
                expenseChart.getData().add(data);

                // Label each slice with the amount and percentage
                double percentage = (totalAmount / totalExpenses) * 100;
                data.setName(String.format("%s - £%.2f (%.2f%%)", category, totalAmount, percentage));
            }

            // Display total expenses
            totalAmountLabel.setText("Total Expenses: £" + totalExpenses);
        } catch (SQLException e) {
            showAlert("❌ Error", "Could not update chart.");
        }
    }

    private void loadCategories() {
        categoryBox.getItems().clear();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT category_name FROM UserCategories WHERE user_id = ?")) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                categoryBox.getItems().add(rs.getString("category_name"));
            }

            if (categoryBox.getItems().isEmpty()) {
                categoryBox.getItems().addAll("Food", "Transport", "Rent", "Shopping", "Other");
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
