package com.idr.nav;

import com.idr.nav.dto.IMUSampleDTO;
import com.idr.nav.ml.AbnormalSignalHandler;
import com.idr.nav.navigation.DeadReckoningEngine;
import com.idr.nav.sensor.NoiseFilter;
import com.idr.nav.sensor.PhoneToVehicleAlignmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeadReckoningEngineTest {

    private DeadReckoningEngine engine;

    @BeforeEach
    void setUp() {
        PhoneToVehicleAlignmentService alignmentService = new PhoneToVehicleAlignmentService();
        alignmentService.setMountType(PhoneToVehicleAlignmentService.MountType.FLAT_CONSOLE);
        NoiseFilter noiseFilter = new NoiseFilter();
        engine = new DeadReckoningEngine(alignmentService, noiseFilter);
        engine.initializeState(28.6139, 77.2090, 0.0, 0.0);
    }

    @Test
    void testStationaryZuptKeepsVelocityZero() {
        AbnormalSignalHandler.SignalConditionResult normalCondition =
                new AbnormalSignalHandler.SignalConditionResult(1.0, 1.0, false, "STANDARD");

        // Stationary IMU: only gravity on Z, zero on X/Y
        IMUSampleDTO sample = new IMUSampleDTO(System.currentTimeMillis(), 0.0, 0.0, 9.80665,
                0.0, 0.0, 0.0, null, null, null, "TEST");

        double[] state = engine.propagate(sample, 0.1, normalCondition);

        assertEquals(0.0, state[2], 0.001, "Stationary vehicle velocity should remain zero under ZUPT");
        assertEquals(28.6139, state[0], 0.0001, "Latitude should not drift while stationary");
    }

    @Test
    void testForwardAccelerationIncreasesVelocity() {
        AbnormalSignalHandler.SignalConditionResult normalCondition =
                new AbnormalSignalHandler.SignalConditionResult(1.0, 1.0, false, "STANDARD");

        // Forward acceleration of 2.0 m/s^2 on Y axis for FLAT_CONSOLE mount
        for (int i = 0; i < 5; i++) {
            IMUSampleDTO sample = new IMUSampleDTO(System.currentTimeMillis() + i * 100,
                    0.0, 2.0, 9.80665, 0.0, 0.0, 0.0, null, null, null, "TEST");
            engine.propagate(sample, 0.1, normalCondition);
        }

        assertTrue(engine.getForwardVelocity() > 0.0, "Velocity should increase under positive forward acceleration");
    }

    @Test
    void testMovingVehicleMaintainsVelocityDuringCruising() {
        AbnormalSignalHandler.SignalConditionResult normalCondition =
                new AbnormalSignalHandler.SignalConditionResult(1.0, 1.0, false, "STANDARD");

        // Initialize moving vehicle at 10 m/s
        engine.setPositionAndVelocity(28.6139, 77.2090, 10.0, 0.0);

        // Cruising: no forward accel, only gravity reaction
        IMUSampleDTO sample = new IMUSampleDTO(System.currentTimeMillis(),
                0.0, 0.0, 9.80665, 0.0, 0.0, 0.0, null, null, null, "TEST");

        double[] state = engine.propagate(sample, 0.1, normalCondition);

        assertTrue(state[2] > 9.0, "Moving vehicle should maintain velocity and not stop abruptly during cruising");
        assertTrue(state[0] > 28.6139, "Vehicle should have moved northward");
    }
}
