package com.example.financetracker;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.util.Optional;

public class CurrencyConverterController {
    @FXML private ComboBox<String> fromCurrencyBox;  // combo box for selecting the 'from' currency
    @FXML private ComboBox<String> toCurrencyBox;    // combo box for selecting the 'to' currency
    @FXML private TextField amountField;              // text field for entering the amount to be converted
    @FXML private Label resultLabel;                  // label to display the result of the conversion
    @FXML private Button backButton;                  // button to navigate back to the main screen

    private int userId;  // user id to identify the current user
    private final CurrencyConverter currencyConverter = new CurrencyConverter(); // instance of CurrencyConverter for conversion logic

    // initialise the ComboBoxes with currency list and set the user prompts
    @FXML
    public void initialize() {
        loadCurrencyList(); // load the full list of currencies when the page is initialised
        setupComboBoxListeners(); // setup listeners to handle filtering and resetting the ComboBox

        // set prompt text to guide the user to select a currency
        fromCurrencyBox.setPromptText("Choose currency");
        toCurrencyBox.setPromptText("Choose currency");
    }

    // set the user ID for this scene
    public void setUserId(int userId) {
        this.userId = userId;
        System.out.println("DEBUG: Currency Converter - User ID: " + userId);  // debug log
    }

    // load the currency list from the database into both ComboBoxes
    private void loadCurrencyList() {
        ObservableList<String> currencies = FXCollections.observableArrayList();  // list to store currencies

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT DISTINCT currency_code FROM exchange_rates");
             ResultSet rs = pstmt.executeQuery()) {

            // fetch and add all currencies to the list
            while (rs.next()) {
                currencies.add(rs.getString("currency_code"));
            }

            // set the list of currencies to the ComboBoxes
            fromCurrencyBox.setItems(currencies);
            toCurrencyBox.setItems(currencies);

        } catch (SQLException e) {
            showAlert("❌ Error", "Failed to load currencies.");
            e.printStackTrace();
        }
    }

    // setup listeners for ComboBoxes to handle filtering and resetting based on user input
    private void setupComboBoxListeners() {
        // listener for user typing into the 'from' currency ComboBox
        fromCurrencyBox.setEditable(true);
        fromCurrencyBox.setOnKeyReleased(event -> filterComboBox(fromCurrencyBox));  // filter list based on input

        // listener for user typing into the 'to' currency ComboBox
        toCurrencyBox.setEditable(true);
        toCurrencyBox.setOnKeyReleased(event -> filterComboBox(toCurrencyBox));  // filter list based on input

        // listener to reset ComboBox when clicked without input (show full list)
        fromCurrencyBox.setOnMouseClicked(event -> resetComboBoxList(fromCurrencyBox));
        toCurrencyBox.setOnMouseClicked(event -> resetComboBoxList(toCurrencyBox));
    }

    // filter the ComboBox list based on user input
    private void filterComboBox(ComboBox<String> comboBox) {
        String filterText = comboBox.getEditor().getText().toUpperCase();  // get the text input and convert to uppercase

        ObservableList<String> filteredList = FXCollections.observableArrayList();  // list to store filtered results

        // check if there's any filter text
        if (!filterText.isEmpty()) {
            // add currencies that match the filter
            for (String currency : comboBox.getItems()) {
                if (currency.toUpperCase().contains(filterText)) {
                    filteredList.add(currency); // add matching currencies
                }
            }
            comboBox.setItems(filteredList);  // update ComboBox with filtered list
        } else {
            resetComboBoxList(comboBox);  // reset ComboBox to full list if input is empty
        }
    }

    // reset the ComboBox to show the full list of currencies
    private void resetComboBoxList(ComboBox<String> comboBox) {
        if (!comboBox.getItems().isEmpty()) {
            loadCurrencyList();  // reset to the full list of currencies
            comboBox.getEditor().clear();  // clear the input field
        }
    }

    // handle the currency conversion logic
    @FXML
    private void handleConvert() {
        String fromCurrency = fromCurrencyBox.getValue();  // get the selected 'from' currency
        String toCurrency = toCurrencyBox.getValue();      // get the selected 'to' currency
        String amountText = amountField.getText();         // get the amount to convert

        // validate user input: ensure both currencies are selected and amount is entered
        if (fromCurrency == null || toCurrency == null || amountText.isEmpty()) {
            showAlert("❌ Error", "Please select both currencies and enter an amount.");
            return;
        }

        try {
            double amount = Double.parseDouble(amountText);  // convert the entered amount to a double

            // perform the currency conversion
            double convertedAmount = currencyConverter.convertCurrency(amount, fromCurrency, toCurrency);
            resultLabel.setText(String.format("💰 %.2f %s = %.2f %s", amount, fromCurrency, convertedAmount, toCurrency));

        } catch (NumberFormatException e) {
            showAlert("❌ Error", "Invalid amount entered.");  // handle invalid amount
        } catch (Exception e) {
            showAlert("❌ Error", "Conversion failed.");
            e.printStackTrace();
        }
    }

    // handle updating exchange rates from the API
    @FXML
    private void handleUpdateRates() {
        boolean success = currencyConverter.updateCurrencyRates();  // update exchange rates from the API
        if (success) {
            showAlert("✅ Success", "Exchange rates updated successfully!");
            loadCurrencyList();  // refresh currency list after update
        } else {
            showAlert("❌ Error", "Failed to update exchange rates.");
        }
    }

    // navigate back to the main screen
    @FXML
    private void handleBack() {
        SceneController sceneController = new SceneController(SceneManager.getPrimaryStage());
        sceneController.switchToSceneWithUser("main.fxml", userId);  // switch to main scene with user ID
    }

    // utility method to show alerts
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);  // create an info alert
        alert.setTitle(title);  // set the alert title
        alert.setContentText(message);  // set the message content
        alert.showAndWait();  // display the alert and wait for user interaction
    }
}
