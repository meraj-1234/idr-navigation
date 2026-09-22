package com.idr.nav.sensor;

import org.springframework.stereotype.Component;

/**
 * Timestamp synchronization and time interval calculation.
 * Ensures consistent time delta (dt) across asynchronous IMU and GNSS feeds,
 * clamping extreme gaps caused by network latency or pauses.
 */
@Component
public class TimestampSynchronizer {

    private long lastImuTimestamp = 0;
    private long lastGnssTimestamp = 0;

    private static final double MIN_DT = 0.005; // 5 ms (200 Hz max)
    private static final double MAX_DT = 0.500; // 500 ms (clamp during stutter/pause)
    private static final double DEFAULT_DT = 0.100; // 100 ms (10 Hz nominal)

    public synchronized double computeImuDeltaTime(long currentTimestamp) {
        if (lastImuTimestamp <= 0) {
            lastImuTimestamp = currentTimestamp;
            return DEFAULT_DT;
        }

        long diffMs = currentTimestamp - lastImuTimestamp;
        lastImuTimestamp = currentTimestamp;

        if (diffMs <= 0) {
            return DEFAULT_DT;
        }

        double dt = diffMs / 1000.0;
        if (dt < MIN_DT) return MIN_DT;
        if (dt > MAX_DT) return MAX_DT;

        return dt;
    }

    public synchronized void recordGnssTimestamp(long timestamp) {
        this.lastGnssTimestamp = timestamp;
    }

    public synchronized long getLastGnssTimestamp() {
        return lastGnssTimestamp;
    }

    public synchronized void reset() {
        this.lastImuTimestamp = 0;
        this.lastGnssTimestamp = 0;
    }
}
