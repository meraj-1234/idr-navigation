package com.idr.nav.mapping;

public class RoadSegment {
    private final String id;
    private final String name;
    private final double startLat;
    private final double startLon;
    private final double endLat;
    private final double endLon;

    public RoadSegment(String id, String name, double startLat, double startLon, double endLat, double endLon) {
        this.id = id;
        this.name = name;
        this.startLat = startLat;
        this.startLon = startLon;
        this.endLat = endLat;
        this.endLon = endLon;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public double getStartLat() { return startLat; }
    public double getStartLon() { return startLon; }
    public double getEndLat() { return endLat; }
    public double getEndLon() { return endLon; }
}
