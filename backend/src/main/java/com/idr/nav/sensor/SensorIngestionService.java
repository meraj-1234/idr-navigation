package com.idr.nav.sensor;

import com.idr.nav.dto.GNSSSampleDTO;
import com.idr.nav.dto.IMUSampleDTO;
import com.idr.nav.dto.MLPredictionDTO;
import com.idr.nav.dto.NavigationStateDTO;
import com.idr.nav.dto.PipelineStatusDTO;
import com.idr.nav.dto.RoutePoint;
import com.idr.nav.exception.InvalidSensorDataException;
import com.idr.nav.fusion.KalmanFilterService;
import com.idr.nav.fusion.KalmanState;
import com.idr.nav.mapping.MapMatchingService;
import com.idr.nav.ml.AbnormalSignalHandler;
import com.idr.nav.ml.EventDetectionService;
import com.idr.nav.ml.SensorWindow;
import com.idr.nav.navigation.DeadReckoningEngine;
import com.idr.nav.navigation.NavigationStateManager;
import org.springframework.stereotype.Service;

/**
 * Sensor Ingestion and Pipeline Orchestration Service.
 * 
 * Ingests IMU and GNSS samples from either:
 * 1. Physical smartphone web clients (DeviceMotion / Geolocation API)
 * 2. Controlled simulation mode
 * 
 * Both sources flow through the EXACT same pipeline.
 */
@Service
public class SensorIngestionService {

    private final TimestampSynchronizer timestampSynchronizer;
    private final SensorWindowBuffer windowBuffer;
    private final EventDetectionService mlEventDetector;
    private final AbnormalSignalHandler abnormalSignalHandler;
    private final DeadReckoningEngine deadReckoningEngine;
    private final KalmanFilterService kalmanFilterService;
    private final MapMatchingService mapMatchingService;
    private final NavigationStateManager stateManager;

    private GNSSSampleDTO latestGnssSample = null;
    private boolean lastGnssAvailable = true;
    private Double reconnectFromLat = null;
    private Double reconnectFromLon = null;
    private long reconnectStartMs = 0;
    private static final long GNSS_RECONNECT_BLEND_MS = 1600;

    public SensorIngestionService(TimestampSynchronizer timestampSynchronizer,
                                  SensorWindowBuffer windowBuffer,
                                  EventDetectionService mlEventDetector,
                                  AbnormalSignalHandler abnormalSignalHandler,
                                  DeadReckoningEngine deadReckoningEngine,
                                  KalmanFilterService kalmanFilterService,
                                  MapMatchingService mapMatchingService,
                                  NavigationStateManager stateManager) {
        this.timestampSynchronizer = timestampSynchronizer;
        this.windowBuffer = windowBuffer;
        this.mlEventDetector = mlEventDetector;
        this.abnormalSignalHandler = abnormalSignalHandler;
        this.deadReckoningEngine = deadReckoningEngine;
        this.kalmanFilterService = kalmanFilterService;
        this.mapMatchingService = mapMatchingService;
        this.stateManager = stateManager;
    }

    /**
     * Aligns IMU/GNSS pipeline clocks and filters to the selected route start so the
     * vehicle marker cannot jump from the default Delhi seed coordinates.
     */
    public synchronized void resetForRoute(double lat, double lon, double heading, double speed,
                                           java.util.List<RoutePoint> routePoints) {
        timestampSynchronizer.reset();
        deadReckoningEngine.initializeState(lat, lon, heading, speed);
        kalmanFilterService.initialize(lat, lon, speed, heading, System.currentTimeMillis());

        GNSSSampleDTO seed = new GNSSSampleDTO();
        seed.setTimestamp(System.currentTimeMillis());
        seed.setLatitude(lat);
        seed.setLongitude(lon);
        seed.setSpeed(speed);
        seed.setHeading(heading);
        seed.setAltitude(210.0);
        seed.setAccuracy(2.5);
        this.latestGnssSample = seed;

        this.lastGnssAvailable = true;
        this.reconnectFromLat = null;
        this.reconnectFromLon = null;

        if (routePoints != null && routePoints.size() >= 2) {
            double[] lats = new double[routePoints.size()];
            double[] lons = new double[routePoints.size()];
            for (int i = 0; i < routePoints.size(); i++) {
                lats[i] = routePoints.get(i).getLat();
                lons[i] = routePoints.get(i).getLon();
            }
            mapMatchingService.loadPolyline(lats, lons);
        }
    }

