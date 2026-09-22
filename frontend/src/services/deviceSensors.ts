import { api } from './api';
import { GNSSSample, IMUSample } from '../types/navigation';

export interface DeviceSensorStatus {
  hasMotion: boolean;
  hasOrientation: boolean;
  hasGeolocation: boolean;
  permissionGranted: boolean;
  isStreaming: boolean;
  error: string | null;
}

export class DeviceSensorBroadcaster {
  private isStreaming = false;
  private geoWatchId: number | null = null;
  private onStatusChange: (status: DeviceSensorStatus) => void;

  private latestOrientation = { alpha: 0, beta: 0, gamma: 0 };
  private lastImuSendTime = 0;
  private imuThrottleMs = 100; // 10 Hz send rate to server

  private status: DeviceSensorStatus = {
    hasMotion: false,
    hasOrientation: false,
    hasGeolocation: false,
    permissionGranted: false,
    isStreaming: false,
    error: null,
  };

  constructor(onStatusChange: (status: DeviceSensorStatus) => void) {
    this.onStatusChange = onStatusChange;
    this.status.hasGeolocation = 'geolocation' in navigator;
    this.status.hasMotion = 'DeviceMotionEvent' in window;
    this.status.hasOrientation = 'DeviceOrientationEvent' in window;
    this.notify();
  }

  private notify() {
    this.onStatusChange({ ...this.status, isStreaming: this.isStreaming });
  }

  public async requestPermissions(): Promise<boolean> {
    try {
      // Check iOS 13+ permission request
      const motionAny = window.DeviceMotionEvent as unknown as { requestPermission?: () => Promise<string> };
      if (typeof motionAny?.requestPermission === 'function') {
        const permissionState = await motionAny.requestPermission();
        if (permissionState === 'granted') {
          this.status.permissionGranted = true;
          this.status.error = null;
          this.notify();
          return true;
        } else {
          this.status.permissionGranted = false;
          this.status.error = 'Motion sensor permission denied by user.';
          this.notify();
          return false;
        }
      }
      // Non-iOS standard browsers
      this.status.permissionGranted = true;
      this.status.error = null;
      this.notify();
      return true;
    } catch (err: unknown) {
      this.status.error = err instanceof Error ? err.message : 'Error requesting device permissions';
      this.notify();
      return false;
    }
  }

  public startStreaming(sessionId: string): void {
    if (this.isStreaming) return;

    this.isStreaming = true;
    this.notify();

    // 1. Orientation Listener
    window.addEventListener('deviceorientation', this.handleOrientation);

    // 2. Motion / IMU Listener
    window.addEventListener('devicemotion', (e) => this.handleMotion(e, sessionId));

    // 3. Geolocation Watcher
    if ('geolocation' in navigator) {
      this.geoWatchId = navigator.geolocation.watchPosition(
        (pos) => {
          const sample: GNSSSample = {
            timestamp: pos.timestamp,
            latitude: pos.coords.latitude,
            longitude: pos.coords.longitude,
            altitude: pos.coords.altitude,
            speed: pos.coords.speed,
            heading: pos.coords.heading,
            accuracy: pos.coords.accuracy,
            sessionId,
          };
          api.sendGNSSSample(sample).catch(() => {});
        },
        (err) => {
          console.warn('Geolocation error:', err.message);
        },
        { enableHighAccuracy: true, maximumAge: 1000, timeout: 5000 }
      );
    }
  }

  private handleOrientation = (e: DeviceOrientationEvent) => {
    this.latestOrientation = {
      alpha: e.alpha ?? 0,
      beta: e.beta ?? 0,
      gamma: e.gamma ?? 0,
    };
  };

  private handleMotion = (e: DeviceMotionEvent, sessionId: string) => {
    const now = Date.now();
    if (now - this.lastImuSendTime < this.imuThrottleMs) {
      return; // Throttle to 10 Hz
    }
    this.lastImuSendTime = now;

    // Use accelerationIncludingGravity if standard acceleration is null
    const accel = e.accelerationIncludingGravity || e.acceleration || { x: 0, y: 0, z: 9.8 };
    const gyro = e.rotationRate || { alpha: 0, beta: 0, gamma: 0 };

    const imuSample: IMUSample = {
      timestamp: now,
      accelerometerX: accel.x ?? 0,
      accelerometerY: accel.y ?? 0,
      accelerometerZ: accel.z ?? 9.80665,
      gyroscopeX: (gyro.beta ?? 0) * (Math.PI / 180),  // deg/s to rad/s
      gyroscopeY: (gyro.gamma ?? 0) * (Math.PI / 180),
      gyroscopeZ: (gyro.alpha ?? 0) * (Math.PI / 180),
      alpha: this.latestOrientation.alpha,
      beta: this.latestOrientation.beta,
      gamma: this.latestOrientation.gamma,
      sessionId,
    };

    api.sendIMUSample(imuSample).catch(() => {});
  };

  public stopStreaming(): void {
    if (!this.isStreaming) return;

    this.isStreaming = false;
    window.removeEventListener('deviceorientation', this.handleOrientation);
    if (this.geoWatchId !== null) {
      navigator.geolocation.clearWatch(this.geoWatchId);
      this.geoWatchId = null;
    }
    this.notify();
  }
}
