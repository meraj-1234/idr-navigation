package com.idr.nav.controller;

import com.idr.nav.dto.*;
import com.idr.nav.navigation.NavigationStateManager;
import com.idr.nav.simulation.RouteBasedSimulationService;
import com.idr.nav.simulation.RouteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for Navigation Session Lifecycle, State inspection,
 * Trajectory history, GNSS Outage/Restoration triggers,
 * and Route-Based Navigation demo endpoints.
 */
@RestController
@RequestMapping("/api/navigation")
public class NavigationController {

    private final NavigationStateManager stateManager;
    private final RouteService routeService;
    private final RouteBasedSimulationService routeSimService;

    public NavigationController(NavigationStateManager stateManager,
                                RouteService routeService,
                                RouteBasedSimulationService routeSimService) {
        this.stateManager    = stateManager;
        this.routeService    = routeService;
        this.routeSimService = routeSimService;
    }

    // ---- Session lifecycle -------------------------------------------------------

    @PostMapping("/session/start")
    public ResponseEntity<NavigationStateDTO> startSession(@RequestBody(required = false) SessionControlDTO req) {
        String name = req != null ? req.getSessionName() : "Nav Session";
        String mode = req != null ? req.getMode() : "SIMULATION";
        Double lat  = req != null ? req.getInitialLatitude() : null;
        Double lon  = req != null ? req.getInitialLongitude() : null;

        stateManager.startSession(name, mode, lat, lon);
        return ResponseEntity.ok(stateManager.getCurrentState());
    }

    @PostMapping("/session/pause")
    public ResponseEntity<Void> pauseSession() {
        stateManager.pauseSession();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/session/stop")
    public ResponseEntity<Void> stopSession() {
        stateManager.stopSession();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/session/reset")
    public ResponseEntity<Void> resetSession() {
        stateManager.resetSession();
        return ResponseEntity.ok().build();
    }

    // ---- GNSS controls ----------------------------------------------------------

    @PostMapping("/gnss/loss")
    public ResponseEntity<NavigationStateDTO> triggerGnssLoss() {
        stateManager.triggerGnssLoss();
        return ResponseEntity.ok(stateManager.getCurrentState());
    }

    @PostMapping("/gnss/restore")
    public ResponseEntity<NavigationStateDTO> triggerGnssRestore() {
        stateManager.triggerGnssRestore();
        return ResponseEntity.ok(stateManager.getCurrentState());
    }

    // ---- State / trajectory / events --------------------------------------------

    @GetMapping("/state")
    public ResponseEntity<NavigationStateDTO> getCurrentState() {
        return ResponseEntity.ok(stateManager.getCurrentState());
    }

    @GetMapping("/trajectory")
    public ResponseEntity<List<NavigationStateDTO>> getTrajectory() {
        return ResponseEntity.ok(stateManager.getTrajectoryHistory());
    }

    @GetMapping("/events")
    public ResponseEntity<List<SystemEventDTO>> getEvents() {
        return ResponseEntity.ok(stateManager.getRecentEvents());
    }

    @GetMapping("/pipeline")
    public ResponseEntity<PipelineStatusDTO> getPipelineStatus() {
        return ResponseEntity.ok(stateManager.getPipelineStatus());
    }

    // ---- Route-Based Navigation Demo --------------------------------------------

    /**
     * POST /api/navigation/routes
     * Body: { from: {lat,lon,name}, to: {lat,lon,name} }
     * Returns: RouteGeometry (polyline + metadata)
     */
    @PostMapping("/routes")
    public ResponseEntity<?> createRoute(@RequestBody RouteRequest request) {
        try {
            RouteGeometry geometry = routeService.createRoute(request);
            stateManager.setActiveRouteId(geometry.getRouteId());
            stateManager.setRouteNavigationStatus("ROUTE_SELECTED");
            stateManager.placeAtRouteStart(geometry.getFromLat(), geometry.getFromLon());
            stateManager.logEvent("ROUTE_CREATED",
                    "Route created: " + geometry.getFromName() + " → " + geometry.getToName()
                    + " (" + (int) geometry.getTotalDistanceMeters() + " m, source: " + geometry.getRouteSource() + ")",
                    "SUCCESS");
            return ResponseEntity.ok(geometry);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Route creation failed: " + e.getMessage()));
        }
    }

    /**
     * GET /api/navigation/routes/active
     * Returns the currently active route geometry (for page-reload recovery).
     */
    @GetMapping("/routes/active")
    public ResponseEntity<?> getActiveRoute() {
        RouteGeometry route = routeService.getActiveRoute();
        if (route == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(route);
    }

    /**
     * POST /api/navigation/routes/navigation/start
     * Starts the route-based simulation along the active route.
     */
    @PostMapping("/routes/navigation/start")
    public ResponseEntity<?> startRouteNavigation() {
        RouteGeometry route = routeService.getActiveRoute();
        if (route == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No active route. Call POST /api/navigation/routes first."));
        }
        routeSimService.startNavigation(route);
        return ResponseEntity.ok(Map.of("status", "NAVIGATION_ACTIVE", "routeId", route.getRouteId()));
    }

    /**
     * POST /api/navigation/routes/navigation/stop
     * Stops route-based navigation (keeps route in memory).
     */
    @PostMapping("/routes/navigation/stop")
    public ResponseEntity<Void> stopRouteNavigation() {
        routeSimService.stopNavigation();
        return ResponseEntity.ok().build();
    }

    /**
     * POST /api/navigation/routes/navigation/reset
     * Full demo reset: stop navigation + clear route + reset session.
     */
    @PostMapping("/routes/navigation/reset")
    public ResponseEntity<Void> resetDemo() {
        routeSimService.stopNavigation();
        routeService.clearActiveRoute();
        stateManager.resetSession();
        return ResponseEntity.ok().build();
    }
}
