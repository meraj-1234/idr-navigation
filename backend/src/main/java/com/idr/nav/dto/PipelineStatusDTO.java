package com.idr.nav.dto;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Status representation of the 9 core processing pipeline stages:
 * 1. Sensor Ingestion
 * 2. Synchronization
 * 3. Noise Filtering
 * 4. Coordinate Transform
 * 5. ML Event Detection
 * 6. Dead Reckoning
 * 7. Kalman Fusion
 * 8. Map Matching
 * 9. Navigation Output
 */
public class PipelineStatusDTO {

    private String sensorIngestion = "STANDBY";
    private String synchronization = "STANDBY";
    private String noiseFiltering = "STANDBY";
    private String coordinateTransform = "STANDBY";
    private String mlEventDetection = "MOCK";
    private String deadReckoning = "STANDBY";
    private String kalmanFusion = "STANDBY";
    private String mapMatching = "STANDBY";
    private String navigationOutput = "STANDBY";

    public PipelineStatusDTO() {}

    public String getSensorIngestion() { return sensorIngestion; }
    public void setSensorIngestion(String sensorIngestion) { this.sensorIngestion = sensorIngestion; }

    public String getSynchronization() { return synchronization; }
    public void setSynchronization(String synchronization) { this.synchronization = synchronization; }

    public String getNoiseFiltering() { return noiseFiltering; }
    public void setNoiseFiltering(String noiseFiltering) { this.noiseFiltering = noiseFiltering; }

    public String getCoordinateTransform() { return coordinateTransform; }
    public void setCoordinateTransform(String coordinateTransform) { this.coordinateTransform = coordinateTransform; }

    public String getMlEventDetection() { return mlEventDetection; }
    public void setMlEventDetection(String mlEventDetection) { this.mlEventDetection = mlEventDetection; }

    public String getDeadReckoning() { return deadReckoning; }
    public void setDeadReckoning(String deadReckoning) { this.deadReckoning = deadReckoning; }

    public String getKalmanFusion() { return kalmanFusion; }
    public void setKalmanFusion(String kalmanFusion) { this.kalmanFusion = kalmanFusion; }

    public String getMapMatching() { return mapMatching; }
    public void setMapMatching(String mapMatching) { this.mapMatching = mapMatching; }

    public String getNavigationOutput() { return navigationOutput; }
    public void setNavigationOutput(String navigationOutput) { this.navigationOutput = navigationOutput; }

    public Map<String, String> toMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("Sensor Ingestion", sensorIngestion);
        map.put("Synchronization", synchronization);
        map.put("Noise Filtering", noiseFiltering);
        map.put("Coordinate Transform", coordinateTransform);
        map.put("ML Event Detection", mlEventDetection);
        map.put("Dead Reckoning", deadReckoning);
        map.put("Kalman Fusion", kalmanFusion);
        map.put("Map Matching", mapMatching);
        map.put("Navigation Output", navigationOutput);
        return map;
    }
}
