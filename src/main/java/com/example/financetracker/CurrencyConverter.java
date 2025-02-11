package com.example.financetracker;

import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;
import java.util.Scanner;

public class CurrencyConverter {

    private static final String API_KEY = "4ddad6c7a68e428f93d410790ba0609a"; // Replace with your Fixer.io API Key
    private static final String API_URL = "http://data.fixer.io/api/latest?access_key=" + API_KEY;

    /**
     * Fetches and updates currency exchange rates from Fixer.io API.
     *
     * @return true if update was successful, false otherwise.
     */
    public boolean updateCurrencyRates() {
        try (Connection conn = DatabaseManager.getConnection()) {
            // Connect to API and fetch exchange rates
            URL url = new URL(API_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            Scanner sc = new Scanner(url.openStream());
            StringBuilder result = new StringBuilder();
            while (sc.hasNext()) {
                result.append(sc.nextLine());
            }
            sc.close();

            // Parse JSON response
            JSONObject jsonResponse = new JSONObject(result.toString());

            // Check if API request was successful
            if (!jsonResponse.optBoolean("success", false)) {
                System.err.println("❌ Currency API request failed.");
                return false;
            }

            JSONObject rates = jsonResponse.optJSONObject("rates");
            if (rates == null || rates.length() == 0) {
                System.err.println("❌ No currency rates found.");
                return false;
            }

            // Clear old exchange rates
            try (PreparedStatement deleteStmt = conn.prepareStatement("DELETE FROM exchange_rates")) {
                deleteStmt.executeUpdate();
            }

            // Insert new exchange rates
            try (PreparedStatement insertStmt = conn.prepareStatement(
                    "INSERT INTO exchange_rates (currency_code, rate_to_base, last_updated) VALUES (?, ?, GETDATE())")) {

                for (String currency : rates.keySet()) {
                    double rate = rates.optDouble(currency, -1);

                    // Skip invalid or zero rates
                    if (rate <= 0 || rate > 999999999) {
                        System.err.println("⚠️ Skipping currency " + currency + " due to invalid rate: " + rate);
                        continue;
                    }

                    insertStmt.setString(1, currency);
                    insertStmt.setDouble(2, rate);
                    insertStmt.addBatch();
                }

                insertStmt.executeBatch();
                System.out.println("✅ Exchange rates updated successfully.");
                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Converts an amount from one currency to another using stored exchange rates.
     *
     * @param amount      Amount to be converted.
     * @param fromCurrency Source currency code.
     * @param toCurrency   Target currency code.
     * @return Converted amount.
     */
    public double convertCurrency(double amount, String fromCurrency, String toCurrency) {
        try (Connection conn = DatabaseManager.getConnection()) {
            String query = "SELECT currency_code, rate_to_base FROM exchange_rates WHERE currency_code IN (?, ?)";

            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, fromCurrency);
                pstmt.setString(2, toCurrency);
                ResultSet rs = pstmt.executeQuery();

                double fromRate = 0, toRate = 0;

                while (rs.next()) {
                    String currencyCode = rs.getString("currency_code");
                    double rate = rs.getDouble("rate_to_base");

                    if (currencyCode.equals(fromCurrency)) {
                        fromRate = rate;
                    } else if (currencyCode.equals(toCurrency)) {
                        toRate = rate;
                    }
                }

                if (fromRate == 0 || toRate == 0) {
                    throw new IllegalArgumentException("❌ Invalid currency rates retrieved.");
                }

                return (amount / fromRate) * toRate;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }
}
