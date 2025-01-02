package com.example.moblieapplication.entity;

public class DeviceList {
    private int deviceCode;
    private String description;
    private String owner;

    public DeviceList(int deviceCode, String description, String owner) {
        this.deviceCode = deviceCode;
        this.description = description;
        this.owner = owner;
    }

    public int getDeviceCode() {
        return deviceCode;
    }

    public void setDeviceCode(int deviceCode) {
        this.deviceCode = deviceCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }
}