    /**
     * Ingests high-frequency IMU sample (Accelerometer, Gyroscope, Orientation).
     */
    public synchronized NavigationStateDTO ingestIMU(IMUSampleDTO imu) {
        if (imu == null) {
            throw new InvalidSensorDataException("IMU sample cannot be null");
        }
        if (imu.getTimestamp() <= 0) {
            imu.setTimestamp(System.currentTimeMillis());
        }

        PipelineStatusDTO pipeline = stateManager.getPipelineStatus();

        // 1. Stage: Sensor Ingestion
        pipeline.setSensorIngestion("ACTIVE");

        // 2. Stage: Synchronization
        double dt = timestampSynchronizer.computeImuDeltaTime(imu.getTimestamp());
        pipeline.setSynchronization("ACTIVE");

        // 3. Stage: Noise Filtering & Windowing
        pipeline.setNoiseFiltering("ACTIVE");
        windowBuffer.addSample(imu);

        // 4. Stage: Coordinate Transform (Phone-to-Vehicle)
        pipeline.setCoordinateTransform("ACTIVE");

        // 5. Stage: ML Event Detection (MOCK MODEL)
        SensorWindow window = new SensorWindow(windowBuffer.getWindow());
        MLPredictionDTO mlPrediction = mlEventDetector.predict(window);
        pipeline.setMlEventDetection("MOCK");

        // 6. Stage: Abnormal Signal Handling & Inertial Dead Reckoning
        AbnormalSignalHandler.SignalConditionResult condition = abnormalSignalHandler.processEvent(mlPrediction);
        double[] drState = deadReckoningEngine.propagate(imu, dt, condition);
        double drLat = drState[0];
        double drLon = drState[1];
        double drVel = drState[2];
        double drHeading = drState[3];
        pipeline.setDeadReckoning("ACTIVE");

        // 7. Stage: Kalman Fusion
        double navLat;
        double navLon;
        double navVel;
        double navHeading;
        String navSource;

        boolean gnssAvailable = stateManager.isGnssAvailable();

        if (gnssAvailable && !lastGnssAvailable) {
            reconnectFromLat = drLat;
            reconnectFromLon = drLon;
            reconnectStartMs = imu.getTimestamp();
        }
        if (!gnssAvailable) {
            reconnectFromLat = null;
            reconnectFromLon = null;
        }
        lastGnssAvailable = gnssAvailable;

        if (gnssAvailable && latestGnssSample != null) {
            // Predict with INS, then correct with GNSS
            kalmanFilterService.predict(drLat, drLon, drVel, drHeading, dt, condition.getKalmanNoiseInflationFactor());
            KalmanState kState = kalmanFilterService.getState();
            double targetLat = latestGnssSample.getLatitude();
            double targetLon = latestGnssSample.getLongitude();
            navVel = latestGnssSample.getSpeed() != null ? latestGnssSample.getSpeed() : kState.getTotalSpeed();
            navHeading = latestGnssSample.getHeading() != null ? latestGnssSample.getHeading() : kState.getHeadingDeg();
            navSource = "GNSS";
            pipeline.setKalmanFusion("ACTIVE");

            if (reconnectFromLat != null && reconnectFromLon != null) {
                double elapsed = imu.getTimestamp() - reconnectStartMs;
                double a = Math.max(0.0, Math.min(1.0, elapsed / (double) GNSS_RECONNECT_BLEND_MS));
                a = a * a * (3.0 - 2.0 * a); // smoothstep
                navLat = reconnectFromLat + a * (targetLat - reconnectFromLat);
                navLon = reconnectFromLon + a * (targetLon - reconnectFromLon);
                navSource = "FUSION";
                if (a >= 1.0) {
                    reconnectFromLat = null;
                    reconnectFromLon = null;
                    navLat = targetLat;
                    navLon = targetLon;
                    navSource = "GNSS";
                }
            } else {
                navLat = targetLat;
                navLon = targetLon;
            }
        } else {
            // GNSS Denied / Lost: dummy dead-reckoning / predicted position continues
            kalmanFilterService.predict(drLat, drLon, drVel, drHeading, dt, condition.getKalmanNoiseInflationFactor() * 2.0);
            navLat = drLat;
            navLon = drLon;
            navVel = drVel;
            navHeading = drHeading;
            navSource = "INS";
            pipeline.setKalmanFusion("GNSS_DENIED");
        }

        // 8. Stage: Map Matching
        MapMatchingService.MapMatchResult matchResult = mapMatchingService.matchPosition(navLat, navLon);
        Double mmLat = matchResult.isMatched() ? matchResult.getLatitude() : null;
        Double mmLon = matchResult.isMatched() ? matchResult.getLongitude() : null;
        pipeline.setMapMatching(matchResult.isMatched() ? "ACTIVE" : "STANDBY");

        // 9. Stage: Navigation Output
        pipeline.setNavigationOutput("ACTIVE");

        // Assemble comprehensive navigation state
        NavigationStateDTO state = new NavigationStateDTO();
        state.setTimestamp(imu.getTimestamp());
        state.setLatitude(navLat);
        state.setLongitude(navLon);
        state.setVelocity(navVel);
        state.setHeading(navHeading);
        state.setSource(navSource);

        state.setDeadReckoningLatitude(drLat);
        state.setDeadReckoningLongitude(drLon);

        if (gnssAvailable && latestGnssSample != null) {
            state.setGnssLatitude(latestGnssSample.getLatitude());
            state.setGnssLongitude(latestGnssSample.getLongitude());
            state.setGnssAccuracy(latestGnssSample.getAccuracy());
        } else {
            state.setGnssLatitude(null);
            state.setGnssLongitude(null);
            state.setGnssAccuracy(null);
        }

        state.setMapMatchedLatitude(mmLat);
        state.setMapMatchedLongitude(mmLon);
        state.setMlPrediction(mlPrediction);
        state.setLatestImuSample(imu);

        // Dispatch state to manager and WebSocket subscribers
        stateManager.publishNavigationState(state);

        return state;
    }

