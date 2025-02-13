package com.example.financetracker;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ExpenseTracker extends Application {
    private final Connection conn;  // connection to the database
    private final String currentUser;  // current user logged in

    // constructor to set the current user and initialize the database connection
    public ExpenseTracker(String username) {
        this.currentUser = username;
        this.conn = DatabaseManager.getConnection(); // ✅ Connect to DB
    }

    @Override
    public void start(Stage primaryStage) {
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));

        // create the UI components
        Label expenseLabel = new Label("Add Expense:");
        TextField amountField = new TextField();  // text field for entering amount
        ComboBox<String> categoryBox = new ComboBox<>();  // combo box for selecting category
        categoryBox.getItems().addAll("Food", "Transport", "Rent", "Shopping", "Other");  // pre-filled categories
        Button addButton = new Button("Add Expense");  // button to add expense

        ListView<String> expenseList = new ListView<>();  // list view to display expenses
        Button refreshButton = new Button("Show Expenses");  // button to refresh the expenses list

        PieChart expenseChart = new PieChart();  // chart to show expense distribution

        // when "Add Expense" button is clicked
        addButton.setOnAction(e -> {
            try {
                // parse the amount entered and call addExpense method
                double amount = Double.parseDouble(amountField.getText());
                addExpense(amount, categoryBox.getValue());
                amountField.clear();  // clear the amount field
                expenseList.getItems().clear();  // clear the list
                expenseList.getItems().addAll(getExpenses());  // update the expense list
                updateChart(expenseChart);  // update the pie chart with new data
            } catch (NumberFormatException ex) {
                // show error alert if invalid amount is entered
                showAlert("Error", "Please enter a valid amount.");
            }
        });

        // when "Show Expenses" button is clicked
        refreshButton.setOnAction(e -> {
            expenseList.getItems().clear();  // clear the list
            expenseList.getItems().addAll(getExpenses());  // load expenses
            updateChart(expenseChart);  // update the pie chart
        });

        // add all components to the root container
        root.getChildren().addAll(expenseLabel, amountField, categoryBox, addButton, refreshButton, expenseList, expenseChart);

        // set the scene and show the window
        primaryStage.setScene(new Scene(root, 500, 700));
        primaryStage.setTitle("Expense Tracker");
        primaryStage.show();
    }

    // method to add a new expense to the database
    private void addExpense(double amount, String category) {
        if (category == null || category.isEmpty()) {
            // Show error alert if no category is selected
            showAlert("Error", "Please select a category.");
            return;
        }

        try (PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO Expenses (user_id, amount, category, date) VALUES ((SELECT id FROM Users WHERE username = ?), ?, ?, GETDATE())")) {
            pstmt.setString(1, currentUser);  // set the current user
            pstmt.setDouble(2, amount);  // set the amount
            pstmt.setString(3, category);  // set the category
            pstmt.executeUpdate();  // execute the query to add the expense
            showAlert("Success", "Expense added successfully!");  // show success message
        } catch (SQLException e) {
            // Show error alert if there is a database error
            showAlert("Database Error", "Could not add expense.");
            e.printStackTrace();
        }
    }

    // method to fetch all expenses for the current user from the database
    private String[] getExpenses() {
        try (PreparedStatement pstmt = conn.prepareStatement(
                "SELECT amount, category, date FROM Expenses WHERE user_id = (SELECT id FROM Users WHERE username = ?) ORDER BY date DESC")) {
            pstmt.setString(1, currentUser);  // set the current user
            ResultSet rs = pstmt.executeQuery();  // execute the query to get the expenses

            StringBuilder sb = new StringBuilder();
            while (rs.next()) {
                // format the expense details
                sb.append("£").append(rs.getDouble("amount"))
                        .append(" - ").append(rs.getString("category"))
                        .append(" - ").append(rs.getString("date"))
                        .append("\n");
            }
            return sb.toString().split("\n");  // return the expenses as a string array
        } catch (SQLException e) {
            // Show error alert if there is a database error
            return new String[]{"Error fetching expenses!"};
        }
    }

    // method to update the pie chart with the expenses data
    private void updateChart(PieChart chart) {
        chart.getData().clear();  // clear previous chart data

        try (PreparedStatement pstmt = conn.prepareStatement(
                "SELECT category, SUM(amount) as total FROM Expenses WHERE user_id = (SELECT id FROM Users WHERE username = ?) GROUP BY category")) {
            pstmt.setString(1, currentUser);  // set the current user
            ResultSet rs = pstmt.executeQuery();  // execute the query to get the expense categories and totals

            // add each category and its total to the pie chart
            while (rs.next()) {
                PieChart.Data data = new PieChart.Data(rs.getString("category"), rs.getDouble("total"));
                chart.getData().add(data);
            }
        } catch (SQLException e) {
            // show error alert if there is a database error
            showAlert("Error", "Could not update chart.");
        }
    }

    // method to show alerts to the user
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);  // create a new information alert
        alert.setTitle(title);  // set the alert title
        alert.setContentText(message);  // set the message content
        alert.show();  // show the alert
    }
}
