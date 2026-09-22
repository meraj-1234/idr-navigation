export interface IMUSample {
  timestamp: number;
  accelerometerX: number;
  accelerometerY: number;
  accelerometerZ: number;
  gyroscopeX: number;
  gyroscopeY: number;
  gyroscopeZ: number;
  alpha?: number | null; // Compass heading 0-360
  beta?: number | null;  // Pitch tilt -180 to 180
  gamma?: number | null; // Roll tilt -90 to 90
  sessionId?: string;
}

export interface GNSSSample {
  timestamp: number;
  latitude: number;
  longitude: number;
  altitude?: number | null;
  speed?: number | null;
  heading?: number | null;
  accuracy?: number | null;
  sessionId?: string;
}

export interface MLPrediction {
  modelStatus: string; // Strictly "MOCK"
  event: 'NORMAL' | 'POTHOLE' | 'BUMP' | 'SPEED_BREAKER' | string;
  vibrationState: 'NORMAL' | 'ENGINE_VIBRATION' | 'ROAD_VIBRATION' | string;
  confidence: number | null; // Strictly null - no fake confidence!
  timestamp: number;
  description: string;
}

export interface PipelineStatus {
  sensorIngestion: string;
  synchronization: string;
  noiseFiltering: string;
  coordinateTransform: string;
  mlEventDetection: string;
  deadReckoning: string;
  kalmanFusion: string;
  mapMatching: string;
  navigationOutput: string;
}

export interface NavigationState {
  sessionId: string;
  timestamp: number;
  latitude: number;
  longitude: number;
  velocity: number | null; // m/s
  heading: number | null;  // degrees
  source: 'GNSS' | 'INS' | 'FUSION' | string;
  gnssAvailable: boolean;
  navigationMode: 'GNSS' | 'DEAD_RECKONING' | 'FUSION' | string;
  gnssLatitude?: number | null;
  gnssLongitude?: number | null;
  gnssAccuracy?: number | null;
  deadReckoningLatitude?: number | null;
  deadReckoningLongitude?: number | null;
  mapMatchedLatitude?: number | null;
  mapMatchedLongitude?: number | null;
  gnssLossTimestamp?: number | null;
  gnssRestoreTimestamp?: number | null;
  mlPrediction?: MLPrediction | null;
  pipelineStatus?: PipelineStatus | null;
  latestImuSample?: IMUSample | null;
  // Route navigation
  activeRouteId?: string | null;
  routeProgress?: number | null;            // 0.0 – 1.0
  distanceRemainingMeters?: number | null;
  routeNavigationStatus?: string | null;    // "IDLE" | "ROUTE_SELECTED" | "NAVIGATION_ACTIVE" | "COMPLETED"
}

export interface SystemEvent {
  timestamp: number;
  formattedTime: string;
  eventType: string;
  message: string;
  severity: 'INFO' | 'WARN' | 'ALERT' | 'SUCCESS';
}

export interface SystemStatus {
  system: string;
  version: string;
  status: string;
  activeSessionId: string;
  sessionStatus: 'ACTIVE' | 'PAUSED' | 'STOPPED' | string;
  sessionMode: 'SIMULATION' | 'REAL_SENSOR' | string;
  navigationMode: 'GNSS' | 'DEAD_RECKONING' | 'FUSION' | string;
  gnssAvailable: boolean;
  isSimulating: boolean;
  mlModelStatus: string;
}

// ---- Route Navigation Types ----

export interface RoutePoint {
  lat: number;
  lon: number;
}

export interface RouteGeometry {
  routeId: string;
  fromName: string;
  fromLat: number;
  fromLon: number;
  toName: string;
  toLat: number;
  toLon: number;
  totalDistanceMeters: number;
  estimatedDurationSeconds: number;
  points: RoutePoint[];
  routeSource: 'OSRM' | 'FALLBACK_INTERPOLATED' | string;
}

export interface RouteRequest {
  from: { lat: number; lon: number; name: string };
  to:   { lat: number; lon: number; name: string };
}
