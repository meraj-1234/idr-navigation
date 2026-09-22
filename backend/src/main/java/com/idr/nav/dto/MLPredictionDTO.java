package com.idr.nav.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Data Transfer Object representing road and vibration events.
 * 
 * STRICT COMPLIANCE RULE:
 * Because a trained machine learning model is not yet integrated into this prototype,
 * modelStatus is explicitly "MOCK" and confidence is strictly null.
 * DO NOT fabricate confidence percentages or benchmark values.
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public class MLPredictionDTO {

    private String modelStatus;     // Strictly "MOCK"
    private String event;           // "NORMAL", "POTHOLE", "BUMP", "SPEED_BREAKER"
    private String vibrationState;  // "NORMAL", "ENGINE_VIBRATION", "ROAD_VIBRATION"
    private Double confidence;      // STRICTLY NULL - Do NOT generate fake confidence values
    private long timestamp;
    private String description;     // Explicit notice for SIH evaluators

    public MLPredictionDTO() {
        this.modelStatus = "MOCK";
        this.event = "NORMAL";
        this.vibrationState = "NORMAL";
        this.confidence = null;
        this.timestamp = System.currentTimeMillis();
        this.description = "MOCK MODEL — TRAINED MODEL NOT INTEGRATED";
    }

    public MLPredictionDTO(String event, String vibrationState, long timestamp) {
        this.modelStatus = "MOCK";
        this.event = event;
        this.vibrationState = vibrationState;
        this.confidence = null; // Strictly null
        this.timestamp = timestamp;
        this.description = "MOCK MODEL — TRAINED MODEL NOT INTEGRATED";
    }

    public String getModelStatus() { return modelStatus; }
    public void setModelStatus(String modelStatus) { this.modelStatus = modelStatus; }

    public String getEvent() { return event; }
    public void setEvent(String event) { this.event = event; }

    public String getVibrationState() { return vibrationState; }
    public void setVibrationState(String vibrationState) { this.vibrationState = vibrationState; }

    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
