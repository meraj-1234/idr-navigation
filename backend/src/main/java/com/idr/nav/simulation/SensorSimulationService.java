package com.idr.nav.simulation;

import com.idr.nav.dto.GNSSSampleDTO;
import com.idr.nav.dto.IMUSampleDTO;
import com.idr.nav.navigation.GeodeticUtils;
import com.idr.nav.navigation.NavigationStateManager;
import com.idr.nav.sensor.SensorIngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

/**
 * Controlled Sensor Simulation Service.
 * 
 * Simulates physical vehicle kinematics along an urban navigation route:
 * - Generates physical 3D IMU forces (longitudinal acceleration/braking, lateral centrifugal turns, road vibration).
 * - Periodically generates GNSS observations.
 * - Simulates realistic GNSS outage in tunnels.
 * - Streams everything through the EXACT same SensorIngestionService pipeline as physical smartphones.
 */
@Service
public class SensorSimulationService {

    private static final Logger log = LoggerFactory.getLogger(SensorSimulationService.class);

    private final SensorIngestionService ingestionService;
    private final NavigationStateManager stateManager;

    private ScheduledExecutorService executorService;
    private ScheduledFuture<?> simulationTask;

    private boolean isSimulating = false;
    private final Random random = new Random();

    // Simulation state
    private List<SimulationRoutePreset.Waypoint> waypoints;
    private int currentWaypointIndex = 0;
    private double currentSimLat;
    private double currentSimLon;
    private double currentSimSpeed; // m/s
    private double currentSimHeading; // degrees
    private int stepCounter = 0;
    private boolean manualGnssOverride = false;

    public SensorSimulationService(SensorIngestionService ingestionService, NavigationStateManager stateManager) {
        this.ingestionService = ingestionService;
        this.stateManager = stateManager;
        this.waypoints = SimulationRoutePreset.getUrbanTunnelRoute();
    }

    public synchronized void startSimulation() {
        if (isSimulating) {
            log.info("Simulation is already running.");
            return;
        }

        this.waypoints = SimulationRoutePreset.getUrbanTunnelRoute();
        this.currentWaypointIndex = 0;
        SimulationRoutePreset.Waypoint startPoint = waypoints.get(0);
        this.currentSimLat = startPoint.lat;
        this.currentSimLon = startPoint.lon;
        this.currentSimSpeed = 0.0;
        this.currentSimHeading = 90.0; // Eastbound
        this.stepCounter = 0;
        this.manualGnssOverride = false;

        stateManager.startSession("Simulation Urban Route", "SIMULATION", currentSimLat, currentSimLon);

        executorService = Executors.newSingleThreadScheduledExecutor();
        // Run simulation loop at 10 Hz (every 100 ms)
        simulationTask = executorService.scheduleAtFixedRate(this::simulationStep, 0, 100, TimeUnit.MILLISECONDS);
        this.isSimulating = true;
        log.info("Sensor simulation started at 10 Hz.");
    }

    public synchronized void stopSimulation() {
        if (!isSimulating) return;

        if (simulationTask != null) {
            simulationTask.cancel(true);
        }
        if (executorService != null) {
            executorService.shutdownNow();
        }
        this.isSimulating = false;
        stateManager.stopSession();
        log.info("Sensor simulation stopped.");
    }

