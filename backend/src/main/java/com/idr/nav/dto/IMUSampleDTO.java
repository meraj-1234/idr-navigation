package com.idr.nav.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class IMUSampleDTO {
    private long timestamp;
    private double accelerometerX;
    private double accelerometerY;
    private double accelerometerZ;
    private double gyroscopeX;
    private double gyroscopeY;
    private double gyroscopeZ;
    private Double alpha; // Compass heading / yaw in degrees (0 to 360)
    private Double beta;  // Front-to-back tilt / pitch (-180 to 180)
    private Double gamma; // Left-to-right tilt / roll (-90 to 90)
    private String sessionId;

    public IMUSampleDTO() {}

    public IMUSampleDTO(long timestamp, double accelerometerX, double accelerometerY, double accelerometerZ,
                         double gyroscopeX, double gyroscopeY, double gyroscopeZ,
                         Double alpha, Double beta, Double gamma, String sessionId) {
        this.timestamp = timestamp;
        this.accelerometerX = accelerometerX;
        this.accelerometerY = accelerometerY;
        this.accelerometerZ = accelerometerZ;
        this.gyroscopeX = gyroscopeX;
        this.gyroscopeY = gyroscopeY;
        this.gyroscopeZ = gyroscopeZ;
        this.alpha = alpha;
        this.beta = beta;
        this.gamma = gamma;
        this.sessionId = sessionId;
    }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public double getAccelerometerX() { return accelerometerX; }
    public void setAccelerometerX(double accelerometerX) { this.accelerometerX = accelerometerX; }

    public double getAccelerometerY() { return accelerometerY; }
    public void setAccelerometerY(double accelerometerY) { this.accelerometerY = accelerometerY; }

    public double getAccelerometerZ() { return accelerometerZ; }
    public void setAccelerometerZ(double accelerometerZ) { this.accelerometerZ = accelerometerZ; }

    public double getGyroscopeX() { return gyroscopeX; }
    public void setGyroscopeX(double gyroscopeX) { this.gyroscopeX = gyroscopeX; }

    public double getGyroscopeY() { return gyroscopeY; }
    public void setGyroscopeY(double gyroscopeY) { this.gyroscopeY = gyroscopeY; }

    public double getGyroscopeZ() { return gyroscopeZ; }
    public void setGyroscopeZ(double gyroscopeZ) { this.gyroscopeZ = gyroscopeZ; }

    public Double getAlpha() { return alpha; }
    public void setAlpha(Double alpha) { this.alpha = alpha; }

    public Double getBeta() { return beta; }
    public void setBeta(Double beta) { this.beta = beta; }

    public Double getGamma() { return gamma; }
    public void setGamma(Double gamma) { this.gamma = gamma; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public double getAccelerationMagnitude() {
        return Math.sqrt(accelerometerX * accelerometerX + accelerometerY * accelerometerY + accelerometerZ * accelerometerZ);
    }
}
