package com.idr.nav.controller;

import com.idr.nav.navigation.NavigationStateManager;
import com.idr.nav.simulation.SensorSimulationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST Controller for System Status, Health check, and Simulation Execution.
 */
@RestController
@RequestMapping("/api")
public class SystemStatusController {

    private final NavigationStateManager stateManager;
    private final SensorSimulationService simulationService;

    public SystemStatusController(NavigationStateManager stateManager, SensorSimulationService simulationService) {
        this.stateManager = stateManager;
        this.simulationService = simulationService;
    }

    @GetMapping("/system/status")
    public ResponseEntity<Map<String, Object>> getSystemStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("system", "IDR NAV - Intelligent Dead Reckoning System");
        status.put("version", "1.0.0-PROTOTYPE");
        status.put("status", "ONLINE");
        status.put("activeSessionId", stateManager.getActiveSessionId());
        status.put("sessionStatus", stateManager.getSessionStatus());
        status.put("sessionMode", stateManager.getSessionMode());
        status.put("navigationMode", stateManager.getNavigationMode());
        status.put("gnssAvailable", stateManager.isGnssAvailable());
        status.put("isSimulating", simulationService.isSimulating());
        // Explicit disclosure of model state
        status.put("mlModelStatus", "MOCK MODEL — TRAINED MODEL NOT INTEGRATED");
        return ResponseEntity.ok(status);
    }

    @PostMapping("/simulation/start")
    public ResponseEntity<Map<String, Object>> startSimulation() {
        simulationService.startSimulation();
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("status", "SIMULATION_STARTED");
        res.put("active", true);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/simulation/stop")
    public ResponseEntity<Map<String, Object>> stopSimulation() {
        simulationService.stopSimulation();
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("status", "SIMULATION_STOPPED");
        res.put("active", false);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/simulation/status")
    public ResponseEntity<Map<String, Object>> getSimulationStatus() {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("isSimulating", simulationService.isSimulating());
        return ResponseEntity.ok(res);
    }
}
