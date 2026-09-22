package com.idr.nav.fusion;

import com.idr.nav.dto.GNSSSampleDTO;
import com.idr.nav.navigation.GeodeticUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Sensor Fusion Kalman Filter Service.
 * 
 * Fuses high-rate Dead Reckoning (INS) state propagation with absolute GNSS updates.
 * - In GNSS AVAILABLE mode: Corrects dead-reckoning drift using GPS measurement updates.
 * - In GNSS LOST mode: Predicts state using inertial dead reckoning while propagating error covariance.
 * - In GNSS RESTORED mode: Gracefully absorbs fresh GNSS observations, recalculating Kalman gains.
 */
@Service
public class KalmanFilterService {

    private static final Logger log = LoggerFactory.getLogger(KalmanFilterService.class);

    private KalmanState state;
    private boolean initialized = false;

    // Process Noise Parameters
    private static final double ACCEL_NOISE = 0.5;   // INS acceleration noise (m/s^2)
    private static final double BASE_GNSS_NOISE_METERS = 3.5; // Nominal GPS position noise in meters

    public synchronized void initialize(double initialLat, double initialLon, double initialSpeed, double initialHeading, long timestamp) {
        double headingRad = Math.toRadians(initialHeading);
        double vEast = initialSpeed * Math.sin(headingRad);
        double vNorth = initialSpeed * Math.cos(headingRad);

        this.state = new KalmanState(initialLat, initialLon, vEast, vNorth, timestamp);
        this.initialized = true;
        log.info("Kalman Filter initialized at [{}, {}], speed: {} m/s", initialLat, initialLon, initialSpeed);
    }

    /**
     * Prediction step: Propagates state using dead reckoning velocity and heading.
     */
    public synchronized void predict(double drLat, double drLon, double drVelocity, double drHeadingDeg, double dt, double noiseInflationFactor) {
        if (!initialized) {
            initialize(drLat, drLon, drVelocity, drHeadingDeg, System.currentTimeMillis());
            return;
        }

        if (dt <= 0.0) dt = 0.1;

        double headingRad = Math.toRadians(drHeadingDeg);
        double vEast = drVelocity * Math.sin(headingRad);
        double vNorth = drVelocity * Math.cos(headingRad);

        // State extrapolation
        double deltaEast = vEast * dt;
        double deltaNorth = vNorth * dt;

        double[] proj = GeodeticUtils.projectCoordinates(state.getLatitude(), state.getLongitude(), deltaEast, deltaNorth);
        state.setLatitude(proj[0]);
        state.setLongitude(proj[1]);
        state.setVelocityEast(vEast);
        state.setVelocityNorth(vNorth);
        state.setTimestamp(System.currentTimeMillis());

        // Error covariance propagation: P = F * P * F^T + Q
        // Approximate discrete process noise Q
        double qPos = 0.5 * ACCEL_NOISE * dt * dt * noiseInflationFactor;
        double qVel = ACCEL_NOISE * dt * noiseInflationFactor;

        // Convert meter uncertainty to approximate degree variance for lat/lon
        double degPerMeter = 1.0 / 111320.0;
        double qPosDeg2 = (qPos * degPerMeter) * (qPos * degPerMeter);

        double[][] P = state.getP();
        P[0][0] += qPosDeg2;
        P[1][1] += qPosDeg2;
        P[2][2] += qVel * qVel;
        P[3][3] += qVel * qVel;
    }

    /**
     * Measurement update step: Fuses GNSS observation when available.
     */
    public synchronized void updateWithGNSS(GNSSSampleDTO gnss) {
        if (!initialized) {
            double initialSpeed = gnss.getSpeed() != null ? gnss.getSpeed() : 0.0;
            double initialHeading = gnss.getHeading() != null ? gnss.getHeading() : 0.0;
            initialize(gnss.getLatitude(), gnss.getLongitude(), initialSpeed, initialHeading, gnss.getTimestamp());
            return;
        }

        double gnssAccuracy = (gnss.getAccuracy() != null && gnss.getAccuracy() > 0)
                ? gnss.getAccuracy() : BASE_GNSS_NOISE_METERS;

        // Measurement noise variance R in degree^2
        double degPerMeter = 1.0 / 111320.0;
        double rPosDeg2 = Math.pow(gnssAccuracy * degPerMeter, 2);

        // Innovation (Measurement residual)
        double yLat = gnss.getLatitude() - state.getLatitude();
        double yLon = gnss.getLongitude() - state.getLongitude();

        double[][] P = state.getP();

        // Kalman Gain for position components: K = P / (P + R)
        double sLat = P[0][0] + rPosDeg2;
        double kLat = P[0][0] / (sLat > 0 ? sLat : 1.0);

        double sLon = P[1][1] + rPosDeg2;
        double kLon = P[1][1] / (sLon > 0 ? sLon : 1.0);

        // State correction
        state.setLatitude(state.getLatitude() + kLat * yLat);
        state.setLongitude(state.getLongitude() + kLon * yLon);

        // Covariance update: P = (1 - K) * P
        P[0][0] = (1.0 - kLat) * P[0][0];
        P[1][1] = (1.0 - kLon) * P[1][1];

        // Velocity correction if GNSS provides Doppler speed and heading
        if (gnss.getSpeed() != null && gnss.getHeading() != null) {
            double hRad = Math.toRadians(gnss.getHeading());
            double gnssVe = gnss.getSpeed() * Math.sin(hRad);
            double gnssVn = gnss.getSpeed() * Math.cos(hRad);

            double rVel = 0.5; // ~0.5 m/s velocity noise
            double sVe = P[2][2] + rVel;
            double kVe = P[2][2] / sVe;
            double sVn = P[3][3] + rVel;
            double kVn = P[3][3] / sVn;

            state.setVelocityEast(state.getVelocityEast() + kVe * (gnssVe - state.getVelocityEast()));
            state.setVelocityNorth(state.getVelocityNorth() + kVn * (gnssVn - state.getVelocityNorth()));

            P[2][2] = (1.0 - kVe) * P[2][2];
            P[3][3] = (1.0 - kVn) * P[3][3];
        }

        state.setTimestamp(gnss.getTimestamp());
    }

    public synchronized KalmanState getState() {
        return state;
    }

    public boolean isInitialized() {
        return initialized;
    }
}
