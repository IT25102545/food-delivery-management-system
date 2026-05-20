package com.fooddelivery.models;

/**
 * OOP Inheritance: FoodItem inherits from the base MenuItem class.
 * Represents a solid food product in the catalog.
 */
public class FoodItem extends MenuItem {
    
    public FoodItem(int id, String name, String description, double price, String imageUrl) {
        super(id, name, description, price, imageUrl);
    }

    /**
     * OOP Polymorphism: Overrides the base method to provide food-specific behavior.
     */
    @Override
    public String getPreparationInstructions() {
        return "Heat to appropriate temperature, plate carefully, and serve immediately.";
    }
}
