package com.idr.nav.simulation;

import java.util.ArrayList;
import java.util.List;

/**
 * Predefined realistic route presets for navigation simulation.
 * Includes straight acceleration corridors, 90-degree turns,
 * speed breakers, potholes, and a designated GNSS-denied tunnel / underpass zone.
 */
public class SimulationRoutePreset {

    public static class Waypoint {
        public final double lat;
        public final double lon;
        public final double targetSpeed; // m/s
        public final String landmark;
        public final boolean isGnssDeniedZone;
        public final String roadEvent; // "NORMAL", "SPEED_BREAKER", "POTHOLE", "BUMP"

        public Waypoint(double lat, double lon, double targetSpeed, String landmark, boolean isGnssDeniedZone, String roadEvent) {
            this.lat = lat;
            this.lon = lon;
            this.targetSpeed = targetSpeed;
            this.landmark = landmark;
            this.isGnssDeniedZone = isGnssDeniedZone;
            this.roadEvent = roadEvent;
        }
    }

    public static List<Waypoint> getUrbanTunnelRoute() {
        List<Waypoint> route = new ArrayList<>();
        // Starting Point: Central Boulevard Open Sky
        route.add(new Waypoint(28.61290, 77.21000, 10.0, "Start - Central Plaza Open Sky", false, "NORMAL"));
        route.add(new Waypoint(28.61292, 77.21400, 14.0, "Boulevard Straight Cruise", false, "NORMAL"));
        route.add(new Waypoint(28.61295, 77.21700, 8.0, "Approaching Intersection Speed Breaker", false, "SPEED_BREAKER"));
        route.add(new Waypoint(28.61300, 77.22100, 13.0, "Boulevard Eastbound", false, "NORMAL"));
        // Tunnel Entry: GNSS-Denied Underpass
        route.add(new Waypoint(28.61305, 77.22500, 12.0, "Tunnel Entrance [GNSS BLOCKED]", true, "NORMAL"));
        route.add(new Waypoint(28.61310, 77.22900, 11.0, "Subterranean Tunnel Segment 1", true, "NORMAL"));
        route.add(new Waypoint(28.61500, 77.23200, 10.0, "Underground Curve / Pothole Anomaly", true, "POTHOLE"));
        route.add(new Waypoint(28.61800, 77.23400, 12.0, "Subterranean Tunnel Segment 2", true, "NORMAL"));
        // Tunnel Exit: Sky clearance restored
        route.add(new Waypoint(28.62100, 77.23500, 14.0, "Tunnel Exit [GNSS RESTORED]", false, "NORMAL"));
        route.add(new Waypoint(28.62400, 77.23600, 12.0, "Northbound Avenue", false, "BUMP"));
        route.add(new Waypoint(28.62700, 77.23650, 10.0, "Destination Terminal Approach", false, "NORMAL"));
        route.add(new Waypoint(28.62900, 77.23700, 0.0, "Destination Terminal Arrival", false, "NORMAL"));
        return route;
    }
}
