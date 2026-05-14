package com.fooddelivery.dao;

import com.fooddelivery.models.Order;
import com.fooddelivery.models.Order.OrderItem;
import com.fooddelivery.utils.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrderDAO {
    private static final List<Order> fallbackOrders = new ArrayList<>();
    private static int fallbackIdCounter = 1;

    // table init
    public void initializeTable() {
        String ordersTable = "CREATE TABLE IF NOT EXISTS orders (" +
            "id              INT AUTO_INCREMENT PRIMARY KEY, " +
            "first_name      VARCHAR(100) NOT NULL, " +
            "last_name       VARCHAR(100) NOT NULL, " +
            "email           VARCHAR(255) NOT NULL, " +
            "phone           VARCHAR(50)  NOT NULL, " +
            "delivery_address TEXT, " +
            "notes           TEXT, " +
            "subtotal        DECIMAL(10,2) NOT NULL DEFAULT 0, " +
            "delivery_fee    DECIMAL(10,2) NOT NULL DEFAULT 2.00, " +
            "total_amount    DECIMAL(10,2) NOT NULL, " +
            "status          VARCHAR(50)   NOT NULL DEFAULT 'PENDING', " +
            "created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP" +
            ")";

        String itemsTable = "CREATE TABLE IF NOT EXISTS order_items (" +
            "id           INT AUTO_INCREMENT PRIMARY KEY, " +
            "order_id     INT NOT NULL, " +
            "product_name VARCHAR(255) NOT NULL, " +
            "size         VARCHAR(50), " +
            "quantity     INT NOT NULL, " +
            "unit_price   DECIMAL(10,2) NOT NULL, " +
            "FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE" +
            ")";

        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            if (conn == null) return;
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(ordersTable);
                stmt.execute(itemsTable);
                
                String[] alters = {
                    "ALTER TABLE orders ADD COLUMN rider_id INT",
                    "ALTER TABLE orders ADD COLUMN rider_payout DECIMAL(10,2)",
                    "ALTER TABLE orders ADD COLUMN completed_at DATETIME",
                    "ALTER TABLE orders ADD COLUMN cancellation_reason VARCHAR(255)",
                    "ALTER TABLE orders ADD COLUMN payment_method VARCHAR(20) DEFAULT 'CARD'",
                    "ALTER TABLE orders ADD COLUMN cash_status VARCHAR(20) DEFAULT NULL"
                };
                for (String alt : alters) {
                    try { stmt.execute(alt); } catch (SQLException ignored) {}
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // create order
    public int placeOrder(Order order) throws SQLException {
        String insertOrder =
            "INSERT INTO orders (first_name, last_name, email, phone, delivery_address, notes, " +
            "subtotal, delivery_fee, total_amount, status, payment_method, cash_status, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', ?, ?, NOW())";

        String insertItem =
            "INSERT INTO order_items (order_id, product_name, size, quantity, unit_price) " +
            "VALUES (?, ?, ?, ?, ?)";

        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            if (conn == null) {
                order.setId(fallbackIdCounter++);
                order.setStatus("PENDING");
                order.setCreatedAt(LocalDateTime.now());
                fallbackOrders.add(order);
                return order.getId();
            }

            conn.setAutoCommit(false);
            try {
                // insert order header
                int generatedId;
                try (PreparedStatement ps = conn.prepareStatement(insertOrder, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, order.getFirstName());
                    ps.setString(2, order.getLastName());
                    ps.setString(3, order.getEmail());
                    ps.setString(4, order.getPhone());
                    ps.setString(5, order.getDeliveryAddress() != null ? order.getDeliveryAddress() : "");
                    ps.setString(6, order.getNotes() != null ? order.getNotes() : "");
                    ps.setDouble(7, order.getSubtotal());
                    ps.setDouble(8, order.getDeliveryFee());
                    ps.setDouble(9, order.getTotalAmount());
                    String pm = order.getPaymentMethod() != null ? order.getPaymentMethod() : "CARD";
                    ps.setString(10, pm);
                    ps.setString(11, "COD".equalsIgnoreCase(pm) ? "PENDING" : null);
                    ps.executeUpdate();

                    ResultSet rs = ps.getGeneratedKeys();
                    if (!rs.next()) { conn.rollback(); return -1; }
                    generatedId = rs.getInt(1);
                }

                // insert line items
                if (order.getItems() != null) {
                    try (PreparedStatement ps = conn.prepareStatement(insertItem)) {
                        for (OrderItem item : order.getItems()) {
                            ps.setInt(1, generatedId);
                            ps.setString(2, item.getProductName());
                            ps.setString(3, item.getSize());
                            ps.setInt(4, item.getQuantity());
                            ps.setDouble(5, item.getUnitPrice());
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }
                }

                conn.commit();
                return generatedId;

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            throw e;
        }
    }

    // read all orders (with items)
    public List<Order> getAllOrders() {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT o.*, " +
            "oi.id as item_id, oi.product_name, oi.size, oi.quantity, oi.unit_price " +
            "FROM orders o " +
            "LEFT JOIN order_items oi ON o.id = oi.order_id " +
            "ORDER BY o.id DESC, oi.id ASC";

        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            if (conn == null) return new ArrayList<>(fallbackOrders);
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                Order current = null;
                while (rs.next()) {
                    int orderId = rs.getInt("id");

                    if (current == null || current.getId() != orderId) {
                        current = mapOrderRow(rs);
                        current.setItems(new ArrayList<>());
                        orders.add(current);
                    }

                    // add item if exists
                    if (rs.getObject("item_id") != null) {
                        current.getItems().add(new OrderItem(
                            rs.getString("product_name"),
                            rs.getString("size"),
                            rs.getInt("quantity"),
                            rs.getDouble("unit_price")
                        ));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return orders;
    }

    public Order getOrderById(int id) {
        String sql = "SELECT o.*, " +
            "oi.id as item_id, oi.product_name, oi.size, oi.quantity, oi.unit_price " +
            "FROM orders o " +
            "LEFT JOIN order_items oi ON o.id = oi.order_id " +
            "WHERE o.id = ?";

        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            if (conn == null) {
                return fallbackOrders.stream().filter(o -> o.getId() == id).findFirst().orElse(null);
            }
            
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    Order current = null;
                    while (rs.next()) {
                        if (current == null) {
                            current = mapOrderRow(rs);
                            current.setItems(new ArrayList<>());
                        }
                        if (rs.getInt("item_id") > 0) {
                            current.getItems().add(new OrderItem(
                                rs.getString("product_name"),
                                rs.getString("size"),
                                rs.getInt("quantity"),
                                rs.getDouble("unit_price")
                            ));
                        }
                    }
                    return current;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // update status
    public boolean updateOrderStatus(int id, String status) {
        String sql = "UPDATE orders SET status = ? WHERE id = ?";
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            if (conn == null) {
                for (Order o : fallbackOrders) {
                    if (o.getId() == id) {
                        o.setStatus(status);
                        return true;
                    }
                }
                return false;
            }
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, status);
                ps.setInt(2, id);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean completeOrder(int orderId, int riderId, double payout) {
        String sql = "UPDATE orders SET status = 'DELIVERED', rider_id = ?, rider_payout = ?, completed_at = NOW() WHERE id = ?";
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            if (conn == null) return false;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, riderId);
                ps.setDouble(2, payout);
                ps.setInt(3, orderId);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean confirmCashCollected(int orderId) {
        String sql = "UPDATE orders SET status = 'CASH_COLLECTED', cash_status = 'COLLECTED' WHERE id = ?";
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            if (conn == null) return false;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, orderId);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean confirmCashSettled(int orderId) {
        String sql = "UPDATE orders SET cash_status = 'SETTLED' WHERE id = ?";
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            if (conn == null) return false;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, orderId);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Order> getOrdersByRider(int riderId) {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT * FROM orders WHERE rider_id = ? ORDER BY completed_at DESC";
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            if (conn == null) return orders;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, riderId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) orders.add(mapOrderRow(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return orders;
    }

    public List<Order> getOrdersByEmail(String email) {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT o.*, " +
            "oi.id as item_id, oi.product_name, oi.size, oi.quantity, oi.unit_price " +
            "FROM orders o " +
            "LEFT JOIN order_items oi ON o.id = oi.order_id " +
            "WHERE o.email = ? " +
            "ORDER BY o.created_at DESC, oi.id ASC";

        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            if (conn == null) {
                for (Order o : fallbackOrders) {
                    if (email.equalsIgnoreCase(o.getEmail())) orders.add(o);
                }
                return orders;
            }
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, email);
                ResultSet rs = ps.executeQuery();
                Order current = null;
                while (rs.next()) {
                    int orderId = rs.getInt("id");

                    if (current == null || current.getId() != orderId) {
                        current = mapOrderRow(rs);
                        current.setItems(new ArrayList<>());
                        orders.add(current);
                    }

                    if (rs.getObject("item_id") != null) {
                        current.getItems().add(new OrderItem(
                            rs.getString("product_name"),
                            rs.getString("size"),
                            rs.getInt("quantity"),
                            rs.getDouble("unit_price")
                        ));
                    }
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return orders;
    }

    // stats (for admin/rider dashboards)
    public double getTotalRevenue() {
        String sql = "SELECT COALESCE(SUM(total_amount), 0) FROM orders WHERE status = 'DELIVERED'";
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            if (conn == null) return 0.0;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0.0;
    }

    public int getTotalOrders() {
        String sql = "SELECT COUNT(*) FROM orders";
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            if (conn == null) return 0;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public List<Order> getOrdersByStatus(String status) {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT * FROM orders WHERE status = ? ORDER BY created_at DESC";
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            if (conn == null) {
                List<Order> filtered = new ArrayList<>();
                for (Order o : fallbackOrders) {
                    if (status.equalsIgnoreCase(o.getStatus())) {
                        filtered.add(o);
                    }
                }
                return filtered;
            }
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, status);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) orders.add(mapOrderRow(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return orders;
    }

    // private mapper
    private Order mapOrderRow(ResultSet rs) throws SQLException {
        Order o = new Order();
        o.setId(rs.getInt("id"));
        o.setFirstName(rs.getString("first_name"));
        o.setLastName(rs.getString("last_name"));
        o.setEmail(rs.getString("email"));
        o.setPhone(rs.getString("phone"));
        o.setDeliveryAddress(rs.getString("delivery_address"));
        o.setNotes(rs.getString("notes"));
        o.setSubtotal(rs.getDouble("subtotal"));
        o.setDeliveryFee(rs.getDouble("delivery_fee"));
        o.setTotalAmount(rs.getDouble("total_amount"));
        o.setStatus(rs.getString("status"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) o.setCreatedAt(ts.toLocalDateTime());
        
        o.setRiderId(rs.getObject("rider_id") != null ? rs.getInt("rider_id") : null);
        o.setRiderPayout(rs.getObject("rider_payout") != null ? rs.getDouble("rider_payout") : null);
        
        Timestamp comp = rs.getTimestamp("completed_at");
        if (comp != null) o.setCompletedAt(comp.toLocalDateTime());
        
        o.setCancellationReason(rs.getString("cancellation_reason"));
        
        try { o.setPaymentMethod(rs.getString("payment_method")); } catch (SQLException ignored) {}
        try { o.setCashStatus(rs.getString("cash_status")); } catch (SQLException ignored) {}
        
        return o;
    }
}

// order processing optimizations applied