    /**
     * Ingests GNSS sample (Latitude, Longitude, Altitude, Speed, Heading, Accuracy).
     */
    public synchronized void ingestGNSS(GNSSSampleDTO gnss) {
        if (gnss == null) {
            throw new InvalidSensorDataException("GNSS sample cannot be null");
        }
        if (gnss.getLatitude() < -90.0 || gnss.getLatitude() > 90.0 ||
            gnss.getLongitude() < -180.0 || gnss.getLongitude() > 180.0) {
            throw new InvalidSensorDataException("Invalid latitude/longitude coordinates");
        }

        if (gnss.getTimestamp() <= 0) {
            gnss.setTimestamp(System.currentTimeMillis());
        }

        this.latestGnssSample = gnss;
        timestampSynchronizer.recordGnssTimestamp(gnss.getTimestamp());

        // If system is currently in GNSS available mode, incorporate into Kalman filter
        if (stateManager.isGnssAvailable()) {
            kalmanFilterService.updateWithGNSS(gnss);
            // Also align dead reckoning baseline if drift is significant
            if (gnss.getSpeed() != null && gnss.getHeading() != null) {
                deadReckoningEngine.setPositionAndVelocity(
                        gnss.getLatitude(), gnss.getLongitude(), gnss.getSpeed(), gnss.getHeading());
            }
        }
    }
}
