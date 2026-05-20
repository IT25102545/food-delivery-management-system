package com.fooddelivery.models;

/**
 * OOP Inheritance: BeverageItem inherits from the base MenuItem class.
 * Represents a drink product in the catalog.
 */
public class BeverageItem extends MenuItem {
    
    public BeverageItem(int id, String name, String description, double price, String imageUrl) {
        super(id, name, description, price, imageUrl);
    }

    /**
     * OOP Polymorphism: Overrides the base method to provide beverage-specific behavior.
     */
    @Override
    public String getPreparationInstructions() {
        return "Prepare in a cup. Serve chilled with ice or hot depending on the beverage type.";
    }
}
