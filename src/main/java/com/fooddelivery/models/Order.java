package com.fooddelivery.models;

import java.time.LocalDateTime;
import java.util.List;

public class Order {
    private int id;

    // Customer contact info (from checkout.html)
    private String firstName;
    private String lastName;
    private String email;
    private String phone;

    // Delivery address (from order.html)
    private String deliveryAddress;

    // Special instructions (from checkout.html)
    private String notes;

    // Financials
    private double subtotal;
    private double deliveryFee;
    private double totalAmount;

    // Order lifecycle
    private String status;           // PENDING, CONFIRMED, OUT_FOR_DELIVERY, DELIVERED, CANCELLED
    private LocalDateTime createdAt;

    // Items snapshot
    private List<OrderItem> items;

    // Nested item class
    public static class OrderItem {
        private String productName;
        private String size;
        private int quantity;
        private double unitPrice;

        public OrderItem() {}

        public OrderItem(String productName, String size, int quantity, double unitPrice) {
            this.productName = productName;
            this.size = size;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
        }

        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public String getSize() { return size; }
        public void setSize(String size) { this.size = size; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public double getUnitPrice() { return unitPrice; }
        public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }
        public double getLineTotal() { return unitPrice * quantity; }
    }

    // Constructors
    public Order() {}

    // Legacy minimal constructor (used by existing getAllOrders mapping)
    public Order(int id, String customerName, double totalAmount, String status) {
        this.id = id;
        // Split customerName into first/last for backwards compat
        String[] parts = customerName.split(" ", 2);
        this.firstName = parts[0];
        this.lastName  = parts.length > 1 ? parts[1] : "";
        this.totalAmount = totalAmount;
        this.status = status;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    // Convenience for existing code expecting customerName
    public String getCustomerName() {
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }
    public void setCustomerName(String customerName) {
        String[] parts = customerName.split(" ", 2);
        this.firstName = parts[0];
        this.lastName  = parts.length > 1 ? parts[1] : "";
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    public double getDeliveryFee() { return deliveryFee; }
    public void setDeliveryFee(double deliveryFee) { this.deliveryFee = deliveryFee; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
}
