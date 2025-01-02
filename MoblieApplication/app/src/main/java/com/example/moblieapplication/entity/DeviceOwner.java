package com.example.moblieapplication.entity;

public class DeviceOwner {
    private String userName;
    private String email;
    private String phone;
    private String permission;

    public DeviceOwner(String userName, String email, String phone, String permission) {
        this.userName = userName;
        this.email = email;
        this.phone = phone;
        this.permission = permission;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }
}
