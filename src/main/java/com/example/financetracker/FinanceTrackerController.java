package com.example.financetracker;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class FinanceTrackerController {
    @FXML
    private Label balanceLabel;

    @FXML
    private TextField amountField;

    @FXML
    private Button addButton;

    @FXML
    protected void addExpense() {
        String amountText = amountField.getText();
        balanceLabel.setText("Added Expense: " + amountText);
    }
}

