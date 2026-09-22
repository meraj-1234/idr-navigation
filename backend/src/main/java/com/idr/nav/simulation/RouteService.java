package com.idr.nav.simulation;

import com.idr.nav.dto.RouteGeometry;
import com.idr.nav.dto.RoutePoint;
import com.idr.nav.dto.RouteRequest;
import com.idr.nav.navigation.GeodeticUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * RouteService — creates routes using the public OSRM routing API.
 * Falls back to straight-line interpolation if OSRM is unreachable.
 *
 * External call: http://router.project-osrm.org/route/v1/driving/{lon},{lat};{lon},{lat}
 *                ?overview=full&geometries=geojson
 */
@Service
public class RouteService {

    private static final Logger log = LoggerFactory.getLogger(RouteService.class);
    private static final String OSRM_BASE = "https://router.project-osrm.org/route/v1/driving/";
    private static final int CONNECT_TIMEOUT_MS = 3000;
    private static final int READ_TIMEOUT_MS = 4000;

    // Stored active route (singleton — one route at a time per demo)
    private final AtomicReference<RouteGeometry> activeRoute = new AtomicReference<>(null);

    /**
     * Creates a route from the request. Tries OSRM first, falls back to interpolation.
     */
    public RouteGeometry createRoute(RouteRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Route request cannot be null");
        }
        RouteRequest.LocationDto from = request.getFrom();
        RouteRequest.LocationDto to = request.getTo();
        if (from == null || to == null) {
            throw new IllegalArgumentException("From and To locations cannot be null");
        }

        RouteGeometry geometry = null;
        try {
            geometry = fetchFromOsrm(from, to);
        } catch (Exception e) {
            log.warn("OSRM routing failed ({}), using fallback interpolation.", e.getMessage());
        }

        if (geometry == null) {
            geometry = buildInterpolatedRoute(from, to);
        }

        geometry.setRouteId("ROUTE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        activeRoute.set(geometry);
        log.info("Route created: {} pts, {} m [{}]", geometry.getPoints().size(),
                (int) geometry.getTotalDistanceMeters(), geometry.getRouteSource());
        return geometry;
    }

    public RouteGeometry getActiveRoute() {
        return activeRoute.get();
    }

    public void clearActiveRoute() {
        activeRoute.set(null);
    }

    // ---- OSRM Integration -------------------------------------------------------

    private RouteGeometry fetchFromOsrm(RouteRequest.LocationDto from, RouteRequest.LocationDto to) throws Exception {
        // OSRM expects lon,lat order
        String urlStr = OSRM_BASE
                + from.getLon() + "," + from.getLat() + ";"
                + to.getLon() + "," + to.getLat()
                + "?overview=full&geometries=geojson";

        log.info("Requesting OSRM route: {}", urlStr);

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setRequestProperty("User-Agent", "IDR-NAV-Demo/1.0");
        conn.connect();

        int status = conn.getResponseCode();
        if (status != 200) {
            throw new RuntimeException("OSRM returned HTTP " + status);
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }

        return parseOsrmResponse(sb.toString(), from, to);
    }

