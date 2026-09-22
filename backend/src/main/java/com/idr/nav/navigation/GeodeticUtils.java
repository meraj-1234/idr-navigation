package com.idr.nav.navigation;

/**
 * Geodetic utilities for WGS-84 reference ellipsoid and spherical projections.
 * Converts local East-North-Up (ENU) displacement vectors in meters to geodetic latitude/longitude.
 */
public final class GeodeticUtils {

    // WGS84 constants
    public static final double WGS84_A = 6378137.0; // semi-major axis in meters
    public static final double WGS84_F = 1.0 / 298.257223563;
    public static final double WGS84_E2 = 2 * WGS84_F - WGS84_F * WGS84_F; // first eccentricity squared

    private GeodeticUtils() {}

    /**
     * Projects a coordinate (lat, lon) by displacement (deltaEast, deltaNorth) in meters.
     * Uses meridional and parallel radii of curvature.
     * 
     * @param latDeg Latitude in degrees
     * @param lonDeg Longitude in degrees
     * @param deltaEastMeters Displacement East in meters
     * @param deltaNorthMeters Displacement North in meters
     * @return double[2] = [newLatitudeDeg, newLongitudeDeg]
     */
    public static double[] projectCoordinates(double latDeg, double lonDeg, double deltaEastMeters, double deltaNorthMeters) {
        double phi = Math.toRadians(latDeg);
        double sinPhi = Math.sin(phi);

        // Radius of curvature in prime vertical (East-West)
        double N = WGS84_A / Math.sqrt(1.0 - WGS84_E2 * sinPhi * sinPhi);

        // Radius of curvature in meridian (North-South)
        double M = WGS84_A * (1.0 - WGS84_E2) / Math.pow(1.0 - WGS84_E2 * sinPhi * sinPhi, 1.5);

        double deltaLatRad = deltaNorthMeters / M;
        double deltaLonRad = deltaEastMeters / (N * Math.cos(phi));

        double newLat = latDeg + Math.toDegrees(deltaLatRad);
        double newLon = lonDeg + Math.toDegrees(deltaLonRad);

        return new double[] { newLat, newLon };
    }

    /**
     * Converts coordinate difference (target - origin) to local East and North displacements in meters.
     */
    public static double[] latLonDiffToMeters(double originLat, double originLon, double targetLat, double targetLon) {
        double phi = Math.toRadians(originLat);
        double sinPhi = Math.sin(phi);

        double N = WGS84_A / Math.sqrt(1.0 - WGS84_E2 * sinPhi * sinPhi);
        double M = WGS84_A * (1.0 - WGS84_E2) / Math.pow(1.0 - WGS84_E2 * sinPhi * sinPhi, 1.5);

        double deltaLatRad = Math.toRadians(targetLat - originLat);
        double deltaLonRad = Math.toRadians(targetLon - originLon);

        double deltaNorth = deltaLatRad * M;
        double deltaEast = deltaLonRad * (N * Math.cos(phi));

        return new double[] { deltaEast, deltaNorth };
    }

    /**
     * Computes great-circle distance between two points using Haversine formula.
     */
    public static double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2.0) * Math.sin(dLon / 2.0);

        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        return WGS84_A * c;
    }

    /**
     * Computes initial bearing in degrees from point 1 to point 2 [0, 360).
     */
    public static double initialBearing(double lat1, double lon1, double lat2, double lon2) {
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double dLon = Math.toRadians(lon2 - lon1);

        double y = Math.sin(dLon) * Math.cos(phi2);
        double x = Math.cos(phi1) * Math.sin(phi2) - Math.sin(phi1) * Math.cos(phi2) * Math.cos(dLon);

        double bearing = Math.toDegrees(Math.atan2(y, x));
        return (bearing + 360.0) % 360.0;
    }

    /**
     * Linear interpolation between two geodetic points. Fraction t is not clamped.
     */
    public static double[] interpolateLatLon(double lat1, double lon1, double lat2, double lon2, double t) {
        return new double[] { lat1 + t * (lat2 - lat1), lon1 + t * (lon2 - lon1) };
    }
}
