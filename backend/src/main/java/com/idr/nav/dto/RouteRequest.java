package com.idr.nav.dto;

/**
 * Request body for POST /api/routes.
 * Contains from/to coordinates and display names.
 */
public class RouteRequest {

    private LocationDto from;
    private LocationDto to;

    public RouteRequest() {}

    public LocationDto getFrom() { return from; }
    public void setFrom(LocationDto from) { this.from = from; }

    public LocationDto getTo() { return to; }
    public void setTo(LocationDto to) { this.to = to; }

    public static class LocationDto {
        private double lat;
        private double lon;
        private String name;

        public LocationDto() {}

        public double getLat() { return lat; }
        public void setLat(double lat) { this.lat = lat; }

        public double getLon() { return lon; }
        public void setLon(double lon) { this.lon = lon; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}
