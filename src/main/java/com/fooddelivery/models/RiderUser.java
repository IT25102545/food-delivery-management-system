package com.fooddelivery.models;

public class RiderUser extends User {

    public RiderUser(int id, String username) {
        super(id, username, "RIDER");
    }

    @Override
    public String getRoleAccess() {
        return "/rider.html";
    }
}
