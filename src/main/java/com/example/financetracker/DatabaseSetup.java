package com.example.financetracker;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseSetup {
    public static void main(String[] args) {
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {

            // print a message indicating that the database setup process has started
            System.out.println("🚀 Setting up the database...");

            // Users Table: stores user details including username, password (hashed), and email
            stmt.execute("""
                IF NOT EXISTS (SELECT * FROM sys.tables WHERE name='Users')
                CREATE TABLE Users (
                    id INT IDENTITY(1,1) PRIMARY KEY,        -- Unique ID for each user
                    username NVARCHAR(50) UNIQUE NOT NULL,    -- Username must be unique and not null
                    password NVARCHAR(255) NOT NULL,          -- Store hashed password
                    email NVARCHAR(100) UNIQUE NULL           -- Optional email address (unique if provided)
                )
            """);

            // Expenses Table: stores expense details for each user including amount, category, and description
            stmt.execute("""
                IF NOT EXISTS (SELECT * FROM sys.tables WHERE name='Expenses')
                CREATE TABLE Expenses (
                    id INT IDENTITY(1,1) PRIMARY KEY,        -- Unique ID for each expense
                    user_id INT NOT NULL,                     -- Foreign key to associate the expense with a user
                    amount DECIMAL(10,2) NOT NULL,            -- The amount of the expense
                    category NVARCHAR(50) NOT NULL,           -- Category of the expense (e.g., "Food", "Transport")
                    description NVARCHAR(255) NULL,           -- Optional description for the expense
                    date DATE DEFAULT GETDATE(),              -- Date of the expense, defaulting to the current date
                    FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE  -- Ensure expenses are deleted if the user is deleted
                )
            """);

            // Budgets Table: stores user budgets including monthly budget and alert threshold
            stmt.execute("""
                IF NOT EXISTS (SELECT * FROM sys.tables WHERE name='Budgets')
                CREATE TABLE Budgets (
                    id INT IDENTITY(1,1) PRIMARY KEY,        -- Unique ID for each budget
                    user_id INT NOT NULL,                     -- Foreign key to associate the budget with a user
                    monthly_budget DECIMAL(10,2) NOT NULL,    -- Monthly budget amount for the user
                    alert_threshold DECIMAL(5,2) DEFAULT 80,  -- Threshold percentage for alerting the user (default is 80%)
                    FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE  -- Ensure budgets are deleted if the user is deleted
                )
            """);

            // Exchange Rates Table: stores live exchange rates for different currencies
            stmt.execute("""
                IF NOT EXISTS (SELECT * FROM sys.tables WHERE name='ExchangeRates')
                CREATE TABLE ExchangeRates (
                    id INT IDENTITY(1,1) PRIMARY KEY,        -- Unique ID for each exchange rate entry
                    currency_code NVARCHAR(10) NOT NULL UNIQUE, -- Currency code (e.g., "USD", "EUR")
                    rate_to_base DECIMAL(10,6) NOT NULL,       -- Exchange rate to the base currency
                    last_updated DATETIME DEFAULT GETDATE()    -- Timestamp for when the rate was last updated
                )
            """);

            // Currency Cache Table: stores the full API response for offline conversion data
            stmt.execute("""
                IF NOT EXISTS (SELECT * FROM sys.tables WHERE name='CurrencyCache')
                CREATE TABLE CurrencyCache (
                    id INT IDENTITY(1,1) PRIMARY KEY,        -- Unique ID for the cache entry
                    rates NVARCHAR(MAX) NOT NULL,             -- Full JSON response containing exchange rates
                    date_saved DATETIME DEFAULT GETDATE()     -- Timestamp of when the data was saved
                )
            """);

            // print a message indicating that the database setup was successful
            System.out.println("✅ Database setup complete!");

        } catch (SQLException e) {
            // print an error message and stack trace if the database setup fails
            System.err.println("❌ Database setup failed!");
            e.printStackTrace();
        }
    }
}
