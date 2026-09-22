package com.idr.nav.ml;

import com.idr.nav.dto.MLPredictionDTO;

/**
 * Interface for Event and Vibration Detection.
 * 
 * ARCHITECTURE CONTRACT:
 * The navigation, dead reckoning, and fusion engines interact strictly with this interface.
 * The current implementation is MockMLModelService.
 * In a future phase, a TrainedMLModelService (e.g. ONNX Runtime, TorchScript, or REST-based model service)
 * can be plugged in without modifying any navigation or sensor fusion code.
 */
public interface EventDetectionService {

    /**
     * Evaluates a temporal window of synchronized sensor measurements.
     * 
     * @param window Temporal window of IMU samples.
     * @return MLPredictionDTO containing event class, vibration class, and model status.
     */
    MLPredictionDTO predict(SensorWindow window);
}
