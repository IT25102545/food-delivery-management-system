package com.fooddelivery.models;

import java.time.LocalDateTime;
import java.util.List;

public class Order {
    private int id;

    // customer contact info (from checkout.html)
    private Integer customerId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;

    // delivery address (from order.html)
    private String deliveryAddress;

    // special instructions (from checkout.html)
    private String notes;

    // financials
    private double subtotal;
    private double deliveryFee;
    private double totalAmount;

    // order lifecycle
    private String status;           // pending, confirmed, out_for_delivery, delivered, cancelled
    private LocalDateTime createdAt;
    
    // rider specifics
    private Integer riderId;
    private Double riderPayout;
    private LocalDateTime completedAt;
    private String cancellationReason;

    // payment
    private String paymentMethod;  // card, cod
    private String cashStatus;     // null, pending, collected

    // items snapshot
    private List<OrderItem> items;

    // nested item class
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

    // constructors
    public Order() {}

    // legacy minimal constructor (used by existing getallorders mapping)
    public Order(int id, String customerName, double totalAmount, String status) {
        this.id = id;
        // split customername into first/last for backwards compat
        String[] parts = customerName.split(" ", 2);
        this.firstName = parts[0];
        this.lastName  = parts.length > 1 ? parts[1] : "";
        this.totalAmount = totalAmount;
        this.status = status;
    }

    // getters & setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Integer getCustomerId() { return customerId; }
    public void setCustomerId(Integer customerId) { this.customerId = customerId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    // convenience for existing code expecting customername
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
    
    public Integer getRiderId() { return riderId; }
    public void setRiderId(Integer riderId) { this.riderId = riderId; }
    
    public Double getRiderPayout() { return riderPayout; }
    public void setRiderPayout(Double riderPayout) { this.riderPayout = riderPayout; }
    
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    
    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getCashStatus() { return cashStatus; }
    public void setCashStatus(String cashStatus) { this.cashStatus = cashStatus; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
}
