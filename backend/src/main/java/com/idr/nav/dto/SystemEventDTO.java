package com.idr.nav.dto;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class SystemEventDTO {
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    private long timestamp;
    private String formattedTime;
    private String eventType;
    private String message;
    private String severity; // "INFO", "WARN", "ALERT", "SUCCESS"

    public SystemEventDTO() {}

    public SystemEventDTO(String eventType, String message, String severity) {
        this.timestamp = System.currentTimeMillis();
        this.formattedTime = TIME_FORMATTER.format(Instant.ofEpochMilli(this.timestamp));
        this.eventType = eventType;
        this.message = message;
        this.severity = severity;
    }

    public SystemEventDTO(long timestamp, String eventType, String message, String severity) {
        this.timestamp = timestamp;
        this.formattedTime = TIME_FORMATTER.format(Instant.ofEpochMilli(timestamp));
        this.eventType = eventType;
        this.message = message;
        this.severity = severity;
    }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        this.formattedTime = TIME_FORMATTER.format(Instant.ofEpochMilli(timestamp));
    }

    public String getFormattedTime() { return formattedTime; }
    public void setFormattedTime(String formattedTime) { this.formattedTime = formattedTime; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
}
