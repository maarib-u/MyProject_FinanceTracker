package com.example.financetracker;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginController {
    @FXML private TextField usernameField; // Text field for entering the username
    @FXML private PasswordField passwordField; // Password field for entering the password

    // method to handle user registration
    @FXML
    private void handleRegister() {
        String username = usernameField.getText(); // get the username entered
        String password = passwordField.getText(); // get the password entered

        // check if username or password fields are empty
        if (username.isEmpty() || password.isEmpty()) {
            showAlert("❌ Error", "Please fill in all fields."); // show alert if empty
            return;
        }

        // check password strength
        if (!isValidPassword(password)) {
            showAlert("❌ Error", "Password must be at least 8 characters long and contain a number and a special character.");
            return;
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement checkUser = conn.prepareStatement("SELECT id FROM Users WHERE username = ?")) {

            checkUser.setString(1, username); // set the username in the query
            ResultSet rs = checkUser.executeQuery();

            if (rs.next()) {
                showAlert("❌ Error", "User already exists."); // show alert if user already exists
                return;
            }

            // hash the password for secure storage
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
            try (PreparedStatement pstmt = conn.prepareStatement("INSERT INTO Users (username, password) VALUES (?, ?)")) {
                pstmt.setString(1, username); // set the username in the query
                pstmt.setString(2, hashedPassword); // set the hashed password in the query
                pstmt.executeUpdate();
                showAlert("✅ Registration Successful", "You can now log in.");
            }
        } catch (SQLException e) {
            showAlert("❌ Database Error", "Registration failed.");
            e.printStackTrace();
        }
    }

    // method to handle user login
    @FXML
    private void handleLogin() {
        String username = usernameField.getText(); // get the username entered
        String password = passwordField.getText(); // get the password entered

        // check if username or password fields are empty
        if (username.isEmpty() || password.isEmpty()) {
            showAlert("❌ Error", "Please fill in all fields."); // show alert if empty
            return;
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT id, password FROM Users WHERE username = ?")) {

            pstmt.setString(1, username); // set the username in the query
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String storedPassword = rs.getString("password"); // retrieve stored hashed password
                int userId = rs.getInt("id");

                // check if entered password matches the stored hashed password
                if (BCrypt.checkpw(password, storedPassword)) {
                    showAlert("✅ Login Successful", "Welcome, " + username + "!");
                    System.out.println("DEBUG: Logged-in user ID - " + userId);

                    // pass user ID to the main scene
                    SceneController sceneController = new SceneController(SceneManager.getPrimaryStage());
                    sceneController.switchToSceneWithUser("main.fxml", userId);
                } else {
                    showAlert("❌ Login Failed", "Invalid username or password.");
                }
            } else {
                showAlert("❌ Login Failed", "Invalid username or password.");
            }
        } catch (SQLException e) {
            showAlert("❌ Database Error", "Login failed.");
            e.printStackTrace();
        }
    }

    // method to show alerts with a title and message
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.show();
    }

    // utility method to validate the password
    private boolean isValidPassword(String password) {
        // check if password has at least 8 characters, one digit, and one special character
        return password.length() >= 8 && password.matches(".*\\d.*") && password.matches(".*[!@#$%^&*(),.?\":{}|<>].*");
    }
}
