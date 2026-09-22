package com.idr.nav.simulation;

import com.idr.nav.dto.GNSSSampleDTO;
import com.idr.nav.dto.IMUSampleDTO;
import com.idr.nav.dto.RouteGeometry;
import com.idr.nav.dto.RoutePoint;
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
 * Route-Based Simulation Service.
 *
 * Drives the simulated vehicle along a user-selected route (RouteGeometry)
 * by interpolating along the polyline (not free-space heading integration).
 * All sensor data flows through SensorIngestionService → full IDR pipeline.
 *
 * GNSS-denied behaviour:
 *   - Ground-truth continues along the route (movement does not stop).
 *   - Simulated GNSS fixes are withheld.
 *   - IMU heading/speed biases feed the existing dummy dead-reckoning engine.
 *
 * GNSS restore behaviour:
 *   - GNSS fixes resume at the current on-route ground-truth position.
 *   - SensorIngestionService blends the estimated position back onto the route.
 */
@Service
public class RouteBasedSimulationService {

    private static final Logger log = LoggerFactory.getLogger(RouteBasedSimulationService.class);

    private static final double NOMINAL_SPEED_MPS = 11.0; // ~40 km/h
    private static final double DT_SECONDS = 0.1;

    private final SensorIngestionService ingestionService;
    private final NavigationStateManager stateManager;

    private ScheduledExecutorService executorService;
    private ScheduledFuture<?> simulationTask;

    private volatile boolean isRunning = false;
    private final Random random = new Random(42L); // seeded for reproducibility

    private List<RoutePoint> routePoints;
    private double[] cumulativeMeters;
    private double routeLengthMeters;

    private double coveredDistance = 0.0;
    private double gtLat;
    private double gtLon;
    private double gtSpeed;
    private double gtHeading;
    private int currentSegmentIndex = 0;

    private double driftHeadingBias = 0.0;
    private double driftSpeedBias = 0.0;
    private int stepCounter = 0;

    public RouteBasedSimulationService(SensorIngestionService ingestionService,
                                       NavigationStateManager stateManager) {
        this.ingestionService = ingestionService;
        this.stateManager = stateManager;
    }