    /**
     * Minimal JSON parser for the OSRM GeoJSON response.
     * Parses the coordinates array from routes[0].geometry.coordinates.
     * We avoid pulling in Jackson ObjectMapper to keep dependencies clean
     * (Jackson is already on classpath via Spring Boot, so we can use it).
     */
    private RouteGeometry parseOsrmResponse(String json, RouteRequest.LocationDto from, RouteRequest.LocationDto to)
            throws Exception {
        // Use com.fasterxml.jackson.databind which is on classpath via Spring Boot
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(json);

        String code = root.path("code").asText("unknown");
        if (!"Ok".equalsIgnoreCase(code)) {
            throw new RuntimeException("OSRM code=" + code);
        }

        com.fasterxml.jackson.databind.JsonNode routes = root.path("routes");
        if (routes == null || !routes.isArray() || routes.isEmpty()) {
            throw new RuntimeException("OSRM returned no route paths");
        }

        com.fasterxml.jackson.databind.JsonNode route = routes.get(0);
        double distanceM = route.path("distance").asDouble();
        double durationS = route.path("duration").asDouble();

        com.fasterxml.jackson.databind.JsonNode coords = route.path("geometry").path("coordinates");
        if (coords == null || !coords.isArray() || coords.isEmpty()) {
            throw new RuntimeException("OSRM returned empty coordinates");
        }

        List<RoutePoint> points = new ArrayList<>();
        for (com.fasterxml.jackson.databind.JsonNode coord : coords) {
            double lon = coord.get(0).asDouble();
            double lat = coord.get(1).asDouble();
            points.add(new RoutePoint(lat, lon));
        }

        if (points.isEmpty()) throw new RuntimeException("OSRM returned empty coordinates");
        points = densify(points, 12.0);

        RouteGeometry g = new RouteGeometry();
        g.setFromName(from.getName() != null ? from.getName() : "Origin");
        g.setFromLat(from.getLat());
        g.setFromLon(from.getLon());
        g.setToName(to.getName() != null ? to.getName() : "Destination");
        g.setToLat(to.getLat());
        g.setToLon(to.getLon());
        g.setTotalDistanceMeters(distanceM);
        g.setEstimatedDurationSeconds(durationS);
        g.setPoints(points);
        g.setRouteSource("OSRM");
        return g;
    }

    // ---- Fallback Interpolation --------------------------------------------------

    /**
     * Generates 30 linearly-interpolated points between from and to.
     * Clearly marked with routeSource = "FALLBACK_INTERPOLATED".
     */
    private RouteGeometry buildInterpolatedRoute(RouteRequest.LocationDto from, RouteRequest.LocationDto to) {
        int numPoints = 30;
        List<RoutePoint> points = new ArrayList<>();

        for (int i = 0; i <= numPoints; i++) {
            double t = (double) i / numPoints;
            double lat = from.getLat() + t * (to.getLat() - from.getLat());
            double lon = from.getLon() + t * (to.getLon() - from.getLon());
            points.add(new RoutePoint(lat, lon));
        }
        points = densify(points, 12.0);

        double distM = GeodeticUtils.haversineDistance(from.getLat(), from.getLon(), to.getLat(), to.getLon());

        RouteGeometry g = new RouteGeometry();
        g.setFromName(from.getName() != null ? from.getName() : "Origin");
        g.setFromLat(from.getLat());
        g.setFromLon(from.getLon());
        g.setToName(to.getName() != null ? to.getName() : "Destination");
        g.setToLat(to.getLat());
        g.setToLon(to.getLon());
        g.setTotalDistanceMeters(distM);
        g.setEstimatedDurationSeconds(distM / 10.0); // ~10 m/s average
        g.setPoints(points);
        g.setRouteSource("FALLBACK_INTERPOLATED");
        return g;
    }

    /**
     * Inserts interpolated vertices so consecutive points are at most maxSpacingMeters apart.
     * Makes on-route marker motion look continuous rather than jumping between sparse OSRM vertices.
     */
    private List<RoutePoint> densify(List<RoutePoint> source, double maxSpacingMeters) {
        if (source == null || source.size() < 2) return source;
        List<RoutePoint> dense = new ArrayList<>();
        dense.add(source.get(0));
        for (int i = 1; i < source.size(); i++) {
            RoutePoint a = source.get(i - 1);
            RoutePoint b = source.get(i);
            double dist = GeodeticUtils.haversineDistance(a.getLat(), a.getLon(), b.getLat(), b.getLon());
            int steps = (int) Math.floor(dist / maxSpacingMeters);
            for (int s = 1; s <= steps; s++) {
                double t = s / (double) (steps + 1);
                double[] p = GeodeticUtils.interpolateLatLon(a.getLat(), a.getLon(), b.getLat(), b.getLon(), t);
                dense.add(new RoutePoint(p[0], p[1]));
            }
            dense.add(b);
        }
        return dense;
    }
}
