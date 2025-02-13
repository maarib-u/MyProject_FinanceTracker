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
    private void handleBack() {
        SceneController sceneController = new SceneController(SceneManager.getPrimaryStage());
        sceneController.switchToSceneWithUser("main.fxml", userId);
    }

    @FXML
    private void handleAddCustomCategory() {
        String customCategory = customCategoryField.getText().trim();

        // Validate custom category input
        if (customCategory.isEmpty()) {
            showAlert("❌ Error", "Please enter a valid category.");
            return;
        }

        // Add the new category to the ComboBox and clear the input field
        categoryBox.getItems().add(customCategory);
        customCategoryField.clear();
        showAlert("✅ Success", "Custom category added: " + customCategory);
    }

    @FXML
    private void handleDeleteCustomCategory() {
        String selectedCategory = categoryBox.getValue();

        // Check if a custom category is selected
        if (selectedCategory == null || !categoryBox.getItems().contains(selectedCategory)) {
            showAlert("❌ Error", "Please select a category to delete.");
            return;
        }

        // Confirm deletion with the user
        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.setTitle("Confirm Deletion");
        confirmationAlert.setHeaderText("Are you sure you want to delete the category: " + selectedCategory + "?");
        confirmationAlert.setContentText("This action cannot be undone.");
        Optional<ButtonType> result = confirmationAlert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Remove the selected category from the ComboBox
            categoryBox.getItems().remove(selectedCategory);
            showAlert("✅ Success", "Category deleted: " + selectedCategory);
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

            int colorIndex = 0; // Track colors for slices
            while (rs.next()) {
                PieChart.Data data = new PieChart.Data(rs.getString("category"), rs.getDouble("total"));
                expenseChart.getData().add(data);

                // Apply color manually
                String colorClass = "default-color" + (colorIndex % 8); // Rotate through 8 colors
                data.getNode().getStyleClass().add(colorClass);
                colorIndex++;
            }
        } catch (SQLException e) {
            showAlert("❌ Error", "Could not update chart.");
        }
    }

    private void loadCategories() {
        categoryBox.getItems().clear();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT DISTINCT category FROM Expenses")) {

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                categoryBox.getItems().add(rs.getString("category"));
            }

            // If no categories are available, load default ones
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
