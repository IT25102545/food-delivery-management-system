package com.fooddelivery.controllers;

import com.fooddelivery.dao.MenuDAO;
import com.fooddelivery.dao.OrderDAO;
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

    public ApiController() {
        // Initialize tables on startup
        menuDAO.initializeTable();
        orderDAO.initializeTable();
        
        // Seed some initial data if empty
        if (menuDAO.getAllMenuItems().isEmpty()) {
            menuDAO.addMenuItem(new MenuItem(0, "Burger", "Delicious beef burger", 8.99, ""));
            menuDAO.addMenuItem(new MenuItem(0, "Pizza", "Cheese and pepperoni", 12.50, ""));
        }
        if (orderDAO.getAllOrders().isEmpty()) {
            // Inserting via SQL directly to seed is easier, but for now we skip or add a method in DAO if needed.
            // We just leave it empty.
        }
    }

    @GetMapping("/menu")
    public List<MenuItem> getMenu() {
        return menuDAO.getAllMenuItems();
    }

    @PostMapping(value = "/menu", consumes = {"multipart/form-data"})
    public ResponseEntity<String> addMenuItem(
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam("price") double price,
            @RequestParam(value = "image", required = false) MultipartFile image) {
        
        String imageUrl = saveImage(image);
        MenuItem item = new MenuItem(0, name, description, price, imageUrl);

        if (menuDAO.addMenuItem(item)) {
            return ResponseEntity.ok("Menu item added successfully");
        }
        return ResponseEntity.status(500).body("Failed to add menu item");
    }

    @PutMapping(value = "/menu/{id}", consumes = {"multipart/form-data"})
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

    @DeleteMapping("/menu/{id}")
    public ResponseEntity<String> deleteMenuItem(@PathVariable int id) {
        if (menuDAO.deleteMenuItem(id)) {
            return ResponseEntity.ok("Menu item deleted successfully");
        }
        return ResponseEntity.status(500).body("Failed to delete menu item");
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
     *   "firstName": "John",
     *   "lastName":  "Doe",
     *   "email":     "john@example.com",
     *   "phone":     "+1 555 0000",
     *   "deliveryAddress": "123 Brew St",
     *   "notes":     "Extra hot",
     *   "subtotal":  8.50,
     *   "deliveryFee": 2.00,
     *   "totalAmount": 10.50,
     *   "items": [
     *     { "productName": "Frappuccino", "size": "Medium", "quantity": 2, "unitPrice": 4.50 }
     *   ]
     * }
     */
    @PostMapping("/orders")
    public ResponseEntity<Map<String, Object>> placeOrder(@RequestBody Order order) {
        try {
            int newId = orderDAO.placeOrder(order);
            if (newId > 0) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "orderId", newId,
                    "message", "Order placed successfully"
                ));
            }
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Failed to save order. DAO returned -1."
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "DB Error: " + e.getMessage()
            ));
        }
    }

    @PutMapping("/orders/{id}/status")
    public ResponseEntity<String> updateOrderStatus(@PathVariable int id, @RequestBody Map<String, String> payload) {
        String status = payload.get("status");
        if (orderDAO.updateOrderStatus(id, status)) {
            return ResponseEntity.ok("Order status updated successfully");
        }
        return ResponseEntity.status(500).body("Failed to update order status");
    }

    @PostMapping("/reports/generate")
    public ResponseEntity<Map<String, String>> generateReport() {
        String result = FileHandler.generateDailyReport();
        return ResponseEntity.ok(Map.of("message", result));
    }

    private String saveImage(MultipartFile file) {
        if (file == null || file.isEmpty()) return "";
        try {
            // Create uploads directory if it doesn't exist outside of the classpath
            String uploadDir = "./external_uploads/";
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();
            
            // Generate a unique filename to prevent overwriting
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
