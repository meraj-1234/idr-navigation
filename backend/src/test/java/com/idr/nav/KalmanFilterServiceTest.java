package com.idr.nav;

import com.idr.nav.dto.GNSSSampleDTO;
import com.idr.nav.fusion.KalmanFilterService;
import com.idr.nav.fusion.KalmanState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KalmanFilterServiceTest {

    private KalmanFilterService kalmanService;

    @BeforeEach
    void setUp() {
        kalmanService = new KalmanFilterService();
        kalmanService.initialize(28.6139, 77.2090, 10.0, 90.0, System.currentTimeMillis());
    }

    @Test
    void testPredictionAdvancesPosition() {
        KalmanState initialState = kalmanService.getState();
        double startLon = initialState.getLongitude();

        // Predict 1 second of eastward motion at 10 m/s
        kalmanService.predict(28.6139, 77.2090, 10.0, 90.0, 1.0, 1.0);

        KalmanState afterState = kalmanService.getState();
        assertTrue(afterState.getLongitude() > startLon, "Eastbound motion should increase longitude");
    }

    @Test
    void testGnssMeasurementUpdateCorrectsState() {
        // Apply dead reckoning prediction
        kalmanService.predict(28.6139, 77.2090, 10.0, 90.0, 0.5, 1.0);

        // GNSS observation provides actual measured position
        GNSSSampleDTO gnss = new GNSSSampleDTO(System.currentTimeMillis(), 28.6140, 77.2095, 200.0, 10.2, 91.0, 2.5, "TEST");
        kalmanService.updateWithGNSS(gnss);

        KalmanState updated = kalmanService.getState();
        assertNotNull(updated);
        // Corrected coordinate should be between prediction and measurement
        assertTrue(updated.getLongitude() > 77.2090);
    }
}
