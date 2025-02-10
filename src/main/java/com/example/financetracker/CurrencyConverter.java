package com.example.financetracker;

import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.util.Scanner;

public class CurrencyConverter {
    private static final String API_KEY = "4ddad6c7a68e428f93d410790ba0609a";  // Replace with your Fixer.io API Key
    private static final String API_URL = "http://data.fixer.io/api/latest?access_key=" + API_KEY;

    public boolean updateCurrencyRates() {
        try (Connection conn = DatabaseManager.getConnection()) {
            URL url = new URL(API_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            Scanner sc = new Scanner(url.openStream());
            StringBuilder result = new StringBuilder();
            while (sc.hasNext()) result.append(sc.nextLine());
            sc.close();

            PreparedStatement pstmt = conn.prepareStatement("INSERT INTO CurrencyCache (rates, date_saved) VALUES (?, ?)");
            pstmt.setString(1, result.toString());
            pstmt.setString(2, LocalDate.now().toString());
            pstmt.executeUpdate();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public double convertCurrency(double amount, String targetCurrency) {
        try (Connection conn = DatabaseManager.getConnection()) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT rates FROM CurrencyCache ORDER BY id DESC LIMIT 1");

            if (rs.next()) {
                JSONObject rates = new JSONObject(rs.getString("rates")).getJSONObject("rates");
                double gbpToBase = rates.getDouble("GBP");
                double targetRate = rates.getDouble(targetCurrency);
                return (amount / gbpToBase) * targetRate;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
