package com.example.financetracker;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.chart.PieChart;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class ExpenseTrackerController {
    @FXML private TextField amountField;  // field for entering expense amount
    @FXML private ComboBox<String> categoryBox;  // drop-down for selecting category
    @FXML private ListView<String> expenseList;  // list view to display expenses
    @FXML private PieChart expenseChart;  // pie chart to show expense distribution
    @FXML private TextField customCategoryField;  // text field for adding a custom category
    @FXML private Label totalAmountLabel;  // label to show total expenses
    @FXML private Label budgetStatusLabel;  // Label for displaying monthly budget

    private int userId;  // user id to identify the current user

    // method to set user id and load expenses and categories
    public void setUserId(int userId) {
        this.userId = userId;
        System.out.println("DEBUG: ExpenseTracker - User ID set to " + userId);
        loadCategories();  // load categories from database
        loadExpenses();  // load expenses from database
        updateChart();  // update pie chart
    }

    // method to add an expense
    @FXML
    private void handleAddExpense() {
        String category = categoryBox.getValue();  // get selected category
        String amountText = amountField.getText();  // get entered amount

        if (category == null || amountText.isEmpty()) {
            showAlert("❌ Error", "Please enter an amount and select a category.");
            return;
        }

        try {
            double amount = Double.parseDouble(amountText);  // convert amount to double
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                         "INSERT INTO Expenses (user_id, amount, category, date) VALUES (?, ?, ?, GETDATE())")) {

                pstmt.setInt(1, userId);  // set user id in query
                pstmt.setDouble(2, amount);  // set amount in query
                pstmt.setString(3, category);  // set category in query
                pstmt.executeUpdate();  // execute query to add expense

                showAlert("✅ Success", "Expense added successfully!");
                loadExpenses();  // reload the expenses list
                updateChart();  // update the pie chart
            }
        } catch (NumberFormatException e) {
            showAlert("❌ Error", "Invalid amount entered.");
        } catch (SQLException e) {
            showAlert("❌ Database Error", "Could not add expense.");
            e.printStackTrace();
        }
    }

    // method to delete an expense
    @FXML
    private void handleDeleteExpense() {
        String selectedExpense = expenseList.getSelectionModel().getSelectedItem();  // get selected expense

        if (selectedExpense == null) {
            showAlert("❌ Error", "Please select an expense to delete.");
            return;
        }

        // confirmation prompt before deleting the expense
        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.setTitle("Confirm Deletion");
        confirmationAlert.setHeaderText("Are you sure you want to delete the expense?");
        confirmationAlert.setContentText("This action cannot be undone.");
        Optional<ButtonType> result = confirmationAlert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                         "DELETE FROM Expenses WHERE user_id = ? AND amount = ? AND category = ? AND date = ?")) {

                // split the selected expense details to delete
                String[] expenseDetails = selectedExpense.split(" - ");
                double amount = Double.parseDouble(expenseDetails[0].replace("£", ""));
                String category = expenseDetails[1];
                String date = expenseDetails[2];

                pstmt.setInt(1, userId);  // set user id in query
                pstmt.setDouble(2, amount);  // set amount in query
                pstmt.setString(3, category);  // set category in query
                pstmt.setString(4, date);  // set date in query

                int rowsAffected = pstmt.executeUpdate();  // execute the delete query

                if (rowsAffected > 0) {
                    showAlert("✅ Success", "Expense deleted successfully!");
                    loadExpenses();  // reload the expenses list
                    updateChart();  // update the pie chart
                } else {
                    showAlert("❌ Error", "Could not delete expense.");
                }
            } catch (SQLException e) {
                showAlert("❌ Error", "Failed to delete expense.");
                e.printStackTrace();
            }
        }
    }

    // method to navigate back to the main screen
    @FXML
    private void handleBack() {
        SceneController sceneController = new SceneController(SceneManager.getPrimaryStage());
        sceneController.switchToSceneWithUser("main.fxml", userId);
    }

    // method to add a custom category
    @FXML
    private void handleAddCustomCategory() {
        String customCategory = customCategoryField.getText().trim();  // get custom category

        if (customCategory.isEmpty()) {
            showAlert("❌ Error", "Please enter a valid category.");
            return;
        }

        // save custom category to the database
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO UserCategories (user_id, category_name) VALUES (?, ?)")) {
            pstmt.setInt(1, userId);  // set user id in query
            pstmt.setString(2, customCategory);  // set custom category in query
            pstmt.executeUpdate();  // execute query to insert custom category
        } catch (SQLException e) {
            showAlert("❌ Error", "Could not save custom category.");
            e.printStackTrace();
            return;
        }

        // add the custom category to the ComboBox
        categoryBox.getItems().add(customCategory);
        customCategoryField.clear();  // clear the input field
        showAlert("✅ Success", "Custom category added: " + customCategory);
    }

    // method to delete a custom category
    @FXML
    private void handleDeleteCustomCategory() {
        String selectedCategory = categoryBox.getValue();  // get selected category

        if (selectedCategory == null || !categoryBox.getItems().contains(selectedCategory)) {
            showAlert("❌ Error", "Please select a category to delete.");
            return;
        }

        // confirmation prompt before deleting the custom category
        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.setTitle("Confirm Deletion");
        confirmationAlert.setHeaderText("Are you sure you want to delete this custom category?");
        confirmationAlert.setContentText("This action cannot be undone.");
        Optional<ButtonType> result = confirmationAlert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                         "DELETE FROM UserCategories WHERE category_name = ? AND user_id = ?")) {

                pstmt.setString(1, selectedCategory);  // set selected category in query
                pstmt.setInt(2, userId);  // set user id in query
                int rowsAffected = pstmt.executeUpdate();  // execute delete query

                if (rowsAffected > 0) {
                    categoryBox.getItems().remove(selectedCategory);  // remove category from ComboBox
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

    // method to load expenses for the current user from the database
    private void loadExpenses() {
        expenseList.getItems().clear();  // clear the existing list
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT amount, category, date FROM Expenses WHERE user_id = ? ORDER BY date DESC")) {

            pstmt.setInt(1, userId);  // set user id in query
            ResultSet rs = pstmt.executeQuery();  // execute query to get expenses

            while (rs.next()) {
                // format each expense entry
                String expenseEntry = "£" + rs.getDouble("amount") + " - " + rs.getString("category") + " - " + rs.getString("date");
                expenseList.getItems().add(expenseEntry);  // add to the list
            }
        } catch (SQLException e) {
            showAlert("❌ Error", "Could not fetch expenses.");
        }
    }

    // method to get the user's monthly budget from the database
    private double getMonthlyBudget() {
        double budget = 0;  // initialise budget to 0
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT monthly_budget FROM Budgets WHERE user_id = ?")) {

            pstmt.setInt(1, userId);  // set the user id in the query
            ResultSet rs = pstmt.executeQuery();  // execute the query

            if (rs.next()) {  // if a result is found
                budget = rs.getDouble("monthly_budget");  // get the monthly budget
            }
        } catch (SQLException e) {
            showAlert("❌ Error", "Could not load budget.");  // show error if query fails
            e.printStackTrace();
        }
        return budget;  // return the budget
    }

    // method to update the pie chart with expense data
    private void updateChart() {
        expenseChart.getData().clear();  // clear previous chart data

        try (Connection conn = DatabaseManager.getConnection();
             // Use a scrollable ResultSet to allow resetting the cursor
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT category, SUM(amount) as total FROM Expenses WHERE user_id = ? GROUP BY category",
                     ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {

            pstmt.setInt(1, userId);  // set user id in the query
            ResultSet rs = pstmt.executeQuery();  // execute query to get expense totals per category

            double totalExpenses = 0;  // variable to hold total expenses

            // First pass: calculate the total expenses
            while (rs.next()) {
                totalExpenses += rs.getDouble("total");  // add each category's total to total expenses
            }

            // Reset the cursor back to the beginning to calculate percentages
            rs.beforeFirst();

            // Second pass: create the chart and calculate percentages
            while (rs.next()) {
                String category = rs.getString("category");
                double totalAmount = rs.getDouble("total");

                // create PieChart data for each category
                PieChart.Data data = new PieChart.Data(category, totalAmount);
                expenseChart.getData().add(data);

                // calculate percentage based on total expenses
                double percentage = (totalAmount / totalExpenses) * 100;

                // label each slice with the amount and percentage
                data.setName(String.format("%s - £%.2f (%.2f%%)", category, totalAmount, percentage));
            }

            // Fetch the monthly budget from the database
            double monthlyBudget = getMonthlyBudget();

            // display the total amount and budget in the labels
            totalAmountLabel.setText(String.format("Total Expenses: £%.2f", totalExpenses));
            budgetStatusLabel.setText(String.format("Monthly Budget: £%.2f", monthlyBudget));

            // trigger alert if total expenses exceed monthly budget
            if (totalExpenses >= monthlyBudget) {
                showAlert("⚠️ Budget Alert", "You have reached or exceeded your monthly budget!");
            }

        } catch (SQLException e) {
            showAlert("❌ Error", "Could not update chart.");
            e.printStackTrace();  // Print stack trace for debugging
        }
    }


    // method to load categories for the current user from the database
    private void loadCategories() {
        categoryBox.getItems().clear();  // clear the existing categories
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT category_name FROM UserCategories WHERE user_id = ?")) {

            pstmt.setInt(1, userId);  // set user id in query
            ResultSet rs = pstmt.executeQuery();  // execute query to get categories
            while (rs.next()) {
                categoryBox.getItems().add(rs.getString("category_name"));  // add categories to ComboBox
            }

            // if no categories, add default categories
            if (categoryBox.getItems().isEmpty()) {
                categoryBox.getItems().addAll("Food", "Transport", "Rent", "Shopping", "Other");
            }
        } catch (SQLException e) {
            showAlert("❌ Error", "Could not load categories.");
        }
    }

    // method to show alerts to the user
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);  // create an information alert
        alert.setTitle(title);  // set the alert title
        alert.setContentText(message);  // set the alert message
        alert.show();  // show the alert
    }
}
