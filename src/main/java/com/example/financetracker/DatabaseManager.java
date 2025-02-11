package com.example.financetracker;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlserver://localhost:1433;databaseName=FinanceDB;encrypt=true;trustServerCertificate=true";
    private static final String DB_USER = "SA";
    private static final String DB_PASSWORD = "Passw0rd";

    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        } catch (SQLException e) {
            System.err.println("❌ Database connection failed!");
            e.printStackTrace();
            throw new RuntimeException("Failed to connect to the database.");
        }
    }
}
