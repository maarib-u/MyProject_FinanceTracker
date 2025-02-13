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
    // fields to hold UI components
    @FXML private TextField budgetField;  // budget input field
    @FXML private Label budgetStatusLabel; // label showing budget status
    @FXML private PieChart budgetChart;  // pie chart for visualising budget data

    private int userId;  // stores the user id

    // sets the user id, loads budget and updates the chart
    public void setUserId(int userId) {
        this.userId = userId;
        System.out.println("Debug: user id set to " + userId);
        loadBudget();
        updateChart();
    }

    // sets up the tooltip for the budget field
    @FXML
    public void initialize() {
        System.out.println("Debug: controller initialized.");
        // tooltip for the budget input field
        Tooltip tooltip = new Tooltip("Enter your monthly budget in £. positive values only.");
        tooltip.setStyle("-fx-background-color: #333333; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10px;");
        budgetField.setTooltip(tooltip);
    }

    // handles setting the budget when user clicks "set budget"
    @FXML
    private void handleSetBudget() {
        String budgetText = budgetField.getText();  // get text from the field

        // check if input is empty or invalid
        if (budgetText.isEmpty() || !isValidBudget(budgetText)) {
            showAlert("❌ Error", "Please enter a valid budget amount.");
            return;
        }

        double budget = Double.parseDouble(budgetText);  // convert text to number

        // check if budget is positive
        if (budget <= 0) {
            showAlert("❌ Error", "Budget must be a positive value.");
            return;
        }

        // ask user for confirmation before updating
        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.setTitle("Confirm budget update");
        confirmationAlert.setHeaderText("Are you sure you want to update your budget?");
        confirmationAlert.setContentText("Your new budget will be set to: £" + budget);
        Optional<ButtonType> result = confirmationAlert.showAndWait();  // wait for response

        // if confirmed, update the budget
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try (Connection conn = DatabaseManager.getConnection()) {
                PreparedStatement pstmt = conn.prepareStatement(
                        "MERGE INTO Budgets AS target " +
                                "USING (SELECT ? AS user_id, ? AS budget) AS source " +
                                "ON target.user_id = source.user_id " +
                                "WHEN MATCHED THEN UPDATE SET monthly_budget = source.budget " +
                                "WHEN NOT MATCHED THEN INSERT (user_id, monthly_budget) VALUES (source.user_id, source.budget);");

                pstmt.setInt(1, userId);  // set user id
                pstmt.setDouble(2, budget);  // set new budget value
                pstmt.executeUpdate();  // execute the update

                showAlert("✅ Success", "Budget updated successfully!");
                loadBudget();  // reload budget data
                updateChart();  // update chart with new budget
            } catch (SQLException e) {
                showAlert("❌ Error", "Database error occurred.");
                e.printStackTrace();
            }
        }
    }

    // loads the current budget from the database
    private void loadBudget() {
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement("SELECT monthly_budget FROM Budgets WHERE user_id = ?");
            pstmt.setInt(1, userId);  // set user id in the query
            ResultSet rs = pstmt.executeQuery();  // execute the query

            // if budget exists, show it in the field and label
            if (rs.next()) {
                budgetField.setText(String.valueOf(rs.getDouble("monthly_budget")));
                budgetStatusLabel.setText("✅ Budget loaded successfully.");
            } else {
                // if no budget set, clear field and show warning
                budgetField.setText("");
                budgetStatusLabel.setText("⚠️ No budget set.");
            }
        } catch (SQLException e) {
            showAlert("❌ Error", "Could not load budget.");
            e.printStackTrace();
        }
    }

    // updates the pie chart based on the current budget and expenses
    private void updateChart() {
        budgetChart.getData().clear();  // clear previous chart data

        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT SUM(amount) as total_spent FROM Expenses WHERE user_id = ?");
            pstmt.setInt(1, userId);  // set user id in the query
            ResultSet rs = pstmt.executeQuery();  // execute the query

            double totalSpent = 0;
            if (rs.next()) {
                totalSpent = rs.getDouble("total_spent");  // get total spent amount
            }

            double totalBudget = Double.parseDouble(budgetField.getText());  // get budget from field

            if (totalBudget > 0) {
                // calculate remaining budget and percentages
                double remaining = totalBudget - totalSpent;
                double spentPercentage = (totalSpent / totalBudget) * 100;
                double remainingPercentage = (remaining / totalBudget) * 100;

                // create PieChart data for spent and remaining budget
                PieChart.Data spentData = new PieChart.Data("Spent: £" + totalSpent + " (" + String.format("%.2f", spentPercentage) + "%)", totalSpent);
                PieChart.Data remainingData = new PieChart.Data("Remaining: £" + remaining + " (" + String.format("%.2f", remainingPercentage) + "%)", remaining);

                budgetChart.getData().addAll(spentData, remainingData);  // add data to chart

                // set chart slice colours
                spentData.getNode().setStyle("-fx-pie-color: #e74c3c;");
                remainingData.getNode().setStyle("-fx-pie-color: #2ecc71;");

                // tooltips for chart slices on hover
                final String spentTooltipText = "Spent: £" + totalSpent + " (" + String.format("%.2f", spentPercentage) + "%)";
                final String remainingTooltipText = "Remaining: £" + remaining + " (" + String.format("%.2f", remainingPercentage) + "%)";

                spentData.getNode().setOnMouseEntered(e -> {
                    Tooltip tooltip = new Tooltip(spentTooltipText);  // create tooltip
                    Tooltip.install(spentData.getNode(), tooltip);  // attach tooltip to slice
                });
                remainingData.getNode().setOnMouseEntered(e -> {
                    Tooltip tooltip = new Tooltip(remainingTooltipText);  // create tooltip
                    Tooltip.install(remainingData.getNode(), tooltip);  // attach tooltip to slice
                });

                // add smooth animation to PieChart after update
                PauseTransition pause = new PauseTransition(Duration.seconds(0.5));
                pause.setOnFinished(event -> budgetChart.layout());  // refresh chart layout
                pause.play();
            }
        } catch (SQLException | NumberFormatException e) {
            showAlert("❌ Error", "Could not update budget chart.");
            e.printStackTrace();
        }

        budgetChart.setLabelsVisible(true);  // make labels visible
        budgetChart.setLegendVisible(true);  // make legend visible
        budgetChart.layout();  // refresh chart layout
    }

    // handle the back button to return to main scene
    @FXML
    private void handleBack() {
        SceneController sceneController = new SceneController(SceneManager.getPrimaryStage());
        sceneController.switchToSceneWithUser("main.fxml", userId);  // switch to main scene
    }

    // shows an alert to the user
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);  // set the title of the alert
        alert.setHeaderText(null);  // remove header for cleaner look
        alert.setContentText(message);  // set content message

        ButtonType okButton = ButtonType.OK;  // OK button for alert
        alert.getButtonTypes().setAll(okButton);  // set the button type
        alert.showAndWait();  // show alert and wait for user action
    }

    // check if the budget input is a valid number
    private boolean isValidBudget(String budgetText) {
        try {
            Double.parseDouble(budgetText);  // try to convert text to number
            return true;  // valid number
        } catch (NumberFormatException e) {
            return false;  // invalid number
        }
    }
}
