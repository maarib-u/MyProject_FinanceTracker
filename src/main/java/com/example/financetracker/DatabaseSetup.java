package com.example.financetracker;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseSetup {
    public static void main(String[] args) {
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {

            // Create Users table (Uses hashed password storage)
            stmt.execute("""
                IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='Users' AND xtype='U')
                CREATE TABLE Users (
                    id INT IDENTITY(1,1) PRIMARY KEY,
                    username NVARCHAR(50) UNIQUE NOT NULL,
                    password NVARCHAR(255) NOT NULL
                )
            """);

            // Create Expenses table
            stmt.execute("""
                IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='Expenses' AND xtype='U')
                CREATE TABLE Expenses (
                    id INT IDENTITY(1,1) PRIMARY KEY,
                    user_id INT NOT NULL,
                    amount DECIMAL(10,2) NOT NULL,
                    category NVARCHAR(50) NOT NULL,
                    date DATE DEFAULT GETDATE(),
                    FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE
                )
            """);

            // Create CurrencyCache table
            stmt.execute("""
                IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='CurrencyCache' AND xtype='U')
                CREATE TABLE CurrencyCache (
                    id INT IDENTITY(1,1) PRIMARY KEY,
                    rates NVARCHAR(MAX) NOT NULL,
                    date_saved DATE DEFAULT GETDATE()
                )
            """);

            System.out.println("✅ Database setup complete!");
        } catch (SQLException e) {
            System.out.println("❌ Database setup failed!");
            e.printStackTrace();
        }
    }
}
