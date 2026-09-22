package com.idr.nav.controller;

import com.idr.nav.dto.GNSSSampleDTO;
import com.idr.nav.dto.IMUSampleDTO;
import com.idr.nav.dto.NavigationStateDTO;
import com.idr.nav.sensor.SensorIngestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Ingesting Raw Sensor Data (IMU & GNSS)
 * from smartphone browsers or remote telemetry agents.
 */
@RestController
@RequestMapping("/api/sensors")
public class SensorController {

    private final SensorIngestionService ingestionService;

    public SensorController(SensorIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/imu")
    public ResponseEntity<NavigationStateDTO> ingestIMU(@RequestBody IMUSampleDTO imu) {
        NavigationStateDTO state = ingestionService.ingestIMU(imu);
        return ResponseEntity.ok(state);
    }

    @PostMapping("/gnss")
    public ResponseEntity<Void> ingestGNSS(@RequestBody GNSSSampleDTO gnss) {
        ingestionService.ingestGNSS(gnss);
        return ResponseEntity.ok().build();
    }
}
