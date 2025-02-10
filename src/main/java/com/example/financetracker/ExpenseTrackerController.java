package com.example.financetracker;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ExpenseTrackerController {
    @FXML private TextField amountField;
    @FXML private ComboBox<String> categoryBox;
    @FXML private ListView<String> expenseList;
    @FXML private PieChart expenseChart;

    @FXML
    public void initialize() {
        categoryBox.setItems(FXCollections.observableArrayList("Food", "Transport", "Rent", "Miscellaneous"));
        loadExpenses();
    }

    @FXML
    private void addExpense() {
        try (Connection conn = DatabaseManager.getConnection()) {
            double amount = Double.parseDouble(amountField.getText());
            String category = categoryBox.getValue();

            if (category == null || category.isEmpty()) {
                showAlert("Error", "Please select a category!");
                return;
            }

            PreparedStatement pstmt = conn.prepareStatement("INSERT INTO Expenses (amount, category) VALUES (?, ?)");
            pstmt.setDouble(1, amount);
            pstmt.setString(2, category);
            pstmt.executeUpdate();

            showAlert("Success", "Expense added successfully!");
            loadExpenses();
        } catch (NumberFormatException e) {
            showAlert("Error", "Please enter a valid amount!");
        } catch (SQLException e) {
            showAlert("Error", "Could not save expense!");
            e.printStackTrace();
        }
    }

    private void loadExpenses() {
        expenseList.getItems().clear();
        expenseChart.getData().clear();

        try (Connection conn = DatabaseManager.getConnection()) {
            ResultSet rs = conn.createStatement().executeQuery("SELECT category, SUM(amount) AS total FROM Expenses GROUP BY category");

            while (rs.next()) {
                String category = rs.getString("category");
                double total = rs.getDouble("total");

                expenseList.getItems().add(String.format("%s: £%.2f", category, total));
                expenseChart.getData().add(new PieChart.Data(category, total));
            }
        } catch (SQLException e) {
            showAlert("Error", "Failed to load expenses!");
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}