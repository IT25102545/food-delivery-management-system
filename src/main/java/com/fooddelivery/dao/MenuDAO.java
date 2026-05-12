package com.fooddelivery.dao;

import com.fooddelivery.models.MenuItem;
import com.fooddelivery.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) for Menu Items.
 * This class handles all CRUD (Create, Read, Update, Delete) operations,
 * communicating directly with the Aiven Cloud MySQL database.
 * If the cloud database is unavailable, it uses a fallback in-memory list.
 */
public class MenuDAO {

    private static final List<MenuItem> fallbackItems = new ArrayList<>();
    private static int fallbackIdCounter = 1;

    /**
     * Initializes the menu_items table in the database if it doesn't already exist.
     * Ensures all necessary columns (id, name, description, price, image_url) are present.
     */
    public void initializeTable() {
        String sql = "CREATE TABLE IF NOT EXISTS menu_items (" +
                     "id INT AUTO_INCREMENT PRIMARY KEY, " +
                     "name VARCHAR(255) NOT NULL, " +
                     "description TEXT, " +
                     "price DECIMAL(10,2) NOT NULL" +
                     ")";
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            if (conn == null) return;
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
                // Attempt to add the image_url column if it doesn't exist yet
                try {
                    stmt.execute("ALTER TABLE menu_items ADD COLUMN image_url VARCHAR(512)");
                } catch (SQLException ignored) {
                    // Column already exists
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves all menu items from the database.
     * @return A list of MenuItem objects to be displayed on the frontend menu.
     */
    public List<MenuItem> getAllMenuItems() {
        List<MenuItem> items = new ArrayList<>();
        String sql = "SELECT * FROM menu_items";
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            if (conn == null) return new ArrayList<>(fallbackItems);
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    items.add(new MenuItem(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getDouble("price"),
                        rs.getString("image_url")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    /**
     * Adds a new product to the database.
     * @param item The MenuItem object containing the new product details.
     * @return true if the insertion was successful, false otherwise.
     */
    public boolean addMenuItem(MenuItem item) {
        String sql = "INSERT INTO menu_items (name, description, price, image_url) VALUES (?, ?, ?, ?)";
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            if (conn == null) {
                item.setId(fallbackIdCounter++);
                fallbackItems.add(item);
                return true;
            }
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, item.getName());
                pstmt.setString(2, item.getDescription());
                pstmt.setDouble(3, item.getPrice());
                pstmt.setString(4, item.getImageUrl());
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Deletes a product from the database by its ID.
     * @param id The unique identifier of the menu item to remove.
     * @return true if the deletion was successful, false otherwise.
     */
    public boolean deleteMenuItem(int id) {
        String sql = "DELETE FROM menu_items WHERE id = ?";
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            if (conn == null) {
                return fallbackItems.removeIf(i -> i.getId() == id);
            }
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, id);
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Updates an existing product's details in the database.
     * @param item The MenuItem object containing updated information.
     * @return true if the update was successful, false otherwise.
     */
    public boolean updateMenuItem(MenuItem item) {
        String sql = "UPDATE menu_items SET name=?, description=?, price=?, image_url=? WHERE id=?";
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            if (conn == null) {
                for (int i = 0; i < fallbackItems.size(); i++) {
                    if (fallbackItems.get(i).getId() == item.getId()) {
                        fallbackItems.set(i, item);
                        return true;
                    }
                }
                return false;
            }
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, item.getName());
                pstmt.setString(2, item.getDescription());
                pstmt.setDouble(3, item.getPrice());
                pstmt.setString(4, item.getImageUrl());
                pstmt.setInt(5, item.getId());
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}

// Menu retrieval logic verified
