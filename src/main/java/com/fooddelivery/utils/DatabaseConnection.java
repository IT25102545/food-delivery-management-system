package com.fooddelivery.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static DatabaseConnection instance;
    private Connection connection;

    private static final String URL = "jdbc:mysql://mysql-online-food-delivery-system-online-food-delivery-system.h.aivencloud.com:15420/defaultdb?sslMode=REQUIRED";
    private static final String USER = "avnadmin";
    private static final String PASSWORD = "YOUR_AIVEN_PASSWORD_HERE";

    private DatabaseConnection() {
        connect();
    }

    private void connect() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            this.connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Successfully connected to the database.");
        } catch (Exception e) {
            System.err.println("Database connection failed. Continuing without DB for UI verification: " + e.getMessage());
            this.connection = null;
        }
    }

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    public synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed() || !connection.isValid(2)) {
                System.out.println("Connection dropped or invalid. Reconnecting...");
                connect();
            }
        } catch (SQLException e) {
            System.err.println("Error validating connection: " + e.getMessage());
            connect();
        }
        return connection;
    }
}
