package com.fooddelivery.models;

public class AdminUser extends User {

    public AdminUser(int id, String username) {
        super(id, username, "ADMIN");
    }

    @Override
    public String getRoleAccess() {
        return "/admin.html";
    }
}
