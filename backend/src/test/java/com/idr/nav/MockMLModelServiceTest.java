package com.idr.nav;

import com.idr.nav.dto.IMUSampleDTO;
import com.idr.nav.dto.MLPredictionDTO;
import com.idr.nav.ml.MockMLModelService;
import com.idr.nav.ml.SensorWindow;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MockMLModelServiceTest {

    @Test
    void testMockModelPropertiesAdhereToStrictPolicy() {
        MockMLModelService service = new MockMLModelService();
        List<IMUSampleDTO> samples = new ArrayList<>();
        long now = System.currentTimeMillis();

        // Baseline steady samples
        for (int i = 0; i < 10; i++) {
            samples.add(new IMUSampleDTO(now + i * 100, 0.1, 0.0, 9.8, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, "TEST"));
        }

        SensorWindow window = new SensorWindow(samples);
        MLPredictionDTO prediction = service.predict(window);

        // Strict verification:
        assertEquals("MOCK", prediction.getModelStatus(), "Model status must strictly be MOCK");
        assertNull(prediction.getConfidence(), "Confidence must strictly be null - NO fabricated confidence percentages allowed");
        assertEquals("NORMAL", prediction.getEvent());
    }

    @Test
    void testDetectsPotholeShockDeterministically() {
        MockMLModelService service = new MockMLModelService();
        List<IMUSampleDTO> samples = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (int i = 0; i < 10; i++) {
            // Inject sharp vertical spike in middle of window
            double z = (i == 5) ? 17.5 : 9.8;
            samples.add(new IMUSampleDTO(now + i * 100, 0.1, 0.0, z, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, "TEST"));
        }

        SensorWindow window = new SensorWindow(samples);
        MLPredictionDTO prediction = service.predict(window);

        assertEquals("MOCK", prediction.getModelStatus());
        assertNull(prediction.getConfidence());
        assertEquals("POTHOLE", prediction.getEvent(), "Large vertical shock should trigger mock POTHOLE event");
    }
}
