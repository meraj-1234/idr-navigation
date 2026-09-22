package com.idr.nav.navigation;

import com.idr.nav.dto.IMUSampleDTO;
import com.idr.nav.ml.AbnormalSignalHandler;
import com.idr.nav.sensor.NoiseFilter;
import com.idr.nav.sensor.PhoneToVehicleAlignmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Baseline Dead Reckoning (Inertial Navigation System) Engine.
 * 
 * Performs:
 * 1. Phone-to-vehicle body frame rotation.
 * 2. Gravity compensation on vertical axis.
 * 3. Zero-Velocity Update (ZUPT) detection when stopped.
 * 4. Forward acceleration numerical integration.
 * 5. Heading integration / orientation tracking.
 * 6. WGS-84 geodetic coordinate projection.
 */
@Service
public class DeadReckoningEngine {

    private static final Logger log = LoggerFactory.getLogger(DeadReckoningEngine.class);

    private final PhoneToVehicleAlignmentService alignmentService;
    private final NoiseFilter noiseFilter;

    // Navigation state variables
    private double currentLatitude = 28.6139; // Default starting location (e.g. New Delhi)
    private double currentLongitude = 77.2090;
    private double forwardVelocity = 0.0;     // m/s
    private double currentHeadingDeg = 0.0;   // 0 = North, 90 = East, 180 = South, 270 = West
    private long lastUpdateTimestamp = 0;

    // Physical and numerical thresholds
    private static final double GRAVITY = 9.80665; // m/s^2
    private static final double ZUPT_ACCEL_VARIANCE_THRESH = 0.20; // m/s^2
    private static final double ZUPT_GYRO_THRESH = 0.08;          // rad/s
    private static final double VELOCITY_DRAG_DECAY = 0.015;      // Light aerodynamic & rolling resistance decay
    private static final double MAX_REALISTIC_VELOCITY = 60.0;    // 60 m/s (~216 km/h) cap for safety

    public DeadReckoningEngine(PhoneToVehicleAlignmentService alignmentService, NoiseFilter noiseFilter) {
        this.alignmentService = alignmentService;
        this.noiseFilter = noiseFilter;
    }

    public synchronized void initializeState(double initialLat, double initialLon, double initialHeading, double initialVelocity) {
        this.currentLatitude = initialLat;
        this.currentLongitude = initialLon;
        this.currentHeadingDeg = (initialHeading + 360.0) % 360.0;
        this.forwardVelocity = Math.max(0.0, initialVelocity);
        this.lastUpdateTimestamp = System.currentTimeMillis();
        this.noiseFilter.reset();
        log.info("Dead Reckoning initialized at [{}, {}], heading: {}°, vel: {} m/s",
                initialLat, initialLon, currentHeadingDeg, forwardVelocity);
    }

    /**
     * Propagates navigation state forward by dt using current IMU measurement.
     */
    public synchronized double[] propagate(IMUSampleDTO sample, double dt, AbnormalSignalHandler.SignalConditionResult condition) {
        if (dt <= 0.0) dt = 0.1;

        // Step 1: Filter raw IMU measurements
        double[] fAccel = noiseFilter.filterAccelerometer(
                sample.getAccelerometerX(), sample.getAccelerometerY(), sample.getAccelerometerZ());
        double[] fGyro = noiseFilter.filterGyroscope(
                sample.getGyroscopeX(), sample.getGyroscopeY(), sample.getGyroscopeZ());

        // Step 2: Transform to vehicle chassis frame
        double[] vehicleAccel = alignmentService.transformToVehicleFrame(
                fAccel[0], fAccel[1], fAccel[2],
                sample.getAlpha(), sample.getBeta(), sample.getGamma());

        double fwdAccel = vehicleAccel[0];
        double lateralAccel = vehicleAccel[1];
        double verticalAccel = vehicleAccel[2];

        // Step 3: Heading update
        // If device orientation alpha (compass/yaw) is present, fuse it smoothly; otherwise integrate yaw gyro
        if (sample.getAlpha() != null) {
            double rawHeading = (360.0 - sample.getAlpha()) % 360.0; // Invert browser compass to standard navigation bearing
            // Complementary filter for heading
            double headingDiff = (rawHeading - currentHeadingDeg + 540.0) % 360.0 - 180.0;
            currentHeadingDeg = (currentHeadingDeg + 0.15 * headingDiff + 360.0) % 360.0;
        } else {
            // Gyroscope Z rate in degrees/sec (assuming gyro in rad/s, convert to deg/s)
            double yawRateDeg = Math.toDegrees(fGyro[2]);
            currentHeadingDeg = (currentHeadingDeg + yawRateDeg * dt + 360.0) % 360.0;
        }

        // Step 4: Zero-Velocity Update (ZUPT) detection
        // Check if vehicle is stationary: low total dynamic acceleration & minimal rotation
        double dynamicAccelMag = Math.abs(Math.sqrt(fwdAccel * fwdAccel + lateralAccel * lateralAccel + verticalAccel * verticalAccel) - GRAVITY);
        double gyroMag = Math.sqrt(fGyro[0] * fGyro[0] + fGyro[1] * fGyro[1] + fGyro[2] * fGyro[2]);

        boolean isStationary = forwardVelocity < 0.5 && dynamicAccelMag < ZUPT_ACCEL_VARIANCE_THRESH && gyroMag < ZUPT_GYRO_THRESH;
        if (isStationary) {
            forwardVelocity *= 0.85; // Rapid decay to zero
            if (forwardVelocity < 0.05) forwardVelocity = 0.0;
        } else {
            // Step 5: Velocity integration with abnormal event signal conditioning
            if (!condition.isSuppressLongitudinalIntegration()) {
                double effectiveAccel = fwdAccel * condition.getAccelerationDampingFactor();
                forwardVelocity += effectiveAccel * dt;
                // Apply rolling friction decay
                forwardVelocity -= VELOCITY_DRAG_DECAY * forwardVelocity * dt;
            }
            if (forwardVelocity < 0.0) forwardVelocity = 0.0;
            if (forwardVelocity > MAX_REALISTIC_VELOCITY) forwardVelocity = MAX_REALISTIC_VELOCITY;
        }

        // Step 6: Displacements in local ENU frame
        double headingRad = Math.toRadians(currentHeadingDeg);
        double deltaNorth = forwardVelocity * Math.cos(headingRad) * dt;
        double deltaEast = forwardVelocity * Math.sin(headingRad) * dt;

        // Step 7: Geodetic WGS-84 projection
        double[] newCoords = GeodeticUtils.projectCoordinates(currentLatitude, currentLongitude, deltaEast, deltaNorth);
        currentLatitude = newCoords[0];
        currentLongitude = newCoords[1];
        lastUpdateTimestamp = sample.getTimestamp();

        return new double[] { currentLatitude, currentLongitude, forwardVelocity, currentHeadingDeg };
    }

    public synchronized void setPositionAndVelocity(double lat, double lon, double vel, double heading) {
        this.currentLatitude = lat;
        this.currentLongitude = lon;
        this.forwardVelocity = Math.max(0.0, vel);
        this.currentHeadingDeg = (heading + 360.0) % 360.0;
    }

    public double getCurrentLatitude() { return currentLatitude; }
    public double getCurrentLongitude() { return currentLongitude; }
    public double getForwardVelocity() { return forwardVelocity; }
    public double getCurrentHeadingDeg() { return currentHeadingDeg; }
    public long getLastUpdateTimestamp() { return lastUpdateTimestamp; }
}
