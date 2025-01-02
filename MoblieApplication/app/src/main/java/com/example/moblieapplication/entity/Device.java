package com.example.moblieapplication.entity;

public class Device {
    private int deviceCode;
    private String description;
    private String temperature;
    private String humidity;
    private String soilMoisture;
    private String phLevel;

    // Firebase default constructor
    public Device() {
    }

    public Device(int deviceCode, String description, String temperature, String humidity, String soilMoisture, String phLevel) {
        this.deviceCode = deviceCode;
        this.description = description;
        this.temperature = temperature;
        this.humidity = humidity;
        this.soilMoisture = soilMoisture;
        this.phLevel = phLevel;
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

    public String getTemperature() {
        return temperature;
    }

    public void setTemperature(String temperature) {
        this.temperature = temperature;
    }

    public String getHumidity() {
        return humidity;
    }

    public void setHumidity(String humidity) {
        this.humidity = humidity;
    }

    public String getSoilMoisture() {
        return soilMoisture;
    }

    public void setSoilMoisture(String soilMoisture) {
        this.soilMoisture = soilMoisture;
    }

    public String getPhLevel() {
        return phLevel;
    }

    public void setPhLevel(String phLevel) {
        this.phLevel = phLevel;
    }
}


