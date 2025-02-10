
package com.example.financetracker;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginScreen extends Application {
    private Connection conn = DatabaseManager.getConnection(); // ✅ Directly initialize connection

    @Override
    public void start(Stage primaryStage) {
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));

        Label userLabel = new Label("Username:");
        TextField userField = new TextField();

        Label passLabel = new Label("Password:");
        PasswordField passField = new PasswordField();

        Button loginButton = new Button("Login");
        Button registerButton = new Button("Create Account");

        root.getChildren().addAll(userLabel, userField, passLabel, passField, loginButton, registerButton);
        Scene loginScene = new Scene(root, 300, 200);
        primaryStage.setScene(loginScene);
        primaryStage.setTitle("Login / Register");
        primaryStage.show();

        // 🔹 Login function
        loginButton.setOnAction(e -> {
            String username = userField.getText();
            String password = passField.getText();
            if (validateLogin(username, password)) {
                ExpenseTracker expenseTracker = new ExpenseTracker(username);
                try {
                    expenseTracker.start(primaryStage);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else {
                showAlert("❌ Login Failed", "Incorrect username or password.");
            }
        });

        // 🔹 Register function
        registerButton.setOnAction(e -> {
            String username = userField.getText();
            String password = passField.getText();
            if (!username.isEmpty() && !password.isEmpty()) {
                if (registerUser(username, password)) {
                    showAlert("✅ Registration Successful", "You can now log in.");
                } else {
                    showAlert("❌ Error", "User already exists.");
                }
            } else {
                showAlert("❌ Error", "Please enter a username and password.");
            }
        });
    }

    // ✅ Secure password hashing with BCrypt
    private boolean registerUser(String username, String password) {
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement("INSERT INTO Users (username, password) VALUES (?, ?)");
            pstmt.setString(1, username);
            pstmt.setString(2, hashedPassword);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    // ✅ Secure password validation with BCrypt
    private boolean validateLogin(String username, String password) {
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement("SELECT password FROM Users WHERE username = ?");
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String storedPassword = rs.getString("password");
                return BCrypt.checkpw(password, storedPassword); // ✅ Compare hashed passwords
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.show();
    }
}
