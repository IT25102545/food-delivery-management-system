package com.fooddelivery.utils;

import java.sql.Connection;

public class TestConnectionMain {
    public static void main(String[] args) {
        System.out.println("Testing database connection...");
        DatabaseConnection db = DatabaseConnection.getInstance();
        Connection conn = db.getConnection();
        if (conn != null) {
            System.out.println("SUCCESS: Connected to Aiven database!");
        } else {
            System.out.println("FAILURE: Could not connect to Aiven database.");
        }
    }
}
