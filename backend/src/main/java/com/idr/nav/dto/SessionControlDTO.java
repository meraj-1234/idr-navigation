package com.idr.nav.dto;

public class SessionControlDTO {
    private String command; // "START", "PAUSE", "STOP", "RESET"
    private String sessionName;
    private String mode; // "SIMULATION", "REAL_SENSOR"
    private Double initialLatitude;
    private Double initialLongitude;
    private String routePreset; // e.g. "URBAN_TUNNEL", "GRID_LOOP"

    public SessionControlDTO() {}

    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }

    public String getSessionName() { return sessionName; }
    public void setSessionName(String sessionName) { this.sessionName = sessionName; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public Double getInitialLatitude() { return initialLatitude; }
    public void setInitialLatitude(Double initialLatitude) { this.initialLatitude = initialLatitude; }

    public Double getInitialLongitude() { return initialLongitude; }
    public void setInitialLongitude(Double initialLongitude) { this.initialLongitude = initialLongitude; }

    public String getRoutePreset() { return routePreset; }
    public void setRoutePreset(String routePreset) { this.routePreset = routePreset; }
}
