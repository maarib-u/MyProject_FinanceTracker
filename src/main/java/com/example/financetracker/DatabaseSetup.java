package com.example.financetracker;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseSetup {
    public static void main(String[] args) {
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {

            System.out.println("🚀 Setting up the database...");

            // Users Table (Secure password storage using hashed passwords)
            stmt.execute("""
                IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='Users' AND xtype='U')
                CREATE TABLE Users (
                    id INT IDENTITY(1,1) PRIMARY KEY,
                    username NVARCHAR(50) UNIQUE NOT NULL,
                    password NVARCHAR(255) NOT NULL,
                    email NVARCHAR(100) UNIQUE NULL
                )
            """);

            // Expenses Table (Tracks user spending)
            stmt.execute("""
                IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='Expenses' AND xtype='U')
                CREATE TABLE Expenses (
                    id INT IDENTITY(1,1) PRIMARY KEY,
                    user_id INT NOT NULL,
                    amount DECIMAL(10,2) NOT NULL,
                    category NVARCHAR(50) NOT NULL,
                    description NVARCHAR(255) NULL,
                    date DATE DEFAULT GETDATE(),
                    FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE
                )
            """);

            // Budgets Table (Allows users to set monthly budgets)
            stmt.execute("""
                IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='Budgets' AND xtype='U')
                CREATE TABLE Budgets (
                    id INT IDENTITY(1,1) PRIMARY KEY,
                    user_id INT NOT NULL,
                    monthly_budget DECIMAL(10,2) NOT NULL,
                    alert_threshold DECIMAL(5,2) DEFAULT 80,
                    FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE
                )
            """);

            // Exchange Rates Table (Stores live exchange rates for different currencies)
            stmt.execute("""
                IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='ExchangeRates' AND xtype='U')
                CREATE TABLE ExchangeRates (
                    id INT IDENTITY(1,1) PRIMARY KEY,
                    currency_code NVARCHAR(10) NOT NULL UNIQUE,
                    rate_to_base DECIMAL(10,6) NOT NULL,
                    last_updated DATETIME DEFAULT GETDATE()
                )
            """);

            // Currency Cache Table (Stores the full API response for offline conversion)
            stmt.execute("""
                IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='CurrencyCache' AND xtype='U')
                CREATE TABLE CurrencyCache (
                    id INT IDENTITY(1,1) PRIMARY KEY,
                    rates NVARCHAR(MAX) NOT NULL,
                    date_saved DATETIME DEFAULT GETDATE()
                )
            """);

            System.out.println("✅ Database setup complete!");

        } catch (SQLException e) {
            System.err.println("❌ Database setup failed!");
            e.printStackTrace();
        }
    }
}