    private void simulationStep() {
        try {
            if (currentWaypointIndex >= waypoints.size()) {
                currentWaypointIndex = 0; // Loop back
            }

            SimulationRoutePreset.Waypoint target = waypoints.get(currentWaypointIndex);
            double distToTarget = GeodeticUtils.haversineDistance(currentSimLat, currentSimLon, target.lat, target.lon);

            // If close to waypoint, advance to next
            if (distToTarget < 8.0 && currentWaypointIndex < waypoints.size() - 1) {
                currentWaypointIndex++;
                target = waypoints.get(currentWaypointIndex);
                log.info("Reached waypoint: {} -> Next: {}", target.landmark, target.landmark);
            }

            double dt = 0.1; // 100 ms
            stepCounter++;

            // Calculate bearing to target
            double targetBearing = GeodeticUtils.initialBearing(currentSimLat, currentSimLon, target.lat, target.lon);
            double headingDiff = (targetBearing - currentSimHeading + 540.0) % 360.0 - 180.0;
            double maxTurnRatePerStep = 6.0; // Max 60 deg/sec
            double turnThisStep = Math.max(-maxTurnRatePerStep, Math.min(maxTurnRatePerStep, headingDiff * 0.4));
            currentSimHeading = (currentSimHeading + turnThisStep + 360.0) % 360.0;

            // Speed adjustment towards target speed
            double targetSpeed = target.targetSpeed;
            double speedDiff = targetSpeed - currentSimSpeed;
            double fwdAccel = Math.max(-2.5, Math.min(2.0, speedDiff * 0.5)); // Accel or braking m/s^2
            currentSimSpeed = Math.max(0.0, currentSimSpeed + fwdAccel * dt);

            // Lateral acceleration (centrifugal force = v * yaw_rate)
            double yawRateRad = Math.toRadians(turnThisStep / dt);
            double lateralAccel = currentSimSpeed * yawRateRad;

            // Vertical acceleration base = gravity (9.80665 m/s^2)
            double verticalAccel = 9.80665 + (random.nextDouble() - 0.5) * 0.4; // Base road vibration

            // Inject shock anomaly if waypoint specifies it
            if ("POTHOLE".equalsIgnoreCase(target.roadEvent) && distToTarget < 15.0) {
                verticalAccel += 6.8 * (random.nextDouble() > 0.5 ? 1 : -1);
            } else if ("SPEED_BREAKER".equalsIgnoreCase(target.roadEvent) && distToTarget < 12.0) {
                verticalAccel += 3.8;
                fwdAccel -= 1.2; // slight deceleration
            } else if ("BUMP".equalsIgnoreCase(target.roadEvent) && distToTarget < 12.0) {
                verticalAccel += 4.5;
            }

            // Move simulated vehicle in ground-truth geodetic frame
            double headingRad = Math.toRadians(currentSimHeading);
            double deltaNorth = currentSimSpeed * Math.cos(headingRad) * dt;
            double deltaEast = currentSimSpeed * Math.sin(headingRad) * dt;
            double[] nextPos = GeodeticUtils.projectCoordinates(currentSimLat, currentSimLon, deltaEast, deltaNorth);
            currentSimLat = nextPos[0];
            currentSimLon = nextPos[1];

            long now = System.currentTimeMillis();

            double pitchRad = Math.toRadians(75.0);
            double cosPitch = Math.cos(pitchRad);
            double sinPitch = Math.sin(pitchRad);

            double deviceAx = lateralAccel + (random.nextDouble() - 0.5) * 0.1;
            double deviceAy = fwdAccel * cosPitch + verticalAccel * sinPitch + (random.nextDouble() - 0.5) * 0.1;
            double deviceAz = -fwdAccel * sinPitch + verticalAccel * cosPitch;

            // Construct IMU Sample (Phone Windshield Mount: 75 deg pitch)
            IMUSampleDTO imu = new IMUSampleDTO();
            imu.setTimestamp(now);
            imu.setAccelerometerX(deviceAx);
            imu.setAccelerometerY(deviceAy);
            imu.setAccelerometerZ(deviceAz);
            imu.setGyroscopeX((random.nextDouble() - 0.5) * 0.02);
            imu.setGyroscopeY((random.nextDouble() - 0.5) * 0.02);
            imu.setGyroscopeZ(yawRateRad);
            imu.setAlpha(360.0 - currentSimHeading); // Browser compass orientation
            imu.setBeta(75.0); // 75 deg pitch mount
            imu.setGamma(0.0);
            imu.setSessionId(stateManager.getActiveSessionId());

            // Ingest IMU through the unified pipeline
            ingestionService.ingestIMU(imu);

            // Handle GNSS simulation (Tunnel zone vs open sky)
            boolean isInTunnel = target.isGnssDeniedZone;
            if (isInTunnel && stateManager.isGnssAvailable() && !manualGnssOverride) {
                stateManager.triggerGnssLoss();
            } else if (!isInTunnel && !stateManager.isGnssAvailable() && !manualGnssOverride) {
                stateManager.triggerGnssRestore();
            }

            // GNSS fix at ~2 Hz (every 5 steps)
            if (stepCounter % 5 == 0 && stateManager.isGnssAvailable()) {
                // Realistic small GNSS jitter (~1-2 meters)
                double jitterLat = (random.nextDouble() - 0.5) * 0.00002;
                double jitterLon = (random.nextDouble() - 0.5) * 0.00002;

                GNSSSampleDTO gnss = new GNSSSampleDTO();
                gnss.setTimestamp(now);
                gnss.setLatitude(currentSimLat + jitterLat);
                gnss.setLongitude(currentSimLon + jitterLon);
                gnss.setSpeed(currentSimSpeed);
                gnss.setHeading(currentSimHeading);
                gnss.setAltitude(215.0);
                gnss.setAccuracy(2.8); // 2.8m typical open-sky accuracy
                gnss.setSessionId(stateManager.getActiveSessionId());

                ingestionService.ingestGNSS(gnss);
            }

        } catch (Exception e) {
            log.error("Error in simulation step", e);
        }
    }

    public synchronized void setManualGnssOverride(boolean override) {
        this.manualGnssOverride = override;
    }

    public boolean isSimulating() {
        return isSimulating;
    }
}
