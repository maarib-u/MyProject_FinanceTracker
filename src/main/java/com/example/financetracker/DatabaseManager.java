package com.example.financetracker;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {
    // URL for the database connection
    private static final String DB_URL = "jdbc:sqlserver://localhost:1433;databaseName=FinanceDB;encrypt=true;trustServerCertificate=true";
    // Database username
    private static final String DB_USER = "SA";
    // Database password
    private static final String DB_PASSWORD = "Passw0rd";

    // method to establish and return a database connection
    public static Connection getConnection() {
        try {
            // attempt to get a connection to the database using the provided details
            return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        } catch (SQLException e) {
            // if the connection fails, print an error message and stack trace
            System.err.println("❌ Database connection failed!");
            e.printStackTrace();
            // throw a runtime exception to signal a critical failure in connection
            throw new RuntimeException("Failed to connect to the database.");
        }
    }
}
