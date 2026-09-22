package com.idr.nav.ml;

import com.idr.nav.dto.MLPredictionDTO;
import org.springframework.stereotype.Component;

/**
 * Abnormal Signal & Event Conditioning Handler.
 * 
 * Separates ML inference output from navigation decision-making.
 * When road surface shocks (potholes, bumps, speed breakers) or excessive vibrations
 * are detected, this handler conditions inertial signals so that spurious shock transients
 * do not corrupt forward velocity dead-reckoning integration or destabilize Kalman filters.
 */
@Component
public class AbnormalSignalHandler {

    public static class SignalConditionResult {
        private final double accelerationDampingFactor;
        private final double kalmanNoiseInflationFactor;
        private final boolean suppressLongitudinalIntegration;
        private final String actionTaken;

        public SignalConditionResult(double accelerationDampingFactor, double kalmanNoiseInflationFactor,
                                     boolean suppressLongitudinalIntegration, String actionTaken) {
            this.accelerationDampingFactor = accelerationDampingFactor;
            this.kalmanNoiseInflationFactor = kalmanNoiseInflationFactor;
            this.suppressLongitudinalIntegration = suppressLongitudinalIntegration;
            this.actionTaken = actionTaken;
        }

        public double getAccelerationDampingFactor() { return accelerationDampingFactor; }
        public double getKalmanNoiseInflationFactor() { return kalmanNoiseInflationFactor; }
        public boolean isSuppressLongitudinalIntegration() { return suppressLongitudinalIntegration; }
        public String getActionTaken() { return actionTaken; }
    }

    public SignalConditionResult processEvent(MLPredictionDTO prediction) {
        if (prediction == null || "NORMAL".equalsIgnoreCase(prediction.getEvent())) {
            // Normal conditions: No damping, standard Kalman noise
            return new SignalConditionResult(1.0, 1.0, false, "STANDARD_OPERATION");
        }

        String event = prediction.getEvent();
        switch (event.toUpperCase()) {
            case "POTHOLE":
                // Sudden negative vertical impulse causing vehicle pitch vibration.
                // Dampen horizontal integration heavily for 1 step to avoid phantom acceleration.
                return new SignalConditionResult(0.2, 4.0, true, "POTHOLE_SHOCK_DAMPED");

            case "BUMP":
                // Vertical bump: inflate process covariance and dampen acceleration spike
                return new SignalConditionResult(0.3, 3.5, true, "BUMP_SHOCK_DAMPED");

            case "SPEED_BREAKER":
                // Smooth double-hump: allow gradual speed change, slightly inflate noise
                return new SignalConditionResult(0.6, 2.0, false, "SPEED_BREAKER_CONDITIONED");

            default:
                return new SignalConditionResult(1.0, 1.0, false, "STANDARD_OPERATION");
        }
    }
}
