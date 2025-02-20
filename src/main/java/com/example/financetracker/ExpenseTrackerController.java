package com.example.financetracker;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.chart.PieChart;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class ExpenseTrackerController {
    @FXML private TextField expenseField;  // field for entering expense amount
    @FXML private ComboBox<String> categoryBox;  // drop-down for selecting category
    @FXML private PieChart expenseChart;  // pie chart to show expense distribution
    @FXML private TextField customCategoryField;  // text field for adding a custom category
    @FXML private Label totalAmountLabel;  // label to show total expenses
    @FXML private Label budgetStatusLabel;  // Label for displaying monthly budget
    @FXML private TableView<Expense> expenseTable;
    @FXML private TableColumn<Expense, String> categoryColumn;
    @FXML private TableColumn<Expense, Double> amountColumn;
    @FXML private TableColumn<Expense, String> dateColumn;
    @FXML private TableColumn<Expense, String> timeColumn;


    private int userId;  // user id to identify the current user

    // method to set user id and load expenses and categories
    public void setUserId(int userId) {
        this.userId = userId;
        System.out.println("DEBUG: ExpenseTracker - User ID set to " + userId);
        loadCategories();  // load categories from database
        loadExpenses();  // load expenses from database
        updateChart();  // update pie chart
    }
    @FXML
    public void initialize() {
        categoryColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCategory()));
        amountColumn.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getAmount()).asObject());
        dateColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDate()));
        timeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTime()));

        expenseTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        loadExpenses();  // Load the expenses when the app starts
    }

    // method to add an expense
    // ✅ Add Expense
    @FXML
    private void handleAddExpense() {
        String category = categoryBox.getValue();
        String amountText = expenseField.getText();

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
    private String convertToSQLDateTimeFormat(String ukDate, String time) {
        try {
            DateTimeFormatter ukFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            DateTimeFormatter sqlFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            // Combine UK Date and Time before converting
            String combinedDateTime = ukDate + " " + time;
            LocalDateTime parsedDateTime = LocalDateTime.parse(combinedDateTime, ukFormat);

            return parsedDateTime.format(sqlFormat);
        } catch (Exception e) {
            showAlert("❌ Error", "Failed to convert date format.");
            e.printStackTrace();
            return null;
        }
    }

    // ✅ Load Expenses
    private void loadExpenses() {
        expenseTable.getItems().clear();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT category, amount, date FROM Expenses WHERE user_id = ? ORDER BY date DESC")) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            DateTimeFormatter ukDateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            DateTimeFormatter ukTimeFormat = DateTimeFormatter.ofPattern("HH:mm:ss");

            while (rs.next()) {
                double amount = rs.getDouble("amount");
                String category = rs.getString("category");

                LocalDateTime dateTime = rs.getTimestamp("date").toLocalDateTime();
                String formattedDate = dateTime.format(ukDateFormat);
                String formattedTime = dateTime.format(ukTimeFormat);

                expenseTable.getItems().add(new Expense(category, amount, formattedDate, formattedTime));
            }

        } catch (SQLException e) {
            showAlert("❌ Error", "Could not fetch expenses.");
            e.printStackTrace();
        }
    }
    // ✅ Delete Expense
    @FXML
    private void handleDeleteExpense() {
        Expense selectedExpense = expenseTable.getSelectionModel().getSelectedItem();

        if (selectedExpense == null) {
            showAlert("❌ Error", "Please select an expense to delete.");
            return;
        }

        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.setTitle("Confirm Deletion");
        confirmationAlert.setHeaderText("Are you sure you want to delete this expense?");
        confirmationAlert.setContentText("This action cannot be undone.");
        Optional<ButtonType> result = confirmationAlert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                         "DELETE FROM Expenses WHERE user_id = ? AND amount = ? AND category = ? " +
                                 "AND CAST(date AS DATETIME2(0)) = CAST(? AS DATETIME2(0))")) {

                pstmt.setInt(1, userId);
                pstmt.setDouble(2, selectedExpense.getAmount());
                pstmt.setString(3, selectedExpense.getCategory());

                // Convert date format correctly
                String sqlFormattedDate = convertToSQLDateTimeFormat(selectedExpense.getDate(), selectedExpense.getTime());
                pstmt.setString(4, sqlFormattedDate);

                System.out.println("Deleting expense with date: " + sqlFormattedDate); // Debugging log

                int rowsAffected = pstmt.executeUpdate();

                if (rowsAffected > 0) {
                    showAlert("✅ Success", "Expense deleted successfully!");
                    loadExpenses();
                    updateChart();
                } else {
                    showAlert("❌ Error", "Could not delete expense. No match found.");
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
