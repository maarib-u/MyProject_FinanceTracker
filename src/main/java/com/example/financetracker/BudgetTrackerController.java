package com.example.financetracker;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class BudgetTrackerController {
    @FXML private TextField budgetField;
    @FXML private Label budgetStatusLabel;
    @FXML private PieChart budgetChart;

    private int userId;

    public void setUserId(int userId) {
        this.userId = userId;
        System.out.println("DEBUG: BudgetTrackerController - User ID set to " + userId);
        loadBudget();
        updateChart();
    }

    @FXML
    public void initialize() {
        System.out.println("DEBUG: BudgetTrackerController initialized.");
        Tooltip tooltip = new Tooltip("Enter your monthly budget in £. Positive values only.");
        tooltip.setStyle("-fx-background-color: #333333; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10px;");
        budgetField.setTooltip(tooltip);
    }

    @FXML
    private void handleSetBudget() {
        String budgetText = budgetField.getText();

        if (budgetText.isEmpty() || !isValidBudget(budgetText)) {
            showAlert("❌ Error", "Please enter a valid budget amount.");
            return;
        }

        double budget = Double.parseDouble(budgetText);

        if (budget <= 0) {
            showAlert("❌ Error", "Budget must be a positive value.");
            return;
        }

        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.setTitle("Confirm Budget Update");
        confirmationAlert.setHeaderText("Are you sure you want to update your budget?");
        confirmationAlert.setContentText("Your new budget will be set to: £" + budget);
        Optional<ButtonType> result = confirmationAlert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try (Connection conn = DatabaseManager.getConnection()) {
                PreparedStatement pstmt = conn.prepareStatement(
                        "MERGE INTO Budgets AS target " +
                                "USING (SELECT ? AS user_id, ? AS budget) AS source " +
                                "ON target.user_id = source.user_id " +
                                "WHEN MATCHED THEN UPDATE SET monthly_budget = source.budget " +
                                "WHEN NOT MATCHED THEN INSERT (user_id, monthly_budget) VALUES (source.user_id, source.budget);");

                pstmt.setInt(1, userId);
                pstmt.setDouble(2, budget);
                pstmt.executeUpdate();

                showAlert("✅ Success", "Budget updated successfully!");
                loadBudget();
                updateChart();
            } catch (SQLException e) {
                showAlert("❌ Error", "Database error occurred.");
                e.printStackTrace();
            }
        }
    }

    private void loadBudget() {
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement("SELECT monthly_budget FROM Budgets WHERE user_id = ?");
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                budgetField.setText(String.valueOf(rs.getDouble("monthly_budget")));
                budgetStatusLabel.setText("✅ Budget loaded successfully.");
            } else {
                budgetField.setText("");
                budgetStatusLabel.setText("⚠️ No budget set.");
            }
        } catch (SQLException e) {
            showAlert("❌ Error", "Could not load budget.");
            e.printStackTrace();
        }
    }

    private void updateChart() {
        budgetChart.getData().clear();

        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT SUM(amount) as total_spent FROM Expenses WHERE user_id = ?");
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            double totalSpent = 0;
            if (rs.next()) {
                totalSpent = rs.getDouble("total_spent");
            }

            double totalBudget = Double.parseDouble(budgetField.getText());

            if (totalBudget > 0) {
                double remaining = totalBudget - totalSpent;
                double spentPercentage = (totalSpent / totalBudget) * 100;
                double remainingPercentage = (remaining / totalBudget) * 100;

                PieChart.Data spentData = new PieChart.Data("Spent: £" + totalSpent + " (" + String.format("%.2f", spentPercentage) + "%)", totalSpent);
                PieChart.Data remainingData = new PieChart.Data("Remaining: £" + remaining + " (" + String.format("%.2f", remainingPercentage) + "%)", remaining);

                budgetChart.getData().addAll(spentData, remainingData);

                spentData.getNode().setStyle("-fx-pie-color: #e74c3c;");
                remainingData.getNode().setStyle("-fx-pie-color: #2ecc71;");

                final String spentTooltipText = "Spent: £" + totalSpent + " (" + String.format("%.2f", spentPercentage) + "%)";
                final String remainingTooltipText = "Remaining: £" + remaining + " (" + String.format("%.2f", remainingPercentage) + "%)";

                spentData.getNode().setOnMouseEntered(e -> {
                    Tooltip tooltip = new Tooltip(spentTooltipText);
                    Tooltip.install(spentData.getNode(), tooltip);
                });
                remainingData.getNode().setOnMouseEntered(e -> {
                    Tooltip tooltip = new Tooltip(remainingTooltipText);
                    Tooltip.install(remainingData.getNode(), tooltip);
                });

                PauseTransition pause = new PauseTransition(Duration.seconds(0.5));
                pause.setOnFinished(event -> budgetChart.layout());
                pause.play();
            }
        } catch (SQLException | NumberFormatException e) {
            showAlert("❌ Error", "Could not update budget chart.");
            e.printStackTrace();
        }

        budgetChart.setLabelsVisible(true);
        budgetChart.setLegendVisible(true);
        budgetChart.layout();
    }

    @FXML
    private void handleBack() {
        SceneController sceneController = new SceneController(SceneManager.getPrimaryStage());
        sceneController.switchToSceneWithUser("main.fxml", userId);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null); // Remove header for a cleaner look
        alert.setContentText(message);

        ButtonType okButton = ButtonType.OK;
        alert.getButtonTypes().setAll(okButton);
        alert.showAndWait();
    }

    private boolean isValidBudget(String budgetText) {
        try {
            Double.parseDouble(budgetText);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
