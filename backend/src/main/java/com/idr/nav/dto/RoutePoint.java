package com.idr.nav.dto;

/**
 * A single coordinate point on a route polyline.
 */
public class RoutePoint {
    private double lat;
    private double lon;

    public RoutePoint() {}

    public RoutePoint(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }

    public double getLon() { return lon; }
    public void setLon(double lon) { this.lon = lon; }
}
