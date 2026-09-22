package com.idr.nav.sensor;

import org.springframework.stereotype.Component;

/**
 * Noise filtering module implementing exponential low-pass filtering
 * on 3-axis accelerometer and gyroscope streams to attenuate high-frequency vibration.
 */
@Component
public class NoiseFilter {

    // Smoothing factor alpha in [0, 1]. Higher = more responsive, lower = smoother.
    private double accelAlpha = 0.65;
    private double gyroAlpha = 0.60;

    private double[] filteredAccel = new double[3];
    private double[] filteredGyro = new double[3];
    private boolean initialized = false;

    public synchronized double[] filterAccelerometer(double ax, double ay, double az) {
        if (!initialized) {
            filteredAccel[0] = ax;
            filteredAccel[1] = ay;
            filteredAccel[2] = az;
            return filteredAccel.clone();
        }

        filteredAccel[0] = accelAlpha * ax + (1.0 - accelAlpha) * filteredAccel[0];
        filteredAccel[1] = accelAlpha * ay + (1.0 - accelAlpha) * filteredAccel[1];
        filteredAccel[2] = accelAlpha * az + (1.0 - accelAlpha) * filteredAccel[2];

        return filteredAccel.clone();
    }

    public synchronized double[] filterGyroscope(double gx, double gy, double gz) {
        if (!initialized) {
            filteredGyro[0] = gx;
            filteredGyro[1] = gy;
            filteredGyro[2] = gz;
            initialized = true;
            return filteredGyro.clone();
        }

        filteredGyro[0] = gyroAlpha * gx + (1.0 - gyroAlpha) * filteredGyro[0];
        filteredGyro[1] = gyroAlpha * gy + (1.0 - gyroAlpha) * filteredGyro[1];
        filteredGyro[2] = gyroAlpha * gz + (1.0 - gyroAlpha) * filteredGyro[2];

        return filteredGyro.clone();
    }

    public synchronized void reset() {
        filteredAccel = new double[3];
        filteredGyro = new double[3];
        initialized = false;
    }
}
