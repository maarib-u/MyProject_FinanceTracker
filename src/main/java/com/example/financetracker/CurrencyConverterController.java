package com.example.financetracker;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CurrencyConverterController {

    @FXML private ComboBox<String> fromCurrencyBox;
    @FXML private ComboBox<String> toCurrencyBox;
    @FXML private TextField amountField;
    @FXML private Label resultLabel;
    @FXML private Button convertButton;
    @FXML private Button updateRatesButton;
    @FXML private Button backButton; // ✅ Add Back button reference

    private int userId;
    private final CurrencyConverter currencyConverter = new CurrencyConverter();

    @FXML
    public void initialize() {
        loadCurrencyList();
    }

    public void setUserId(int userId) {
        this.userId = userId;
        System.out.println("DEBUG: Currency Converter - User ID: " + userId);
    }

    private void loadCurrencyList() {
        ObservableList<String> currencies = FXCollections.observableArrayList();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT DISTINCT currency_code FROM exchange_rates");
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                currencies.add(rs.getString("currency_code"));
            }

            fromCurrencyBox.setItems(currencies);
            toCurrencyBox.setItems(currencies);

        } catch (SQLException e) {
            showAlert("Error", "Failed to load currencies from the database.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleConvert() {
        String fromCurrency = fromCurrencyBox.getValue();
        String toCurrency = toCurrencyBox.getValue();
        String amountText = amountField.getText();

        if (fromCurrency == null || toCurrency == null || amountText.isEmpty()) {
            showAlert("Error", "Please select both currencies and enter an amount.");
            return;
        }

        try {
            double amount = Double.parseDouble(amountText);
            double convertedAmount = currencyConverter.convertCurrency(amount, fromCurrency, toCurrency);
            resultLabel.setText(String.format("%.2f %s = %.2f %s", amount, fromCurrency, convertedAmount, toCurrency));
        } catch (NumberFormatException e) {
            showAlert("Error", "Invalid amount entered.");
        } catch (Exception e) {
            showAlert("Error", "Conversion failed.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleUpdateRates() {
        boolean success = currencyConverter.updateCurrencyRates();
        if (success) {
            showAlert("Success", "Exchange rates updated successfully!");
            loadCurrencyList();
        } else {
            showAlert("Error", "Failed to update exchange rates.");
        }
    }

    @FXML
    private void handleBack() {  // ✅ Back button function
        SceneController sceneController = new SceneController(SceneManager.getPrimaryStage());
        sceneController.switchToSceneWithUser("main.fxml", userId);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
