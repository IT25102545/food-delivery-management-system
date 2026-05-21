package com.fooddelivery.models;

/**
 * Represents a product/item in the menu catalog.
 * This is the primary data model (blueprint) used by the Item Management module
 * to map database records to Java objects.
 */
public class MenuItem {
    private int id;
    private String name;
    private String description;
    private double price;
    private String imageUrl;

    public MenuItem() {
    }

    public MenuItem(int id, String name, String description, double price, String imageUrl) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageUrl = imageUrl;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    /**
     * OOP Business Logic: Calculates the promotional price of the item.
     * Demonstrates encapsulation by keeping data manipulation within the model.
     * @param discountPercentage The discount percentage (e.g., 10 for 10% off)
     * @return The new discounted price
     */
    public double getDiscountedPrice(double discountPercentage) {
        if (discountPercentage <= 0 || discountPercentage >= 100) {
            return this.price;
        }
        return this.price - (this.price * (discountPercentage / 100.0));
    }

    /**
     * OOP Polymorphism (Base Method): Intended to be overridden by subclasses
     * (FoodItem and BeverageItem) to provide item-specific preparation behavior.
     * @return General preparation instructions
     */
    public String getPreparationInstructions() {
        return "Standard preparation in the kitchen.";
    }
}
