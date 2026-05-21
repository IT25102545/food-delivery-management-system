package com.fooddelivery.utils;

import java.sql.Connection;
import java.sql.Statement;

public class ClearDB {
    public static void main(String[] args) {
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            if (conn != null) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("DROP TABLE IF EXISTS `delivery_users`");
                    System.out.println("Table dropped successfully.");
                }
            } else {
                System.out.println("Connection is null");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
