package com.idr.nav.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "navigation_events")
public class NavigationEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    @Column(name = "timestamp", nullable = false)
    private Long timestamp;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "description", nullable = false, length = 512)
    private String description;

    @Column(name = "severity", nullable = false, length = 16)
    private String severity; // INFO, WARN, ALERT, SUCCESS

    public NavigationEventEntity() {}

    public NavigationEventEntity(String sessionId, Long timestamp, String eventType, String description, String severity) {
        this.sessionId = sessionId;
        this.timestamp = timestamp;
        this.eventType = eventType;
        this.description = description;
        this.severity = severity;
    }

    public Long getId() { return id; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
}
