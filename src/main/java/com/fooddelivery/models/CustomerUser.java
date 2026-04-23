package com.fooddelivery.models;

public class CustomerUser extends User {

    public CustomerUser(int id, String username) {
        super(id, username, "CUSTOMER");
    }

    @Override
    public String getRoleAccess() {
        return "/customer.html";
    }
}
