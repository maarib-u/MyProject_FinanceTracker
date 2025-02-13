package com.example.financetracker;

import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;
import java.util.Scanner;

public class CurrencyConverter {
    private static final String API_KEY = "4ddad6c7a68e428f93d410790ba0609a"; // fixer.io API Key
    private static final String API_URL = "http://data.fixer.io/api/latest?access_key=" + API_KEY;

    // fetches and updates currency exchange rates from the fixer.io API
    public boolean updateCurrencyRates() {
        try (Connection conn = DatabaseManager.getConnection()) {
            // connect to the API and fetch the exchange rates
            URL url = new URL(API_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            // read the response from the API
            Scanner sc = new Scanner(url.openStream());
            StringBuilder result = new StringBuilder();
            while (sc.hasNext()) {
                result.append(sc.nextLine()); // append each line to result
            }
            sc.close();

            // parse the JSON response from the API
            JSONObject jsonResponse = new JSONObject(result.toString());

            // check if the API request was successful
            if (!jsonResponse.optBoolean("success", false)) {
                System.err.println("❌ currency api request failed.");
                return false;
            }

            // extract the rates object from the response
            JSONObject rates = jsonResponse.optJSONObject("rates");
            if (rates == null || rates.length() == 0) {
                System.err.println("❌ no currency rates found.");
                return false;
            }

            // clear old exchange rates from the database
            try (PreparedStatement deleteStmt = conn.prepareStatement("DELETE FROM exchange_rates")) {
                deleteStmt.executeUpdate();  // delete existing data
            }

            // insert new exchange rates into the database
            try (PreparedStatement insertStmt = conn.prepareStatement(
                    "INSERT INTO exchange_rates (currency_code, rate_to_base, last_updated) VALUES (?, ?, GETDATE())")) {
                // loop through each currency and insert its rate
                for (String currency : rates.keySet()) {
                    double rate = rates.optDouble(currency, -1);
                    if (rate <= 0 || rate > 999999999) {
                        System.err.println("⚠️ skipping currency " + currency + " due to invalid rate: " + rate);
                        continue;  // skip invalid rates
                    }
                    insertStmt.setString(1, currency);  // set the currency code
                    insertStmt.setDouble(2, rate);      // set the rate to base currency
                    insertStmt.addBatch();  // add to batch for efficient insertion
                }
                insertStmt.executeBatch();  // execute the batch insert
                System.out.println("✅ exchange rates updated successfully.");
                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;  // return false if an error occurs
        }
    }

    // converts an amount from one currency to another using stored exchange rates
    public double convertCurrency(double amount, String fromCurrency, String toCurrency) {
        try (Connection conn = DatabaseManager.getConnection()) {
            // prepare SQL query to fetch exchange rates for both currencies
            String query = "SELECT currency_code, rate_to_base FROM exchange_rates WHERE currency_code IN (?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, fromCurrency);  // set 'from' currency
                pstmt.setString(2, toCurrency);    // set 'to' currency
                ResultSet rs = pstmt.executeQuery(); // execute the query

                double fromRate = 0, toRate = 0;  // variables to store rates
                while (rs.next()) {
                    String currencyCode = rs.getString("currency_code");
                    double rate = rs.getDouble("rate_to_base");

                    // check if the currency matches 'from' or 'to'
                    if (currencyCode.equals(fromCurrency)) {
                        fromRate = rate;  // store rate for 'from' currency
                    } else if (currencyCode.equals(toCurrency)) {
                        toRate = rate;  // store rate for 'to' currency
                    }
                }

                // if either rate is zero, something went wrong
                if (fromRate == 0 || toRate == 0) {
                    throw new IllegalArgumentException("❌ invalid currency rates retrieved.");
                }

                // convert the amount based on the exchange rates
                return (amount / fromRate) * toRate;  // formula for conversion
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return 0;  // return 0 if an error occurs
        }
    }
}
