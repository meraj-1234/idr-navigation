package com.idr.nav.dto;

import java.util.List;

/**
 * Full route geometry returned after route creation.
 * Contains the polyline points, metadata, and source indicator.
 */
public class RouteGeometry {

    private String routeId;
    private String fromName;
    private double fromLat;
    private double fromLon;
    private String toName;
    private double toLat;
    private double toLon;
    private double totalDistanceMeters;
    private double estimatedDurationSeconds;
    private List<RoutePoint> points;
    /** "OSRM" or "FALLBACK_INTERPOLATED" */
    private String routeSource;

    public RouteGeometry() {}

    public String getRouteId() { return routeId; }
    public void setRouteId(String routeId) { this.routeId = routeId; }

    public String getFromName() { return fromName; }
    public void setFromName(String fromName) { this.fromName = fromName; }

    public double getFromLat() { return fromLat; }
    public void setFromLat(double fromLat) { this.fromLat = fromLat; }

    public double getFromLon() { return fromLon; }
    public void setFromLon(double fromLon) { this.fromLon = fromLon; }

    public String getToName() { return toName; }
    public void setToName(String toName) { this.toName = toName; }

    public double getToLat() { return toLat; }
    public void setToLat(double toLat) { this.toLat = toLat; }

    public double getToLon() { return toLon; }
    public void setToLon(double toLon) { this.toLon = toLon; }

    public double getTotalDistanceMeters() { return totalDistanceMeters; }
    public void setTotalDistanceMeters(double totalDistanceMeters) { this.totalDistanceMeters = totalDistanceMeters; }

    public double getEstimatedDurationSeconds() { return estimatedDurationSeconds; }
    public void setEstimatedDurationSeconds(double estimatedDurationSeconds) { this.estimatedDurationSeconds = estimatedDurationSeconds; }

    public List<RoutePoint> getPoints() { return points; }
    public void setPoints(List<RoutePoint> points) { this.points = points; }

    public String getRouteSource() { return routeSource; }
    public void setRouteSource(String routeSource) { this.routeSource = routeSource; }
}
