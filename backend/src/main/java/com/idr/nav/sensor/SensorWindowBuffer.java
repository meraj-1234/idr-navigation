package com.idr.nav.sensor;

import com.idr.nav.dto.IMUSampleDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Sliding window buffer for raw and filtered sensor samples.
 * Supplies time-domain windows to the ML Event Detection module.
 */
@Component
public class SensorWindowBuffer {

    private final int windowCapacity;
    private final LinkedList<IMUSampleDTO> buffer = new LinkedList<>();

    public SensorWindowBuffer() {
        this(20); // Default window size of 20 samples (~1-2 seconds at 10-20 Hz)
    }

    public SensorWindowBuffer(int windowCapacity) {
        this.windowCapacity = windowCapacity;
    }

    public synchronized void addSample(IMUSampleDTO sample) {
        if (buffer.size() >= windowCapacity) {
            buffer.removeFirst();
        }
        buffer.addLast(sample);
    }

    public synchronized List<IMUSampleDTO> getWindow() {
        return new ArrayList<>(buffer);
    }

    public synchronized int size() {
        return buffer.size();
    }

    public synchronized boolean isFull() {
        return buffer.size() >= windowCapacity;
    }

    public synchronized void clear() {
        buffer.clear();
    }
}
