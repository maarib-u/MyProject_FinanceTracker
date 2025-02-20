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
import java.text.DecimalFormat;

public class ExpenseTracker extends Application {
    private Connection conn;
    private String currentUser;

    public ExpenseTracker() {
        this.conn = DatabaseManager.getConnection();
        this.currentUser = "defaultUser"; // Placeholder; replace with actual user authentication logic
    }

    @Override
    public void start(Stage primaryStage) {
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));

        Label expenseLabel = new Label("Add Expense:");
        TextField amountField = new TextField();
        ComboBox<String> categoryBox = new ComboBox<>();
        categoryBox.getItems().addAll("Food", "Transport", "Rent", "Shopping", "Other");
        Button addButton = new Button("Add Expense");
        ListView<String> expenseList = new ListView<>();
        Button refreshButton = new Button("Show Expenses");
        PieChart expenseChart = new PieChart();

        addButton.setOnAction(e -> {
            try {
                double amount = Double.parseDouble(amountField.getText());
                addExpense(amount, categoryBox.getValue());
                amountField.clear();
                refreshExpenseList(expenseList);
                updateChart(expenseChart);
            } catch (NumberFormatException ex) {
                showAlert("❌ Error", "Please enter a valid amount.");
            }
        });

        refreshButton.setOnAction(e -> {
            refreshExpenseList(expenseList);
            updateChart(expenseChart);
        });

        root.getChildren().addAll(expenseLabel, amountField, categoryBox, addButton, refreshButton, expenseList, expenseChart);

        primaryStage.setScene(new Scene(root, 500, 700));
        primaryStage.setTitle("Expense Tracker");
        primaryStage.show();
    }

    private void addExpense(double amount, String category) {
        if (category == null || category.isEmpty()) {
            showAlert("❌ Error", "Please select a category.");
            return;
        }

        try (PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO Expenses (user_id, amount, category, date) VALUES ((SELECT id FROM Users WHERE username = ?), ?, ?, GETDATE())")) {
            pstmt.setString(1, currentUser);
            pstmt.setDouble(2, amount);
            pstmt.setString(3, category);
            pstmt.executeUpdate();
            showAlert("✅ Success", "Expense added successfully!");
        } catch (SQLException e) {
            showAlert("❌ Database Error", "Could not add expense.");
            e.printStackTrace();
        }
    }

    private void refreshExpenseList(ListView<String> expenseList) {
        expenseList.getItems().clear();
        try (PreparedStatement pstmt = conn.prepareStatement(
                "SELECT amount, category, date FROM Expenses WHERE user_id = (SELECT id FROM Users WHERE username = ?) ORDER BY date DESC")) {
            pstmt.setString(1, currentUser);
            ResultSet rs = pstmt.executeQuery();

            DecimalFormat currencyFormat = new DecimalFormat("£#,##0.00");

            while (rs.next()) {
                String expenseDetails = String.format("%s - %s - %s",
                        currencyFormat.format(rs.getDouble("amount")),
                        rs.getString("category"),
                        rs.getString("date"));
                expenseList.getItems().add(expenseDetails);
            }
        } catch (SQLException e) {
            showAlert("❌ Error", "Could not fetch expenses.");
        }
    }

    private void updateChart(PieChart chart) {
        chart.getData().clear();
        try (PreparedStatement pstmt = conn.prepareStatement(
                "SELECT category, SUM(amount) as total FROM Expenses WHERE user_id = (SELECT id FROM Users WHERE username = ?) GROUP BY category")) {
            pstmt.setString(1, currentUser);
            ResultSet rs = pstmt.executeQuery();

            DecimalFormat currencyFormat = new DecimalFormat("£#,##0.00");

            while (rs.next()) {
                PieChart.Data data = new PieChart.Data(rs.getString("category"), rs.getDouble("total"));
                data.setName(String.format("%s - %s", rs.getString("category"), currencyFormat.format(rs.getDouble("total"))));
                chart.getData().add(data);
            }
        } catch (SQLException e) {
            showAlert("❌ Error", "Could not update chart.");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.show();
    }
}
