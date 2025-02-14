package com.example.financetracker;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {
    // URL for the database connection, which specifies the server, port, and database name
    private static final String DB_URL = "jdbc:sqlserver://localhost:1433;databaseName=FinanceDB;encrypt=true;trustServerCertificate=true";
    // Username for accessing the database
    private static final String DB_USER = "SA";
    // Password for accessing the database
    private static final String DB_PASSWORD = "Passw0rd";

    // method to establish and return a connection to the database
    public static Connection getConnection() {
        try {
            // attempt to get a connection to the database using the provided URL, username, and password
            return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        } catch (SQLException e) {
            // if the connection fails, display an error alert to the user and print the error stack trace for debugging
            showError("Could not connect to the database. Please check your connection settings.");
            e.printStackTrace();
            // throw a runtime exception to signal that a critical failure occurred while connecting
            throw new RuntimeException("Failed to connect to the database.");
        }
    }

    // method to show an error message to the user
    private static void showError(String message) {
        // create an alert to display the error message to the user
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("❌ Database Connection Failed"); // set the alert's title
        alert.setHeaderText("Database connection failed."); // set the header of the alert
        alert.setContentText(message); // set the content message to explain the error

        // add a single OK button to the alert
        alert.getButtonTypes().setAll(ButtonType.OK);
        alert.showAndWait(); // show the alert and wait for the user to acknowledge it
    }
}
