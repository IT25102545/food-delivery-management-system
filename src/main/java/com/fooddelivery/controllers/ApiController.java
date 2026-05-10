package com.fooddelivery.controllers;

import com.fooddelivery.dao.MenuDAO;
import com.fooddelivery.dao.OrderDAO;
import com.fooddelivery.dao.UserDAO;
import com.fooddelivery.models.MenuItem;
import com.fooddelivery.models.Order;
import com.fooddelivery.models.Order.OrderItem;
import com.fooddelivery.utils.FileHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final MenuDAO menuDAO = new MenuDAO();
    private final OrderDAO orderDAO = new OrderDAO();
    private final UserDAO userDAO = new UserDAO();

    public ApiController() {
        // initialize tables on startup
        menuDAO.initializeTable();
        orderDAO.initializeTable();
        userDAO.initializeTable();
        
        // seed some initial data if empty
        if (menuDAO.getAllMenuItems().isEmpty()) {
            menuDAO.addMenuItem(new MenuItem(0, "Burger", "Delicious beef burger", 8.99, ""));
            menuDAO.addMenuItem(new MenuItem(0, "Pizza", "Cheese and pepperoni", 12.50, ""));
        }
        if (orderDAO.getAllOrders().isEmpty()) {
            // inserting via sql directly to seed is easier, but for now we skip or add a method in dao if needed
            // we just leave it empty
        }
    }

    /**
     * GET /api/menu
     * Fetches all menu items to be displayed on the customer menu and admin
     * dashboard.
     */
    @GetMapping("/menu")
    public List<MenuItem> getMenu() {
        return menuDAO.getAllMenuItems();
    }

    /**
     * POST /api/menu
     * Adds a new menu item from the admin panel, including image upload handling.
     */
    @PostMapping(value = "/menu", consumes = { "multipart/form-data" })
    public ResponseEntity<String> addMenuItem(
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam("price") double price,
            @RequestParam(value = "image", required = false) MultipartFile image) {

        // --- Input Validation Added for Security ---
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Error: Product name cannot be empty");
        }
        if (price <= 0) {
            return ResponseEntity.badRequest().body("Error: Product price must be greater than zero");
        }
        // -----------------------------------------

        String imageUrl = saveImage(image);
        MenuItem item = new MenuItem(0, name, description, price, imageUrl);

        if (menuDAO.addMenuItem(item)) {
            return ResponseEntity.ok("Menu item added successfully");
        }
        return ResponseEntity.status(500).body("Failed to add menu item");
    }

    /**
     * PUT /api/menu/{id}
     * Updates an existing menu item's details. Called when an admin edits a
     * product.
     */
    @PutMapping(value = "/menu/{id}", consumes = { "multipart/form-data" })
    public ResponseEntity<String> updateMenuItem(
            @PathVariable int id,
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam("price") double price,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "existingImageUrl", required = false) String existingImageUrl) {

        String imageUrl = image != null && !image.isEmpty() ? saveImage(image) : existingImageUrl;
        MenuItem item = new MenuItem(id, name, description, price, imageUrl);

        if (menuDAO.updateMenuItem(item)) {
            return ResponseEntity.ok("Menu item updated successfully");
        }
        return ResponseEntity.status(500).body("Failed to update menu item");
    }

    /**
     * DELETE /api/menu/{id}
     * Removes a menu item from the catalog entirely.
     */
    @DeleteMapping("/menu/{id}")
    public ResponseEntity<String> deleteMenuItem(@PathVariable int id) {
        if (menuDAO.deleteMenuItem(id)) {
            return ResponseEntity.ok("Menu item deleted successfully");
        }
        return ResponseEntity.status(500).body("Failed to delete menu item");
    }

    /**
     * GET /api/menu/{id}/instructions
     * Demonstrates OOP Polymorphism by dynamically treating the item as either
     * a FoodItem or a BeverageItem and calling the overridden method.
     */
    @GetMapping("/menu/{id}/instructions")
    public ResponseEntity<String> getInstructions(@PathVariable int id) {
        MenuItem item = menuDAO.getAllMenuItems().stream()
                .filter(m -> m.getId() == id).findFirst().orElse(null);
        if (item == null) return ResponseEntity.notFound().build();

        MenuItem specificItem;
        String nameLower = item.getName().toLowerCase();

        // Dynamically instantiate the correct subclass based on item name
        if (nameLower.contains("coffee") || nameLower.contains("tea")
                || nameLower.contains("drink") || nameLower.contains("cola")
                || nameLower.contains("water") || nameLower.contains("juice")) {
            specificItem = new com.fooddelivery.models.BeverageItem(
                    item.getId(), item.getName(), item.getDescription(),
                    item.getPrice(), item.getImageUrl());
        } else {
            specificItem = new com.fooddelivery.models.FoodItem(
                    item.getId(), item.getName(), item.getDescription(),
                    item.getPrice(), item.getImageUrl());
        }

        // Polymorphism in action: Java automatically calls the correct overridden method!
        return ResponseEntity.ok("Instructions for " + specificItem.getName()
                + ": " + specificItem.getPreparationInstructions());
    }

    @GetMapping("/orders")
    public List<Order> getOrders() {
        return orderDAO.getAllOrders();
    }

    @GetMapping("/orders/status/{status}")
    public List<Order> getOrdersByStatus(@PathVariable String status) {
        return orderDAO.getOrdersByStatus(status.toUpperCase());
    }

    /**
     * POST /api/orders
     * Payload (JSON) from checkout.html:
     * {
     * "firstName": "John",
     * "lastName": "Doe",
     * "email": "john@example.com",
     * "phone": "+1 555 0000",
     * "deliveryAddress": "123 Brew St",
     * "notes": "Extra hot",
     * "subtotal": 8.50,
     * "deliveryFee": 2.00,
     * "totalAmount": 10.50,
     * "items": [
     * { "productName": "Frappuccino", "size": "Medium", "quantity": 2, "unitPrice":
     * 4.50 }
     * ]
     * }
     */
    @PostMapping("/orders")
    public ResponseEntity<Map<String, Object>> placeOrder(@RequestBody Order order) {
        try {
            // if paying with credits, deduct first
            if ("CREDITS".equalsIgnoreCase(order.getPaymentMethod())) {
                if (order.getCustomerId() == null || order.getCustomerId() <= 0) {
                    return ResponseEntity.status(400).body(Map.of("success", false, "message", "Must be logged in to use credits."));
                }
                if (!userDAO.chargeCredits(order.getCustomerId(), order.getTotalAmount())) {
                    return ResponseEntity.status(400).body(Map.of("success", false, "message", "Insufficient Zestro Credits."));
                }
            }
            int newId = orderDAO.placeOrder(order);
            if (newId > 0) {
                if (order.getCustomerId() != null && order.getCustomerId() > 0) {
                    userDAO.addTransaction(order.getCustomerId(), "PAYMENT", -order.getTotalAmount(), newId, "Order Payment - " + order.getPaymentMethod());
                }
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "orderId", newId,
                        "message", "Order placed successfully"));
            }
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Failed to save order. DAO returned -1."));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "DB Error: " + e.getMessage()));
        }
    }

    @PutMapping("/orders/{id}/cash-collected")
    public ResponseEntity<Map<String, Object>> confirmCashCollected(@PathVariable int id) {
        if (orderDAO.confirmCashCollected(id)) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Cash confirmed as collected"));
        }
        return ResponseEntity.status(500).body(Map.of("success", false, "message", "Failed to update cash status"));
    }

    @PutMapping("/orders/{id}/cash-settled")
    public ResponseEntity<Map<String, Object>> confirmCashSettled(@PathVariable int id) {
        if (orderDAO.confirmCashSettled(id)) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Cash confirmed as settled"));
        }
        return ResponseEntity.status(500).body(Map.of("success", false, "message", "Failed to update cash status"));
    }

    @PutMapping("/orders/{id}/status")
    public ResponseEntity<String> updateOrderStatus(@PathVariable int id, @RequestBody Map<String, Object> payload) {
        String status = (String) payload.get("status");
        
        if ("DELIVERED".equalsIgnoreCase(status) && payload.containsKey("riderId") && payload.containsKey("earnings")) {
            try {
                int riderId = Integer.parseInt(payload.get("riderId").toString());
                double earnings = Double.parseDouble(payload.get("earnings").toString());
                
                if (orderDAO.completeOrder(id, riderId, earnings)) {
                    userDAO.updateRiderStats(riderId, earnings);
                    return ResponseEntity.ok("Order delivered successfully");
                }
            } catch (Exception ignored) {}
        }
        
        if ("CANCELLED".equalsIgnoreCase(status)) {
            Order order = orderDAO.getOrderById(id);
            if (order != null && "CREDITS".equalsIgnoreCase(order.getPaymentMethod()) && order.getCustomerId() != null && order.getCustomerId() > 0) {
                // refund credits
                userDAO.chargeCredits(order.getCustomerId(), -order.getTotalAmount()); // negative charge = refund
                userDAO.addTransaction(order.getCustomerId(), "REFUND", order.getTotalAmount(), id, "Order Cancelled Refund");
            }
        }
        
        if (orderDAO.updateOrderStatus(id, status)) {
            return ResponseEntity.ok("Order status updated successfully");
        }
        return ResponseEntity.status(500).body("Failed to update order status");
    }

    @PostMapping(value = "/auth/register", consumes = {"multipart/form-data"})
    public ResponseEntity<Map<String, Object>> register(
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            @RequestParam(value = "role", defaultValue = "RIDER") String role,
            @RequestParam(value = "fullName", required = false) String fullName,
            @RequestParam(value = "mobile", required = false) String mobile,
            @RequestParam(value = "address", required = false) String address,
            @RequestParam(value = "nic", required = false) String nic,
            @RequestParam(value = "profilePic", required = false) MultipartFile profilePic,
            @RequestParam(value = "nicImage", required = false) MultipartFile nicImage) {
        
        String profilePicUrl = saveImage(profilePic);
        String nicImageUrl = saveImage(nicImage);
        
        if (userDAO.registerUser(username, password, role, fullName, mobile, address, nic, profilePicUrl, nicImageUrl)) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Registration successful"));
        }
        return ResponseEntity.status(400).body(Map.of("success", false, "message", "Username already exists"));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        String password = payload.get("password");
        
        Map<String, Object> user = userDAO.authenticateUser(username, password);
        if (user != null) {
            String status = (String) user.get("status");
            if ("RIDER".equalsIgnoreCase((String) user.get("role")) && !"APPROVED".equalsIgnoreCase(status)) {
                return ResponseEntity.status(403).body(Map.of("success", false, "message", "Account pending admin approval"));
            }
            return ResponseEntity.ok(Map.of("success", true, "user", user));
        }
        return ResponseEntity.status(401).body(Map.of("success", false, "message", "Invalid credentials"));
    }

    @PostMapping("/auth/register/customer")
    public ResponseEntity<Map<String, Object>> registerCustomer(@RequestBody Map<String, String> payload) {
        String fullName = payload.get("fullName");
        String username = payload.get("username"); 
        String password = payload.get("password");
        String mobile = payload.get("mobile");
        String address = payload.get("address");

        if (userDAO.registerUser(username, password, "CUSTOMER", fullName, mobile, address, null, null, null)) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Registration successful"));
        }
        return ResponseEntity.status(400).body(Map.of("success", false, "message", "Email already exists"));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable int id) {
        if (userDAO.deleteUser(id)) {
            return ResponseEntity.ok(Map.of("success", true, "message", "User deleted successfully"));
        }
        return ResponseEntity.status(500).body(Map.of("success", false, "message", "Failed to delete user"));
    }

    @PostMapping("/customers/{id}/topup")
    public ResponseEntity<Map<String, Object>> topUpCredits(@PathVariable int id, @RequestBody Map<String, Object> payload) {
        try {
            double amount = Double.parseDouble(payload.get("amount").toString());
            if (userDAO.topUpCredits(id, amount)) {
                userDAO.addTransaction(id, "TOPUP", amount, null, "Wallet Top-up");
                return ResponseEntity.ok(Map.of("success", true, "message", "Top-up successful"));
            }
        } catch(Exception e) {}
        return ResponseEntity.status(400).body(Map.of("success", false, "message", "Top-up failed"));
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<Map<String, Object>>> getAllTransactions() {
        return ResponseEntity.ok(userDAO.getTransactions(null));
    }

    @GetMapping("/transactions/customer/{id}")
    public ResponseEntity<List<Map<String, Object>>> getCustomerTransactions(@PathVariable int id) {
        return ResponseEntity.ok(userDAO.getTransactions(id));
    }

    @PostMapping("/customers/{id}/cards")
    public ResponseEntity<Map<String, Object>> updateSavedCards(@PathVariable int id, @RequestBody Map<String, Object> payload) {
        try {
            String cardsJson = (String) payload.get("cards");
            if (userDAO.updateSavedCards(id, cardsJson)) {
                return ResponseEntity.ok(Map.of("success", true, "message", "Cards updated successfully"));
            }
        } catch(Exception e) {}
        return ResponseEntity.status(400).body(Map.of("success", false, "message", "Failed to update cards"));
    }

    @GetMapping("/customers/{id}")
    public ResponseEntity<Map<String, Object>> getCustomerProfile(@PathVariable int id) {
        Map<String, Object> user = userDAO.getUserById(id);
        if (user != null) {
            return ResponseEntity.ok(Map.of("success", true, "user", user));
        }
        return ResponseEntity.status(404).body(Map.of("success", false, "message", "Customer not found"));
    }

    @GetMapping("/admin/riders/{id}/pending-cash")
    public ResponseEntity<List<Order>> getPendingCashOrders(@PathVariable int id) {
        List<Order> pendingCashOrders = orderDAO.getOrdersByRider(id).stream()
            .filter(o -> "COLLECTED".equals(o.getCashStatus()))
            .toList();
        return ResponseEntity.ok(pendingCashOrders);
    }

    @DeleteMapping("/admin/cleanup-db-orders")
    public ResponseEntity<String> cleanupOrders() {
        try {
            java.sql.Connection conn = com.fooddelivery.utils.DatabaseConnection.getInstance().getConnection();
            try (java.sql.Statement stmt = conn.createStatement()) {
                stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
                stmt.execute("TRUNCATE TABLE order_items");
                stmt.execute("TRUNCATE TABLE orders");
                stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
                
                // clear the fallback memory arrays too
                try {
                    java.lang.reflect.Field fallbackOrders = com.fooddelivery.dao.OrderDAO.class.getDeclaredField("fallbackOrders");
                    fallbackOrders.setAccessible(true);
                    ((java.util.List) fallbackOrders.get(null)).clear();
                    
                    java.lang.reflect.Field fallbackIdCounter = com.fooddelivery.dao.OrderDAO.class.getDeclaredField("fallbackIdCounter");
                    fallbackIdCounter.setAccessible(true);
                    fallbackIdCounter.set(null, 1);
                } catch (Exception e) {
                    // ignore reflection errors if it's fine
                }
                
                return ResponseEntity.ok("Database cleaned successfully");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/admin/customers")
    public ResponseEntity<List<Map<String, Object>>> getCustomers() {
        return ResponseEntity.ok(userDAO.getCustomers());
    }


    @GetMapping("/admin/riders/pending")
    public ResponseEntity<List<Map<String, Object>>> getPendingRiders() {
        return ResponseEntity.ok(userDAO.getPendingRiders());
    }

    @GetMapping("/admin/riders/approved")
    public ResponseEntity<List<Map<String, Object>>> getApprovedRiders() {
        return ResponseEntity.ok(userDAO.getApprovedRiders());
    }

    @PutMapping("/admin/riders/{id}/status")
    public ResponseEntity<Map<String, Object>> updateRiderStatus(@PathVariable int id, @RequestBody Map<String, String> payload) {
        String status = payload.get("status");
        if (userDAO.updateUserStatus(id, status)) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Rider status updated"));
        }
        return ResponseEntity.status(500).body(Map.of("success", false, "message", "Failed to update rider status"));
    }

    @PutMapping("/riders/{id}/online")
    public ResponseEntity<Map<String, Object>> setOnlineStatus(@PathVariable int id, @RequestBody Map<String, Boolean> payload) {
        Boolean isOnline = payload.get("isOnline");
        if (isOnline != null && userDAO.setOnlineStatus(id, isOnline)) {
            return ResponseEntity.ok(Map.of("success", true));
        }
        return ResponseEntity.status(500).body(Map.of("success", false));
    }
    
    @GetMapping("/riders/{id}/history")
    public ResponseEntity<List<Order>> getRiderHistory(@PathVariable int id) {
        return ResponseEntity.ok(orderDAO.getOrdersByRider(id));
    }
    
    @GetMapping("/riders/{id}/payouts")
    public ResponseEntity<List<Map<String, Object>>> getRiderPayouts(@PathVariable int id) {
        return ResponseEntity.ok(userDAO.getPayoutHistory(id));
    }
    
    @PostMapping("/admin/riders/{id}/pay")
    public ResponseEntity<Map<String, Object>> payRider(@PathVariable int id, @RequestBody Map<String, Object> payload) {
        try {
            double amount = Double.parseDouble(payload.get("amount").toString());
            String method = payload.getOrDefault("method", "Bank Transfer").toString();
            if (userDAO.payRider(id, amount, method)) {
                return ResponseEntity.ok(Map.of("success", true, "message", "Payout processed"));
            }
        } catch (Exception e) {}
        return ResponseEntity.status(400).body(Map.of("success", false, "message", "Failed to process payout"));
    }

    @PostMapping("/reports/generate")
    public ResponseEntity<Map<String, String>> generateReport() {
        String result = FileHandler.generateDailyReport();
        return ResponseEntity.ok(Map.of("message", result));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> getUserProfile(@PathVariable int id) {
        Map<String, Object> user = userDAO.getUserById(id);
        if (user != null) {
            return ResponseEntity.ok(user);
        }
        return ResponseEntity.status(404).body(Map.of("message", "User not found"));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> updateUserProfile(@PathVariable int id, @RequestBody Map<String, String> payload) {
        String fullName = payload.get("fullName");
        String mobile = payload.get("mobile");
        String address = payload.get("address");

        if (userDAO.updateUserProfile(id, fullName, mobile, address)) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Profile updated successfully"));
        }
        return ResponseEntity.status(500).body(Map.of("success", false, "message", "Failed to update profile"));
    }

    @PutMapping(value = "/users/{id}/settings", consumes = {"multipart/form-data"})
    public ResponseEntity<Map<String, Object>> updateAdminSettings(
            @PathVariable int id,
            @RequestParam("fullName") String fullName,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam(value = "profilePic", required = false) MultipartFile profilePic,
            @RequestParam(value = "existingProfilePic", required = false) String existingProfilePic) {
        
        String profilePicUrl = profilePic != null && !profilePic.isEmpty() ? saveImage(profilePic) : existingProfilePic;

        if (userDAO.updateAdminSettings(id, fullName, password, profilePicUrl)) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Admin settings updated successfully"));
        }
        return ResponseEntity.status(500).body(Map.of("success", false, "message", "Failed to update admin settings"));
    }

    @GetMapping("/orders/customer/{email}")
    public ResponseEntity<List<Order>> getCustomerOrders(@PathVariable String email) {
        return ResponseEntity.ok(orderDAO.getOrdersByEmail(email));
    }

    private String saveImage(MultipartFile file) {
        if (file == null || file.isEmpty())
            return "";
        try {
            // create uploads directory if it doesn't exist outside of the classpath
            String uploadDir = "./external_uploads/";
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();
            
            // generate a unique filename to prevent overwriting
            String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename().replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
            Path path = Paths.get(uploadDir + filename);
            Files.write(path, file.getBytes());
            return "/uploads/" + filename;
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }
}
