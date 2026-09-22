package com.idr.nav.mapping;

import com.idr.nav.navigation.GeodeticUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Baseline Map Matching Service.
 * 
 * Performs geometric orthogonal projection from raw/fused coordinates
 * onto nearest candidate road network segments.
 * 
 * DESIGN EXTENSION NOTE:
 * Designed as an extensible service. Can be backed by local polyline graphs or
 * connected to external map matching engines (OSRM / Valhalla / GraphHopper).
 */
@Service
public class MapMatchingService {

    public static class MapMatchResult {
        private final boolean matched;
        private final double latitude;
        private final double longitude;
        private final double distanceToRoadMeters;
        private final String roadName;

        public MapMatchResult(boolean matched, double latitude, double longitude, double distanceToRoadMeters, String roadName) {
            this.matched = matched;
            this.latitude = latitude;
            this.longitude = longitude;
            this.distanceToRoadMeters = distanceToRoadMeters;
            this.roadName = roadName;
        }

        public boolean isMatched() { return matched; }
        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
        public double getDistanceToRoadMeters() { return distanceToRoadMeters; }
        public String getRoadName() { return roadName; }
    }

    private final List<RoadSegment> localRoadSegments = new ArrayList<>();
    private static final double MAX_MATCHING_RADIUS_METERS = 40.0; // Distance threshold to snap to road

    public MapMatchingService() {
        // Initialize with realistic arterial road segments around the default demo area (New Delhi Urban Corridor)
        // Segment 1: Rajpath / Kartavya Path Central Avenue (Straight East-West)
        localRoadSegments.add(new RoadSegment("SEG-1", "Central Boulevard Eastbound", 28.6129, 77.2100, 28.6130, 77.2300));
        localRoadSegments.add(new RoadSegment("SEG-2", "Janpath Crossing Corridor", 28.6050, 77.2195, 28.6250, 77.2195));
        localRoadSegments.add(new RoadSegment("SEG-3", "Underpass Highway Connector", 28.6130, 77.2300, 28.6190, 77.2400));
        localRoadSegments.add(new RoadSegment("SEG-4", "Metro Tunnel Parallel Avenue", 28.6190, 77.2400, 28.6280, 77.2380));
    }

    public synchronized void registerRoadSegment(RoadSegment segment) {
        localRoadSegments.add(segment);
    }

    public synchronized void clearSegments() {
        localRoadSegments.clear();
    }

    /**
     * Replaces the candidate network with consecutive segments from the selected route polyline
     * so map-matching snaps to the demo route instead of the static Delhi corridor.
     */
    public synchronized void loadPolyline(double[] latitudes, double[] longitudes) {
        localRoadSegments.clear();
        if (latitudes == null || longitudes == null || latitudes.length < 2
                || latitudes.length != longitudes.length) {
            return;
        }
        for (int i = 0; i < latitudes.length - 1; i++) {
            localRoadSegments.add(new RoadSegment(
                    "ROUTE-SEG-" + i,
                    "Selected Route",
                    latitudes[i], longitudes[i],
                    latitudes[i + 1], longitudes[i + 1]
            ));
        }
    }

    /**
     * Matches input coordinates to closest candidate road polyline.
     */
    public MapMatchResult matchPosition(double lat, double lon) {
        if (localRoadSegments.isEmpty()) {
            return new MapMatchResult(false, lat, lon, 0.0, "NO_NETWORK_DATA");
        }

        double minDistance = Double.MAX_VALUE;
        double bestLat = lat;
        double bestLon = lon;
        String bestRoad = "UNMATCHED";

        for (RoadSegment seg : localRoadSegments) {
            // Project point onto line segment in local ENU coordinates
            double[] diffStart = GeodeticUtils.latLonDiffToMeters(seg.getStartLat(), seg.getStartLon(), lat, lon);
            double[] segVector = GeodeticUtils.latLonDiffToMeters(seg.getStartLat(), seg.getStartLon(), seg.getEndLat(), seg.getEndLon());

            double segLengthSq = segVector[0] * segVector[0] + segVector[1] * segVector[1];
            if (segLengthSq < 1e-6) continue;

            // Parametric scalar projection t clamped to [0, 1]
            double t = (diffStart[0] * segVector[0] + diffStart[1] * segVector[1]) / segLengthSq;
            t = Math.max(0.0, Math.min(1.0, t));

            // Closest point in local ENU meters
            double projEast = t * segVector[0];
            double projNorth = t * segVector[1];

            // Distance from query point to projection
            double distEast = diffStart[0] - projEast;
            double distNorth = diffStart[1] - projNorth;
            double dist = Math.sqrt(distEast * distEast + distNorth * distNorth);

            if (dist < minDistance) {
                minDistance = dist;
                double[] snapped = GeodeticUtils.projectCoordinates(seg.getStartLat(), seg.getStartLon(), projEast, projNorth);
                bestLat = snapped[0];
                bestLon = snapped[1];
                bestRoad = seg.getName();
            }
        }

        if (minDistance <= MAX_MATCHING_RADIUS_METERS) {
            return new MapMatchResult(true, bestLat, bestLon, minDistance, bestRoad);
        } else {
            // Distance is too high to snap; return raw position without pretending to match
            return new MapMatchResult(false, lat, lon, minDistance, "OFF_ROAD_EXCEEDS_RADIUS");
        }
    }
}
