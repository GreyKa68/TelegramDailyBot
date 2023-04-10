package com.example.telegramdailybot.model;

public class ParseResult {
    private Notification notification;
    private String errorMessage;

    // Constructors, getters, and setters
    public ParseResult(Notification notification) {
        this.notification = notification;
    }

    public ParseResult(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    public ParseResult(Notification notification, String errorMessage) {
        this.notification = notification;
        this.errorMessage = errorMessage;
    }
    public Notification getNotification() {
        return notification;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean hasError() {
        return errorMessage != null;
    }
}

