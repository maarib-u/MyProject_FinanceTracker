package com.example.financetracker;

public class Expense {
    private final String category;
    private final double amount;
    private final String date;
    private final String time;

    public Expense(String category, double amount, String date, String time) {
        this.category = category;
        this.amount = amount;
        this.date = date;
        this.time = time;
    }

    public String getCategory() { return category; }
    public double getAmount() { return amount; }
    public String getDate() { return date; }
    public String getTime() { return time; }
}
