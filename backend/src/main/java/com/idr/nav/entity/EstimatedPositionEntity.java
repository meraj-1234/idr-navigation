package com.idr.nav.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "estimated_positions")
public class EstimatedPositionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    @Column(name = "timestamp", nullable = false)
    private Long timestamp;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Column(name = "velocity")
    private Double velocity;

    @Column(name = "heading")
    private Double heading;

    @Column(name = "source", length = 32)
    private String source;

    @Column(name = "gnss_available")
    private Boolean gnssAvailable;

    @Column(name = "navigation_mode", length = 32)
    private String navigationMode;

    public EstimatedPositionEntity() {}

    public EstimatedPositionEntity(String sessionId, Long timestamp, Double latitude, Double longitude,
                                   Double velocity, Double heading, String source,
                                   Boolean gnssAvailable, String navigationMode) {
        this.sessionId = sessionId;
        this.timestamp = timestamp;
        this.latitude = latitude;
        this.longitude = longitude;
        this.velocity = velocity;
        this.heading = heading;
        this.source = source;
        this.gnssAvailable = gnssAvailable;
        this.navigationMode = navigationMode;
    }

    public Long getId() { return id; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Double getVelocity() { return velocity; }
    public void setVelocity(Double velocity) { this.velocity = velocity; }

    public Double getHeading() { return heading; }
    public void setHeading(Double heading) { this.heading = heading; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public Boolean getGnssAvailable() { return gnssAvailable; }
    public void setGnssAvailable(Boolean gnssAvailable) { this.gnssAvailable = gnssAvailable; }

    public String getNavigationMode() { return navigationMode; }
    public void setNavigationMode(String navigationMode) { this.navigationMode = navigationMode; }
}
