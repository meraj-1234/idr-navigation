package com.idr.nav.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class NavigationStateDTO {

    private String sessionId;
    private long timestamp;
    private double latitude;
    private double longitude;
    private Double velocity; // m/s
    private Double heading;  // degrees (0 - 360)
    private String source;   // "GNSS", "INS", "FUSION"
    private boolean gnssAvailable;
    private String navigationMode; // "GNSS", "DEAD_RECKONING", "FUSION"

    // Component coordinate traces
    private Double gnssLatitude;
    private Double gnssLongitude;
    private Double gnssAccuracy;

    private Double deadReckoningLatitude;
    private Double deadReckoningLongitude;

    private Double mapMatchedLatitude;
    private Double mapMatchedLongitude;

    // Outage event timestamps
    private Long gnssLossTimestamp;
    private Long gnssRestoreTimestamp;

    // Associated ML prediction (Always MOCK with null confidence)
    private MLPredictionDTO mlPrediction;

    // Pipeline status map
    private PipelineStatusDTO pipelineStatus;

    // Latest IMU sample for real-time dashboard telemetry
    private IMUSampleDTO latestImuSample;

    // Route navigation progress fields
    private String activeRouteId;
    private Double routeProgress;          // 0.0 - 1.0
    private Double distanceRemainingMeters;
    private String routeNavigationStatus;  // "IDLE" | "ROUTE_SELECTED" | "NAVIGATION_ACTIVE" | "COMPLETED" | "STOPPED"

    public NavigationStateDTO() {}

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public Double getVelocity() { return velocity; }
    public void setVelocity(Double velocity) { this.velocity = velocity; }

    public Double getHeading() { return heading; }
    public void setHeading(Double heading) { this.heading = heading; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public boolean isGnssAvailable() { return gnssAvailable; }
    public void setGnssAvailable(boolean gnssAvailable) { this.gnssAvailable = gnssAvailable; }

    public String getNavigationMode() { return navigationMode; }
    public void setNavigationMode(String navigationMode) { this.navigationMode = navigationMode; }

    public Double getGnssLatitude() { return gnssLatitude; }
    public void setGnssLatitude(Double gnssLatitude) { this.gnssLatitude = gnssLatitude; }

    public Double getGnssLongitude() { return gnssLongitude; }
    public void setGnssLongitude(Double gnssLongitude) { this.gnssLongitude = gnssLongitude; }

    public Double getGnssAccuracy() { return gnssAccuracy; }
    public void setGnssAccuracy(Double gnssAccuracy) { this.gnssAccuracy = gnssAccuracy; }

    public Double getDeadReckoningLatitude() { return deadReckoningLatitude; }
    public void setDeadReckoningLatitude(Double deadReckoningLatitude) { this.deadReckoningLatitude = deadReckoningLatitude; }

    public Double getDeadReckoningLongitude() { return deadReckoningLongitude; }
    public void setDeadReckoningLongitude(Double deadReckoningLongitude) { this.deadReckoningLongitude = deadReckoningLongitude; }

    public Double getMapMatchedLatitude() { return mapMatchedLatitude; }
    public void setMapMatchedLatitude(Double mapMatchedLatitude) { this.mapMatchedLatitude = mapMatchedLatitude; }

    public Double getMapMatchedLongitude() { return mapMatchedLongitude; }
    public void setMapMatchedLongitude(Double mapMatchedLongitude) { this.mapMatchedLongitude = mapMatchedLongitude; }

    public Long getGnssLossTimestamp() { return gnssLossTimestamp; }
    public void setGnssLossTimestamp(Long gnssLossTimestamp) { this.gnssLossTimestamp = gnssLossTimestamp; }

    public Long getGnssRestoreTimestamp() { return gnssRestoreTimestamp; }
    public void setGnssRestoreTimestamp(Long gnssRestoreTimestamp) { this.gnssRestoreTimestamp = gnssRestoreTimestamp; }

    public MLPredictionDTO getMlPrediction() { return mlPrediction; }
    public void setMlPrediction(MLPredictionDTO mlPrediction) { this.mlPrediction = mlPrediction; }

    public PipelineStatusDTO getPipelineStatus() { return pipelineStatus; }
    public void setPipelineStatus(PipelineStatusDTO pipelineStatus) { this.pipelineStatus = pipelineStatus; }

    public IMUSampleDTO getLatestImuSample() { return latestImuSample; }
    public void setLatestImuSample(IMUSampleDTO latestImuSample) { this.latestImuSample = latestImuSample; }

    public String getActiveRouteId() { return activeRouteId; }
    public void setActiveRouteId(String activeRouteId) { this.activeRouteId = activeRouteId; }

    public Double getRouteProgress() { return routeProgress; }
    public void setRouteProgress(Double routeProgress) { this.routeProgress = routeProgress; }

    public Double getDistanceRemainingMeters() { return distanceRemainingMeters; }
    public void setDistanceRemainingMeters(Double distanceRemainingMeters) { this.distanceRemainingMeters = distanceRemainingMeters; }

    public String getRouteNavigationStatus() { return routeNavigationStatus; }
    public void setRouteNavigationStatus(String routeNavigationStatus) { this.routeNavigationStatus = routeNavigationStatus; }
}