    public synchronized void startNavigation(RouteGeometry route) {
        if (isRunning) stopNavigation();

        this.routePoints = route.getPoints();
        buildCumulativeDistances();
        this.coveredDistance = 0.0;
        this.driftHeadingBias = 0.0;
        this.driftSpeedBias = 0.0;
        this.stepCounter = 0;
        this.currentSegmentIndex = 0;

        RoutePoint start = routePoints.get(0);
        this.gtLat = start.getLat();
        this.gtLon = start.getLon();
        this.gtSpeed = 5.0; // Start with immediate visible forward momentum
        this.gtHeading = headingAtDistance(0.0);

        stateManager.startSession("Route Navigation: " + route.getFromName() + " → " + route.getToName(),
                "SIMULATION", gtLat, gtLon);
        stateManager.setActiveRouteId(route.getRouteId());
        stateManager.setRouteNavigationStatus("NAVIGATION_ACTIVE");
        stateManager.setRouteProgress(0.0);
        stateManager.setDistanceRemainingMeters(routeLengthMeters);

        ingestionService.resetForRoute(gtLat, gtLon, gtHeading, gtSpeed, routePoints);

        executorService = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "route-sim");
            t.setDaemon(true);
            return t;
        });
        simulationTask = executorService.scheduleAtFixedRate(this::simulationStep, 0, 100, TimeUnit.MILLISECONDS);
        isRunning = true;

        stateManager.logEvent("NAVIGATION_STARTED",
                "Route navigation started: " + route.getFromName() + " → " + route.getToName(), "SUCCESS");
        log.info("Route-based simulation started ({} pts, {:.0f}m)", routePoints.size(), routeLengthMeters);
    }

    public synchronized void stopNavigation() {
        if (!isRunning) return;
        if (simulationTask != null) simulationTask.cancel(true);
        if (executorService != null) executorService.shutdownNow();
        isRunning = false;
        if (!"COMPLETED".equals(stateManager.getRouteNavigationStatus())) {
            stateManager.setRouteNavigationStatus("STOPPED");
        }
        stateManager.stopSession();
        log.info("Route-based simulation stopped.");
    }

    public boolean isRunning() { return isRunning; }

    private void simulationStep() {
        try {
            if (coveredDistance >= routeLengthMeters && routeLengthMeters > 0) {
                onRouteComplete();
                return;
            }

            stepCounter++;
            double dt = DT_SECONDS;

            // Scale target speed dynamically so route completes in ~45-60 seconds with clear visible motion
            double targetSpeed = Math.max(22.0, Math.min(55.0, routeLengthMeters / 45.0));
            gtSpeed = gtSpeed + Math.max(-2.0, Math.min(3.0, (targetSpeed - gtSpeed) * 0.4)) * dt;
            gtSpeed = Math.max(3.0, Math.min(targetSpeed, gtSpeed));

            coveredDistance = Math.min(routeLengthMeters, coveredDistance + gtSpeed * dt);
            double[] pos = positionAtDistance(coveredDistance);
            double nextHeading = headingAtDistance(coveredDistance);
            double headingDiff = ((nextHeading - gtHeading + 540.0) % 360.0) - 180.0;
            double turn = Math.max(-8.0, Math.min(8.0, headingDiff));
            gtHeading = nextHeading;
            gtLat = pos[0];
            gtLon = pos[1];

            double distRemaining = Math.max(0.0, routeLengthMeters - coveredDistance);
            double routeProgress = routeLengthMeters <= 0 ? 1.0 : Math.min(1.0, coveredDistance / routeLengthMeters);
            stateManager.setDistanceRemainingMeters(distRemaining);
            stateManager.setRouteProgress(routeProgress);

            boolean gnssOn = stateManager.isGnssAvailable();
            if (!gnssOn) {
                driftHeadingBias += (random.nextDouble() - 0.45) * 0.08;
                driftHeadingBias = Math.max(-8.0, Math.min(8.0, driftHeadingBias));
                driftSpeedBias = Math.min(driftSpeedBias + 0.005, 1.5);
            } else {
                driftHeadingBias *= 0.85;
                driftSpeedBias *= 0.85;
            }

            double imuHeading = gtHeading + driftHeadingBias;
            double imuSpeed = gtSpeed + driftSpeedBias;

            double yawRateRad = Math.toRadians(turn / dt);
            double lateralAccel = imuSpeed * yawRateRad;
            double fwdAccel = (imuSpeed - gtSpeed) / dt;
            double verticalAccel = 9.80665 + (random.nextDouble() - 0.5) * 0.3;

            double pitchRad = Math.toRadians(75.0);
            double cosPitch = Math.cos(pitchRad);
            double sinPitch = Math.sin(pitchRad);

            double deviceAx = lateralAccel + (random.nextDouble() - 0.5) * 0.08;
            double deviceAy = fwdAccel * cosPitch + verticalAccel * sinPitch + (random.nextDouble() - 0.5) * 0.08;
            double deviceAz = -fwdAccel * sinPitch + verticalAccel * cosPitch;

            long now = System.currentTimeMillis();

            IMUSampleDTO imu = new IMUSampleDTO();
            imu.setTimestamp(now);
            imu.setAccelerometerX(deviceAx);
            imu.setAccelerometerY(deviceAy);
            imu.setAccelerometerZ(deviceAz);
            imu.setGyroscopeX((random.nextDouble() - 0.5) * 0.015);
            imu.setGyroscopeY((random.nextDouble() - 0.5) * 0.015);
            imu.setGyroscopeZ(yawRateRad);
            imu.setAlpha(360.0 - imuHeading);
            imu.setBeta(75.0);
            imu.setGamma(0.0);
            imu.setSessionId(stateManager.getActiveSessionId());

            if (gnssOn) {
                GNSSSampleDTO gnss = new GNSSSampleDTO();
                gnss.setTimestamp(now);
                gnss.setLatitude(gtLat);
                gnss.setLongitude(gtLon);
                gnss.setSpeed(gtSpeed);
                gnss.setHeading(gtHeading);
                gnss.setAltitude(210.0);
                gnss.setAccuracy(2.5);
                gnss.setSessionId(stateManager.getActiveSessionId());
                ingestionService.ingestGNSS(gnss);
            }

            ingestionService.ingestIMU(imu);

            if (coveredDistance >= routeLengthMeters) {
                onRouteComplete();
            }
        } catch (Exception e) {
            log.error("Error in route simulation step", e);
        }
    }

    private void onRouteComplete() {
        if (!isRunning) return;
        log.info("Route navigation completed.");
        stateManager.setRouteNavigationStatus("COMPLETED");
        stateManager.setRouteProgress(1.0);
        stateManager.setDistanceRemainingMeters(0.0);
        stateManager.logEvent("ROUTE_COMPLETED", "Vehicle has arrived at the destination.", "SUCCESS");
        stopNavigation();
    }

    private void buildCumulativeDistances() {
        int n = routePoints.size();
        cumulativeMeters = new double[n];
        cumulativeMeters[0] = 0.0;
        for (int i = 1; i < n; i++) {
            RoutePoint a = routePoints.get(i - 1);
            RoutePoint b = routePoints.get(i);
            cumulativeMeters[i] = cumulativeMeters[i - 1]
                    + GeodeticUtils.haversineDistance(a.getLat(), a.getLon(), b.getLat(), b.getLon());
        }
        routeLengthMeters = cumulativeMeters[n - 1];
        if (routeLengthMeters <= 0) {
            routeLengthMeters = 1.0;
        }
    }

    private double[] positionAtDistance(double distanceMeters) {
        int n = routePoints.size();
        if (distanceMeters <= 0) {
            currentSegmentIndex = 0;
            RoutePoint p = routePoints.get(0);
            return new double[] { p.getLat(), p.getLon() };
        }
        if (distanceMeters >= cumulativeMeters[n - 1]) {
            currentSegmentIndex = Math.max(0, n - 2);
            RoutePoint p = routePoints.get(n - 1);
            return new double[] { p.getLat(), p.getLon() };
        }
        int i = 1;
        while (i < n && cumulativeMeters[i] < distanceMeters) {
            i++;
        }
        currentSegmentIndex = Math.max(0, i - 1);
        RoutePoint a = routePoints.get(i - 1);
        RoutePoint b = routePoints.get(i);
        double segLen = cumulativeMeters[i] - cumulativeMeters[i - 1];
        double t = segLen <= 1e-6 ? 1.0 : (distanceMeters - cumulativeMeters[i - 1]) / segLen;
        return GeodeticUtils.interpolateLatLon(a.getLat(), a.getLon(), b.getLat(), b.getLon(), t);
    }

    private double headingAtDistance(double distanceMeters) {
        int n = routePoints.size();
        if (n < 2) return gtHeading;
        int i = currentSegmentIndex;
        if (i < 0) i = 0;
        if (i > n - 2) i = n - 2;
        if (cumulativeMeters != null) {
            int j = 1;
            while (j < n && cumulativeMeters[j] < distanceMeters) {
                j++;
            }
            i = Math.max(0, Math.min(n - 2, j - 1));
        }
        RoutePoint a = routePoints.get(i);
        RoutePoint b = routePoints.get(i + 1);
        return GeodeticUtils.initialBearing(a.getLat(), a.getLon(), b.getLat(), b.getLon());
    }
}
