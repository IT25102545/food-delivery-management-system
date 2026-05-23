package com.fooddelivery.dao;

import com.fooddelivery.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserDAO {

    private static final List<Map<String, Object>> fallbackUsers = new ArrayList<>();
    private static int fallbackIdCounter = 1;

    public void initializeTable() {
        String sql = "CREATE TABLE IF NOT EXISTS `delivery_users` (" +
                     "`id` INT AUTO_INCREMENT PRIMARY KEY, " +
                     "`username` VARCHAR(255) UNIQUE NOT NULL, " +
                     "`password` VARCHAR(255) NOT NULL, " +
                     "`role` VARCHAR(50) NOT NULL, " +
                     "`status` VARCHAR(50) NOT NULL, " +
                     "`full_name` VARCHAR(255), " +
                     "`mobile` VARCHAR(50), " +
                     "`address` TEXT, " +
                     "`nic` VARCHAR(50), " +
                     "`profile_pic` VARCHAR(512), " +
                     "`total_deliveries` INT DEFAULT 0, " +
                     "`total_earned` DECIMAL(10,2) DEFAULT 0.0, " +
                     "`current_balance` DECIMAL(10,2) DEFAULT 0.0, " +
                     "`is_online` BOOLEAN DEFAULT FALSE" +
                     ")";
                     
        String payoutTable = "CREATE TABLE IF NOT EXISTS `payout_history` (" +
                     "`id` BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                     "`rider_id` INT NOT NULL, " +
                     "`amount_paid` DECIMAL(10,2) NOT NULL, " +
                     "`paid_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                     "`payment_method` VARCHAR(50) NOT NULL" +
                     ")";
                     
        String transactionsTable = "CREATE TABLE IF NOT EXISTS `transactions` (" +
                     "`id` BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                     "`user_id` INT NOT NULL, " +
                     "`type` VARCHAR(50) NOT NULL, " +
                     "`amount` DECIMAL(10,2) NOT NULL, " +
                     "`order_id` INT, " +
                     "`description` VARCHAR(255), " +
                     "`created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                     ")";
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            if (conn == null) return;
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
                stmt.execute(payoutTable);
                stmt.execute(transactionsTable);
                
                // attempt to add new columns for existing tables
                String[] alterStatements = {
                    "ALTER TABLE `delivery_users` ADD COLUMN `full_name` VARCHAR(255)",
                    "ALTER TABLE `delivery_users` ADD COLUMN `mobile` VARCHAR(50)",
                    "ALTER TABLE `delivery_users` ADD COLUMN `address` TEXT",
                    "ALTER TABLE `delivery_users` ADD COLUMN `nic` VARCHAR(50)",
                    "ALTER TABLE `delivery_users` ADD COLUMN `profile_pic` VARCHAR(512)",
                    "ALTER TABLE `delivery_users` ADD COLUMN `total_deliveries` INT DEFAULT 0",
                    "ALTER TABLE `delivery_users` ADD COLUMN `total_earned` DECIMAL(10,2) DEFAULT 0.0",
                    "ALTER TABLE `delivery_users` ADD COLUMN `current_balance` DECIMAL(10,2) DEFAULT 0.0",
                    "ALTER TABLE `delivery_users` ADD COLUMN `is_online` BOOLEAN DEFAULT FALSE",
                    "ALTER TABLE `delivery_users` ADD COLUMN `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP",
                    "ALTER TABLE `delivery_users` ADD COLUMN `zestro_credits` DECIMAL(10,2) DEFAULT 0.0",
                    "ALTER TABLE `delivery_users` ADD COLUMN `saved_cards` TEXT",
                    "ALTER TABLE `delivery_users` ADD COLUMN `unique_id` VARCHAR(50)",
                    "ALTER TABLE `delivery_users` ADD COLUMN `nic_image` VARCHAR(512)"
                };
                for (String alter : alterStatements) {
                    try { stmt.execute(alter); } catch (SQLException ignored) {}
                }

                // seed an admin user if not exists
                try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM `delivery_users` WHERE `role`='ADMIN'")) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        stmt.execute("INSERT INTO `delivery_users` (`username`, `password`, `role`, `status`) VALUES ('admin', 'admin', 'ADMIN', 'APPROVED')");
                    }
                }
                
                // seed a customer user if not exists
                try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM `delivery_users` WHERE `role`='CUSTOMER'")) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        stmt.execute("INSERT INTO `delivery_users` (`username`, `password`, `role`, `status`, `full_name`, `mobile`, `address`) VALUES ('customer@zestro.com', 'password123', 'CUSTOMER', 'APPROVED', 'Demo Customer', '+1 (555) 123-4567', '123 Zestro Ave, Food City')");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        if (fallbackUsers.isEmpty()) {
            Map<String, Object> admin = new HashMap<>();
            admin.put("id", fallbackIdCounter++);
            admin.put("username", "admin");
            admin.put("password", "admin");
            admin.put("role", "ADMIN");
            admin.put("status", "APPROVED");
            fallbackUsers.add(admin);
        }
    }

    public boolean registerUser(String username, String password, String role, String fullName, String mobile, String address, String nic, String profilePic, String nicImage) {
        String status = "RIDER".equalsIgnoreCase(role) ? "PENDING" : "APPROVED";
        String uniqueId = "RIDER".equalsIgnoreCase(role) ? "RID-" + System.currentTimeMillis() : null;
        
        String sql = "INSERT INTO `delivery_users` (`username`, `password`, `role`, `status`, `full_name`, `mobile`, `address`, `nic`, `profile_pic`, `unique_id`, `nic_image`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            if (conn == null) {
                // fallback check if user exists
                for (Map<String, Object> u : fallbackUsers) {
                    if (u.get("username").equals(username)) return false;
                }
                Map<String, Object> newUser = new HashMap<>();
                newUser.put("id", fallbackIdCounter++);
                newUser.put("username", username);
                newUser.put("password", password);
                newUser.put("role", role);
                newUser.put("status", status);
                newUser.put("full_name", fullName);
                newUser.put("mobile", mobile);
                newUser.put("address", address);
                newUser.put("nic", nic);
                newUser.put("profile_pic", profilePic);
                newUser.put("unique_id", uniqueId);
                newUser.put("nic_image", nicImage);
                newUser.put("total_deliveries", 0);
                newUser.put("total_earned", 0.0);
                newUser.put("current_balance", 0.0);
                newUser.put("zestro_credits", 0.0);
                newUser.put("saved_cards", "[]");
                newUser.put("is_online", false);
                fallbackUsers.add(newUser);
                return true;
            }
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                pstmt.setString(2, password);
                pstmt.setString(3, role);
                pstmt.setString(4, status);
                pstmt.setString(5, fullName);
                pstmt.setString(6, mobile);
                pstmt.setString(7, address);
                pstmt.setString(8, nic);
                pstmt.setString(9, profilePic);
                pstmt.setString(10, uniqueId);
                pstmt.setString(11, nicImage);
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Map<String, Object> authenticateUser(String username, String password) {
        String sql = "SELECT * FROM `delivery_users` WHERE `username` = ? AND `password` = ?";
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            if (conn == null) {
                for (Map<String, Object> u : fallbackUsers) {
                    if (u.get("username").equals(username) && u.get("password").equals(password)) {
                        Map<String, Object> result = new HashMap<>(u);
                        result.remove("password"); // don't return password
                        return result;
                    }
                }
                return null;
            }
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                pstmt.setString(2, password);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        Map<String, Object> user = new HashMap<>();
                        user.put("id", rs.getInt("id"));
                        user.put("username", rs.getString("username"));
                        user.put("role", rs.getString("role"));
                        user.put("status", rs.getString("status"));
                        user.put("full_name", rs.getString("full_name"));
                        user.put("mobile", rs.getString("mobile"));
                        user.put("address", rs.getString("address"));
                        user.put("profile_pic", rs.getString("profile_pic"));
                        user.put("zestro_credits", rs.getDouble("zestro_credits"));
                        user.put("saved_cards", rs.getString("saved_cards"));
                        return user;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Map<String, Object> getUserById(int id) {
        String sql = "SELECT * FROM `delivery_users` WHERE `id` = ?";
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            if (conn == null) {
                for (Map<String, Object> u : fallbackUsers) {
                    if ((int)u.get("id") == id) {
                        Map<String, Object> result = new HashMap<>(u);
                        result.remove("password");
                        return result;
                    }
                }
                return null;
            }
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, id);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        Map<String, Object> user = new HashMap<>();
                        user.put("id", rs.getInt("id"));
                        user.put("username", rs.getString("username"));
                        user.put("role", rs.getString("role"));
                        user.put("status", rs.getString("status"));
                        user.put("full_name", rs.getString("full_name"));
                        user.put("mobile", rs.getString("mobile"));
                        user.put("address", rs.getString("address"));
                        user.put("profile_pic", rs.getString("profile_pic"));
                        user.put("zestro_credits", rs.getDouble("zestro_credits"));
                        user.put("saved_cards", rs.getString("saved_cards"));
                        return user;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean updateUserProfile(int id, String fullName, String mobile, String address) {
        String sql = "UPDATE `delivery_users` SET `full_name` = ?, `mobile` = ?, `address` = ? WHERE `id` = ?";
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            if (conn == null) {
                for (Map<String, Object> u : fallbackUsers) {
                    if ((int)u.get("id") == id) {
                        u.put("full_name", fullName);
                        u.put("mobile", mobile);
                        u.put("address", address);
                        return true;
                    }
                }
                return false;
            }
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, fullName);
                pstmt.setString(2, mobile);
                pstmt.setString(3, address);
                pstmt.setInt(4, id);
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateAdminSettings(int id, String fullName, String password, String profilePicUrl) {
        // build dynamic sql based on whether password or profilepicurl are provided
        StringBuilder sql = new StringBuilder("UPDATE `delivery_users` SET `full_name` = ?");
        if (password != null && !password.trim().isEmpty()) {
            sql.append(", `password` = ?");
        }
        if (profilePicUrl != null && !profilePicUrl.trim().isEmpty()) {
            sql.append(", `profile_pic` = ?");
        }
        sql.append(" WHERE `id` = ? AND `role` = 'ADMIN'");

        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            if (conn == null) {
                // handle fallback data
                for (Map<String, Object> u : fallbackUsers) {
                    if ((int)u.get("id") == id && "ADMIN".equals(u.get("role"))) {
                        u.put("full_name", fullName);
                        if (password != null && !password.trim().isEmpty()) {
                            u.put("password", password);
                        }
                        if (profilePicUrl != null && !profilePicUrl.trim().isEmpty()) {
                            u.put("profile_pic", profilePicUrl);
                        }
                        return true;
                    }
                }
                return false;
            }

            try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
                int index = 1;
                pstmt.setString(index++, fullName);
                
                if (password != null && !password.trim().isEmpty()) {
                    pstmt.setString(index++, password);
                }
                if (profilePicUrl != null && !profilePicUrl.trim().isEmpty()) {
                    pstmt.setString(index++, profilePicUrl);
                }
                
                pstmt.setInt(index++, id);
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteUser(int id) {
        String sql = "DELETE FROM `delivery_users` WHERE `id` = ?";
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            if (conn == null) {
                return fallbackUsers.removeIf(u -> (int)u.get("id") == id);
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

    public boolean topUpCredits(int userId, double amount) {
        String sql = "UPDATE `delivery_users` SET `zestro_credits` = `zestro_credits` + ? WHERE `id` = ?";
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            if (conn == null) {
                for (Map<String, Object> u : fallbackUsers) {
                    if ((int)u.get("id") == userId) {
                        double current = (double) u.getOrDefault("zestro_credits", 0.0);
                        u.put("zestro_credits", current + amount);
                        return true;
                    }
                }
                return false;
            }
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setDouble(1, amount);
                pstmt.setInt(2, userId);
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean chargeCredits(int userId, double amount) {
        String sql = "UPDATE `delivery_users` SET `zestro_credits` = `zestro_credits` - ? WHERE `id` = ? AND `zestro_credits` >= ?";
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            if (conn == null) {
                for (Map<String, Object> u : fallbackUsers) {
                    if ((int)u.get("id") == userId) {
                        double current = (double) u.getOrDefault("zestro_credits", 0.0);
                        if (current >= amount) {
                            u.put("zestro_credits", current - amount);
                            return true;
                        }
                    }
                }
                return false;
            }
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setDouble(1, amount);
                pstmt.setInt(2, userId);
                pstmt.setDouble(3, amount);
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateSavedCards(int userId, String cardsJson) {
        String sql = "UPDATE `delivery_users` SET `saved_cards` = ? WHERE `id` = ?";
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            if (conn == null) {
                for (Map<String, Object> u : fallbackUsers) {
                    if ((int)u.get("id") == userId) {
                        u.put("saved_cards", cardsJson);
                        return true;
                    }
                }
                return false;
            }
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, cardsJson);
                pstmt.setInt(2, userId);
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Map<String, Object>> getPendingRiders() {
        List<Map<String, Object>> pending = new ArrayList<>();
        String sql = "SELECT `id`, `username`, `role`, `status`, `full_name`, `mobile`, `address`, `nic`, `profile_pic`, `total_deliveries`, `total_earned`, `current_balance`, `is_online`, `unique_id`, `nic_image` FROM `delivery_users` WHERE `role` = 'RIDER' AND `status` = 'PENDING'";
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            if (conn == null) {
                for (Map<String, Object> u : fallbackUsers) {
                    if ("RIDER".equals(u.get("role")) && "PENDING".equals(u.get("status"))) {
                        Map<String, Object> user = new HashMap<>(u);
                        user.remove("password");
                        pending.add(user);
                    }
                }
                return pending;
            }
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    Map<String, Object> user = new HashMap<>();
                    user.put("id", rs.getInt("id"));
                    user.put("username", rs.getString("username"));
                    user.put("role", rs.getString("role"));
                    user.put("status", rs.getString("status"));
                    user.put("full_name", rs.getString("full_name"));
                    user.put("mobile", rs.getString("mobile"));
                    user.put("address", rs.getString("address"));
                    user.put("nic", rs.getString("nic"));
                    user.put("profile_pic", rs.getString("profile_pic"));
                    user.put("total_deliveries", rs.getInt("total_deliveries"));
                    user.put("total_earned", rs.getDouble("total_earned"));
                    user.put("current_balance", rs.getDouble("current_balance"));
                    user.put("is_online", rs.getBoolean("is_online"));
                    user.put("unique_id", rs.getString("unique_id"));
                    user.put("nic_image", rs.getString("nic_image"));
                    pending.add(user);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return pending;
    }

    public List<Map<String, Object>> getCustomers() {
        List<Map<String, Object>> customers = new ArrayList<>();
        String sql = "SELECT `id`, `username`, `role`, `status`, `full_name`, `mobile`, `address`, `created_at` FROM `delivery_users` WHERE `role` = 'CUSTOMER'";
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            if (conn == null) {
                for (Map<String, Object> u : fallbackUsers) {
                    if ("CUSTOMER".equals(u.get("role"))) {
                        Map<String, Object> user = new HashMap<>(u);
                        user.remove("password");
                        customers.add(user);
                    }
                }
                return customers;
            }
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    Map<String, Object> user = new HashMap<>();
                    user.put("id", rs.getInt("id"));
                    user.put("username", rs.getString("username"));
                    user.put("role", rs.getString("role"));
                    user.put("status", rs.getString("status"));
                    user.put("full_name", rs.getString("full_name"));
                    user.put("mobile", rs.getString("mobile"));
                    user.put("address", rs.getString("address"));
                    Timestamp createdAt = null;
                    try { createdAt = rs.getTimestamp("created_at"); } catch (Exception e) {}
                    user.put("created_at", createdAt != null ? createdAt.toString() : null);
                    customers.add(user);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return customers;
    }

    public List<Map<String, Object>> getApprovedRiders() {
        List<Map<String, Object>> approved = new ArrayList<>();
        String sql = "SELECT `id`, `username`, `status`, `full_name`, `mobile`, `address`, `nic`, `profile_pic`, `total_deliveries`, `total_earned`, `current_balance`, `is_online`, `unique_id`, `nic_image` FROM `delivery_users` WHERE `role` = 'RIDER' AND `status` IN ('APPROVED', 'SUSPENDED')";
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            if (conn == null) {
                for (Map<String, Object> u : fallbackUsers) {
                    if ("RIDER".equals(u.get("role")) && ("APPROVED".equals(u.get("status")) || "SUSPENDED".equals(u.get("status")))) {
                        Map<String, Object> user = new HashMap<>(u);
                        user.remove("password");
                        approved.add(user);
                    }
                }
                return approved;
            }
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    Map<String, Object> user = new HashMap<>();
                    user.put("id", rs.getInt("id"));
                    user.put("username", rs.getString("username"));
                    user.put("status", rs.getString("status"));
                    user.put("full_name", rs.getString("full_name"));
                    user.put("mobile", rs.getString("mobile"));
                    user.put("address", rs.getString("address"));
                    user.put("nic", rs.getString("nic"));
                    user.put("profile_pic", rs.getString("profile_pic"));
                    user.put("total_deliveries", rs.getInt("total_deliveries"));
                    user.put("total_earned", rs.getDouble("total_earned"));
                    user.put("current_balance", rs.getDouble("current_balance"));
                    user.put("is_online", rs.getBoolean("is_online"));
                    user.put("unique_id", rs.getString("unique_id"));
                    user.put("nic_image", rs.getString("nic_image"));
                    approved.add(user);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return approved;
    }

    public boolean updateRiderStats(int id, double earnings) {
        String sql = "UPDATE `delivery_users` SET `total_deliveries` = `total_deliveries` + 1, `total_earned` = `total_earned` + ?, `current_balance` = `current_balance` + ? WHERE `id` = ?";
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            if (conn == null) return false;
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setDouble(1, earnings);
                pstmt.setDouble(2, earnings);
                pstmt.setInt(3, id);
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean payRider(int riderId, double amount, String method) {
        String updateBalance = "UPDATE `delivery_users` SET `current_balance` = `current_balance` - ? WHERE `id` = ? AND `current_balance` >= ?";
        String insertHistory = "INSERT INTO `payout_history` (`rider_id`, `amount_paid`, `payment_method`) VALUES (?, ?, ?)";
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            if (conn == null) return false;
            
            conn.setAutoCommit(false);
            try {
                int updated = 0;
                try (PreparedStatement psUpdate = conn.prepareStatement(updateBalance)) {
                    psUpdate.setDouble(1, amount);
                    psUpdate.setInt(2, riderId);
                    psUpdate.setDouble(3, amount);
                    updated = psUpdate.executeUpdate();
                }
                
                if (updated > 0) {
                    try (PreparedStatement psInsert = conn.prepareStatement(insertHistory)) {
                        psInsert.setInt(1, riderId);
                        psInsert.setDouble(2, amount);
                        psInsert.setString(3, method);
                        psInsert.executeUpdate();
                    }
                    conn.commit();
                    return true;
                } else {
                    conn.rollback();
                    return false;
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public List<Map<String, Object>> getPayoutHistory(int riderId) {
        List<Map<String, Object>> payouts = new ArrayList<>();
        String sql = "SELECT * FROM `payout_history` WHERE `rider_id` = ? ORDER BY `paid_at` DESC";
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            if (conn == null) return payouts;
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, riderId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> p = new HashMap<>();
                        p.put("id", rs.getLong("id"));
                        p.put("amount_paid", rs.getDouble("amount_paid"));
                        p.put("paid_at", rs.getTimestamp("paid_at"));
                        p.put("payment_method", rs.getString("payment_method"));
                        payouts.add(p);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return payouts;
    }

    public boolean setOnlineStatus(int id, boolean isOnline) {
        String sql = "UPDATE `delivery_users` SET `is_online` = ? WHERE `id` = ?";
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            if (conn == null) return false;
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setBoolean(1, isOnline);
                pstmt.setInt(2, id);
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateUserStatus(int id, String status) {
        String sql = "UPDATE `delivery_users` SET `status` = ? WHERE `id` = ?";
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            if (conn == null) {
                for (Map<String, Object> u : fallbackUsers) {
                    if ((Integer) u.get("id") == id) {
                        u.put("status", status);
                        return true;
                    }
                }
                return false;
            }
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, status);
                pstmt.setInt(2, id);
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean addTransaction(int userId, String type, double amount, Integer orderId, String description) {
        String sql = "INSERT INTO `transactions` (`user_id`, `type`, `amount`, `order_id`, `description`) VALUES (?, ?, ?, ?, ?)";
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            if (conn == null) return false;
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, userId);
                pstmt.setString(2, type);
                pstmt.setDouble(3, amount);
                if (orderId != null) {
                    pstmt.setInt(4, orderId);
                } else {
                    pstmt.setNull(4, java.sql.Types.INTEGER);
                }
                pstmt.setString(5, description);
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Map<String, Object>> getTransactions(Integer userId) {
        List<Map<String, Object>> transactions = new ArrayList<>();
        String sql = "SELECT * FROM `transactions`";
        if (userId != null) {
            sql += " WHERE `user_id` = ?";
        }
        sql += " ORDER BY `created_at` DESC";
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            if (conn == null) return transactions;
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                if (userId != null) {
                    pstmt.setInt(1, userId);
                }
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> t = new HashMap<>();
                        t.put("id", rs.getLong("id"));
                        t.put("user_id", rs.getInt("user_id"));
                        t.put("type", rs.getString("type"));
                        t.put("amount", rs.getDouble("amount"));
                        t.put("order_id", rs.getObject("order_id"));
                        t.put("description", rs.getString("description"));
                        t.put("created_at", rs.getTimestamp("created_at").toLocalDateTime().toString());
                        transactions.add(t);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return transactions;
    }
}
