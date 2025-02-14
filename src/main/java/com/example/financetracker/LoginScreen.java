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
    private final Connection conn = DatabaseManager.getConnection(); // initialize connection to the database

    @Override
    public void start(Stage primaryStage) {
        // save primary stage in SceneManager
        SceneManager.setPrimaryStage(primaryStage);

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

        // login function
        loginButton.setOnAction(e -> {
            String username = userField.getText(); // get the entered username
            String password = passField.getText(); // get the entered password
            if (validateLogin(username, password)) {
                // use SceneController to switch scenes instead of directly calling ExpenseTracker
                SceneController sceneController = new SceneController(SceneManager.getPrimaryStage());
                sceneController.switchToScene("expense_tracker.fxml");
            } else {
                showAlert("❌ Login Failed", "Incorrect username or password.");
            }
        });

        // register function
        registerButton.setOnAction(e -> {
            String username = userField.getText(); // get the entered username
            String password = passField.getText(); // get the entered password
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

    // method to register a user
    private boolean registerUser(String username, String password) {
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt()); // hash the password

        try (PreparedStatement pstmt = conn.prepareStatement("INSERT INTO Users (username, password) VALUES (?, ?)")) {
            pstmt.setString(1, username);
            pstmt.setString(2, hashedPassword);
            pstmt.executeUpdate(); // insert new user into the database
            return true;
        } catch (SQLException e) {
            return false; // user already exists, return false
        }
    }

    // method to validate user login credentials
    private boolean validateLogin(String username, String password) {
        try (PreparedStatement pstmt = conn.prepareStatement("SELECT password FROM Users WHERE username = ?")) {
            pstmt.setString(1, username); // set the username in the query
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String storedPassword = rs.getString("password"); // retrieve stored password
                return BCrypt.checkpw(password, storedPassword); // compare hashed password
            }
        } catch (SQLException e) {
            e.printStackTrace(); // handle database errors
        }
        return false; // return false if login fails
    }

    // method to show alerts
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.show(); // display alert
    }

    public static void main(String[] args) {
        launch(args);
    }
}
