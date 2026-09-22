package com.idr.nav.ml;

import com.idr.nav.dto.IMUSampleDTO;
import java.util.Collections;
import java.util.List;

/**
 * Window container encapsulating a slice of synchronized IMU samples
 * formatted for feature extraction or deep-learning model inference.
 */
public class SensorWindow {

    private final List<IMUSampleDTO> samples;
    private final long startTime;
    private final long endTime;

    public SensorWindow(List<IMUSampleDTO> samples) {
        this.samples = samples != null ? Collections.unmodifiableList(samples) : Collections.emptyList();
        if (!this.samples.isEmpty()) {
            this.startTime = this.samples.get(0).getTimestamp();
            this.endTime = this.samples.get(this.samples.size() - 1).getTimestamp();
        } else {
            this.startTime = 0;
            this.endTime = 0;
        }
    }

    public List<IMUSampleDTO> getSamples() {
        return samples;
    }

    public int getSampleSize() {
        return samples.size();
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public double getDurationSeconds() {
        if (samples.size() < 2) return 0.0;
        return (endTime - startTime) / 1000.0;
    }
}
