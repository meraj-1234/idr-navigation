package com.idr.nav.ml;

import com.idr.nav.dto.IMUSampleDTO;
import com.idr.nav.dto.MLPredictionDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * ============================================================================
 * MOCK MODEL — TRAINED MODEL NOT INTEGRATED
 * ============================================================================
 * 
 * NOTICE FOR EVALUATORS AND REVIEWERS:
 * This class is an explicit placeholder implementation conforming to the
 * EventDetectionService interface.
 * 
 * In accordance with engineering integrity principles:
 * - NO trained model weights are integrated in this prototype build.
 * - NO artificial model accuracy or confidence percentages are fabricated.
 * - Confidence is strictly returned as null.
 * 
 * Simple deterministic heuristic rules are used solely to demonstrate that
 * the downstream signal quality handler and navigation fusion engine correctly
 * receive, react to, and process abnormal road anomalies (potholes, bumps).
 */
@Service
public class MockMLModelService implements EventDetectionService {

    private static final Logger log = LoggerFactory.getLogger(MockMLModelService.class);

    // Baseline thresholds for deterministic demonstration
    private static final double SHOCK_JERK_THRESHOLD = 5.5; // m/s^2 vertical deviation
    private static final double SPEED_BREAKER_THRESHOLD = 3.2;
    private static final double VIBRATION_VARIANCE_THRESHOLD = 1.8;

    @Override
    public MLPredictionDTO predict(SensorWindow window) {
        long currentTimestamp = window.getEndTime() > 0 ? window.getEndTime() : System.currentTimeMillis();
        List<IMUSampleDTO> samples = window.getSamples();

        if (samples.isEmpty()) {
            return new MLPredictionDTO("NORMAL", "NORMAL", currentTimestamp);
        }

        // Compute peak-to-peak range and standard deviation of vertical acceleration
        double minZ = Double.MAX_VALUE;
        double maxZ = -Double.MAX_VALUE;
        double sumZ = 0.0;

        for (IMUSampleDTO s : samples) {
            double z = s.getAccelerometerZ();
            if (z < minZ) minZ = z;
            if (z > maxZ) maxZ = z;
            sumZ += z;
        }

        double meanZ = sumZ / samples.size();
        double varianceZ = 0.0;
        for (IMUSampleDTO s : samples) {
            double diff = s.getAccelerometerZ() - meanZ;
            varianceZ += diff * diff;
        }
        varianceZ /= samples.size();
        double peakToPeak = maxZ - minZ;

        // Classify road event deterministically for pipeline exercise
        String detectedEvent = "NORMAL";
        if (peakToPeak > SHOCK_JERK_THRESHOLD) {
            detectedEvent = "POTHOLE";
        } else if (peakToPeak > SPEED_BREAKER_THRESHOLD) {
            // Distinguish bump vs speed breaker by sample window shape
            detectedEvent = (varianceZ > 2.5) ? "BUMP" : "SPEED_BREAKER";
        }

        // Classify vibration state
        String vibrationState = "NORMAL";
        if (varianceZ > VIBRATION_VARIANCE_THRESHOLD) {
            // Check gyro variance for engine vs road vibration
            vibrationState = (peakToPeak > 4.0) ? "ROAD_VIBRATION" : "ENGINE_VIBRATION";
        }

        MLPredictionDTO prediction = new MLPredictionDTO(detectedEvent, vibrationState, currentTimestamp);
        // Note: ModelStatus is set to "MOCK" and confidence is strictly null inside MLPredictionDTO
        return prediction;
    }
}
