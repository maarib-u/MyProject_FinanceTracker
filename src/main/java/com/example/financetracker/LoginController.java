package com.example.financetracker;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.mindrot.jbcrypt.BCrypt;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;

    @FXML
    private void handleRegister() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert("❌ Error", "Please fill in all fields.");
            return;
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement checkUser = conn.prepareStatement("SELECT id FROM Users WHERE username = ?")) {

            checkUser.setString(1, username);
            ResultSet rs = checkUser.executeQuery();

            if (rs.next()) {
                showAlert("❌ Error", "User already exists.");
                return;
            }

            // Hash password & insert new user
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
            try (PreparedStatement pstmt = conn.prepareStatement("INSERT INTO Users (username, password) VALUES (?, ?)")) {
                pstmt.setString(1, username);
                pstmt.setString(2, hashedPassword);
                pstmt.executeUpdate();
                showAlert("✅ Registration Successful", "You can now log in.");
            }
        } catch (SQLException e) {
            showAlert("❌ Database Error", "Registration failed.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert("❌ Error", "Please fill in all fields.");
            return;
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT id, password FROM Users WHERE username = ?")) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                int userId = rs.getInt("id");
                String storedPassword = rs.getString("password");

                if (BCrypt.checkpw(password, storedPassword)) {
                    showAlert("✅ Login Successful", "Welcome, " + username + "!");
                    System.out.println("DEBUG: Logged-in user ID - " + userId);

                    // Pass user ID to the main scene
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

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.show();
    }
}
