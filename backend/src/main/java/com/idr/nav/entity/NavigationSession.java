package com.idr.nav.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "navigation_sessions")
public class NavigationSession {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "status", nullable = false, length = 32)
    private String status; // ACTIVE, PAUSED, STOPPED

    @Column(name = "mode", nullable = false, length = 32)
    private String mode; // SIMULATION, REAL_SENSOR

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "initial_latitude")
    private Double initialLatitude;

    @Column(name = "initial_longitude")
    private Double initialLongitude;

    public NavigationSession() {}

    public NavigationSession(String id, String name, String status, String mode,
                             LocalDateTime startTime, Double initialLatitude, Double initialLongitude) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.mode = mode;
        this.startTime = startTime;
        this.initialLatitude = initialLatitude;
        this.initialLongitude = initialLongitude;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public Double getInitialLatitude() { return initialLatitude; }
    public void setInitialLatitude(Double initialLatitude) { this.initialLatitude = initialLatitude; }

    public Double getInitialLongitude() { return initialLongitude; }
    public void setInitialLongitude(Double initialLongitude) { this.initialLongitude = initialLongitude; }
}
