package com.idr.nav.navigation;

import com.idr.nav.dto.*;
import com.idr.nav.entity.EstimatedPositionEntity;
import com.idr.nav.entity.NavigationEventEntity;
import com.idr.nav.entity.NavigationSession;
import com.idr.nav.repository.EstimatedPositionRepository;
import com.idr.nav.repository.NavigationEventRepository;
import com.idr.nav.repository.NavigationSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Central State Manager for Navigation sessions, mode transitions,
 * event broadcasting, and telemetry dispatching.
 */
@Service
public class NavigationStateManager {

    private static final Logger log = LoggerFactory.getLogger(NavigationStateManager.class);

    private final NavigationSessionRepository sessionRepository;
    private final EstimatedPositionRepository positionRepository;
    private final NavigationEventRepository eventRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // Active session state
    private String activeSessionId = "SESSION-DEFAULT";
    private String sessionStatus = "STOPPED"; // "ACTIVE", "PAUSED", "STOPPED"
    private String sessionMode = "SIMULATION"; // "SIMULATION", "REAL_SENSOR"

    // Navigation mode: "GNSS", "DEAD_RECKONING", "FUSION"
    private String navigationMode = "FUSION";
    private boolean gnssAvailable = true;

    private Long gnssLossTimestamp = null;
    private Long gnssRestoreTimestamp = null;

    private NavigationStateDTO currentState = new NavigationStateDTO();
    private final PipelineStatusDTO pipelineStatus = new PipelineStatusDTO();
    private final Deque<SystemEventDTO> recentEvents = new ConcurrentLinkedDeque<>();
    private final List<NavigationStateDTO> trajectoryHistory = Collections.synchronizedList(new ArrayList<>());

    private long lastPersistTimestamp = 0;
    private static final long PERSIST_INTERVAL_MS = 1000; // Persist every 1 sec to keep DB lean

    // Route navigation state
    private String activeRouteId = null;
    private String routeNavigationStatus = "IDLE"; // "IDLE","ROUTE_SELECTED","NAVIGATION_ACTIVE","COMPLETED","STOPPED"
    private double routeProgress = 0.0;
    private double distanceRemainingMeters = 0.0;


    public NavigationStateManager(NavigationSessionRepository sessionRepository,
                                  EstimatedPositionRepository positionRepository,
                                  NavigationEventRepository eventRepository,
                                  SimpMessagingTemplate messagingTemplate) {
        this.sessionRepository = sessionRepository;
        this.positionRepository = positionRepository;
        this.eventRepository = eventRepository;
        this.messagingTemplate = messagingTemplate;

        // Initialize default state
        currentState.setSessionId(activeSessionId);
        currentState.setNavigationMode("FUSION");
        currentState.setGnssAvailable(true);
        currentState.setLatitude(28.6139);
        currentState.setLongitude(77.2090);
        currentState.setVelocity(0.0);
        currentState.setHeading(0.0);
        currentState.setSource("FUSION");
        currentState.setPipelineStatus(pipelineStatus);
        currentState.setMlPrediction(new MLPredictionDTO("NORMAL", "NORMAL", System.currentTimeMillis()));

        logEvent("SYSTEM_INIT", "IDR NAV system initialized. Ready for session start.", "INFO");
    }

    public synchronized void startSession(String sessionName, String mode, Double initialLat, Double initialLon) {
        this.activeSessionId = "SES-" + System.currentTimeMillis();
        this.sessionStatus = "ACTIVE";
        this.sessionMode = (mode != null && !mode.isEmpty()) ? mode : "SIMULATION";
        this.gnssAvailable = true;
        this.navigationMode = "FUSION";
        this.gnssLossTimestamp = null;
        this.gnssRestoreTimestamp = null;
        this.trajectoryHistory.clear();

        double startLat = initialLat != null ? initialLat : 28.6129;
        double startLon = initialLon != null ? initialLon : 77.2100;

        NavigationSession session = new NavigationSession(
                activeSessionId,
                sessionName != null ? sessionName : "IDR Nav Session " + activeSessionId,
                sessionStatus,
                sessionMode,
                LocalDateTime.now(),
                startLat,
                startLon
        );
        sessionRepository.save(session);

        currentState.setSessionId(activeSessionId);
        currentState.setLatitude(startLat);
        currentState.setLongitude(startLon);
        currentState.setGnssLatitude(startLat);
        currentState.setGnssLongitude(startLon);
        currentState.setDeadReckoningLatitude(startLat);
        currentState.setDeadReckoningLongitude(startLon);
        currentState.setNavigationMode(navigationMode);
        currentState.setGnssAvailable(gnssAvailable);

        logEvent("SESSION_START", "Navigation session started: " + activeSessionId + " (" + sessionMode + ")", "SUCCESS");
    }

    public synchronized void pauseSession() {
        this.sessionStatus = "PAUSED";
        logEvent("SESSION_PAUSE", "Navigation session paused: " + activeSessionId, "WARN");
    }

    public synchronized void stopSession() {
        this.sessionStatus = "STOPPED";
        sessionRepository.findById(activeSessionId).ifPresent(session -> {
            session.setStatus("STOPPED");
            session.setEndTime(LocalDateTime.now());
            sessionRepository.save(session);
        });
        logEvent("SESSION_STOP", "Navigation session stopped: " + activeSessionId, "INFO");
    }

    public synchronized void resetSession() {
        this.sessionStatus = "STOPPED";
        this.gnssAvailable = true;
        this.navigationMode = "FUSION";
        this.gnssLossTimestamp = null;
        this.gnssRestoreTimestamp = null;
        this.trajectoryHistory.clear();
        this.recentEvents.clear();
        this.activeRouteId = null;
        this.routeNavigationStatus = "IDLE";
        this.routeProgress = 0.0;
        this.distanceRemainingMeters = 0.0;

        logEvent("SESSION_RESET", "Navigation session and telemetry buffers reset.", "INFO");
    }

    /**
     * Places the live marker on the selected route origin before navigation starts.
     */
    public synchronized void placeAtRouteStart(double lat, double lon) {
        currentState.setLatitude(lat);
        currentState.setLongitude(lon);
        currentState.setGnssLatitude(lat);
        currentState.setGnssLongitude(lon);
        currentState.setDeadReckoningLatitude(lat);
        currentState.setDeadReckoningLongitude(lon);
        currentState.setVelocity(0.0);
        currentState.setSource("GNSS");
        currentState.setRouteNavigationStatus(routeNavigationStatus);
        currentState.setActiveRouteId(activeRouteId);
        currentState.setRouteProgress(0.0);
        try {
            messagingTemplate.convertAndSend("/topic/telemetry", currentState);
        } catch (Exception ignored) {}
    }

    /**
     * Simulates or registers absolute GNSS Loss.
     */
    public synchronized void triggerGnssLoss() {
        this.gnssAvailable = false;
        this.navigationMode = "DEAD_RECKONING";
        this.gnssLossTimestamp = System.currentTimeMillis();
        currentState.setGnssAvailable(false);
        currentState.setNavigationMode("DEAD_RECKONING");
        currentState.setGnssLossTimestamp(this.gnssLossTimestamp);
        pipelineStatus.setKalmanFusion("GNSS_DENIED");
        pipelineStatus.setDeadReckoning("ACTIVE");

        logEvent("GNSS_LOST", "GNSS SIGNAL LOST! Switched to Dead Reckoning inertial navigation.", "ALERT");
    }

    /**
     * Restores GNSS availability and engages Kalman sensor fusion.
     */
    public synchronized void triggerGnssRestore() {
        this.gnssAvailable = true;
        this.navigationMode = "FUSION";
        this.gnssRestoreTimestamp = System.currentTimeMillis();
        currentState.setGnssAvailable(true);
        currentState.setNavigationMode("FUSION");
        currentState.setGnssRestoreTimestamp(this.gnssRestoreTimestamp);
        pipelineStatus.setKalmanFusion("ACTIVE");

        logEvent("GNSS_RESTORED", "GNSS Signal Restored. Kalman sensor fusion re-engaged.", "SUCCESS");
    }

    /**
     * Dispatches state update and broadcasts via WebSocket.
     */
    public synchronized void publishNavigationState(NavigationStateDTO state) {
        this.currentState = state;
        state.setSessionId(activeSessionId);
        state.setGnssAvailable(gnssAvailable);
        state.setNavigationMode(navigationMode);
        state.setGnssLossTimestamp(gnssLossTimestamp);
        state.setGnssRestoreTimestamp(gnssRestoreTimestamp);
        state.setPipelineStatus(pipelineStatus);
        state.setActiveRouteId(activeRouteId);
        state.setRouteProgress(routeProgress);
        state.setDistanceRemainingMeters(distanceRemainingMeters);
        state.setRouteNavigationStatus(routeNavigationStatus);

        // Append to in-memory trajectory
        trajectoryHistory.add(state);
        if (trajectoryHistory.size() > 2000) {
            trajectoryHistory.remove(0);
        }

        // Throttle database persistence
        long now = System.currentTimeMillis();
        if (now - lastPersistTimestamp >= PERSIST_INTERVAL_MS && "ACTIVE".equalsIgnoreCase(sessionStatus)) {
            lastPersistTimestamp = now;
            EstimatedPositionEntity entity = new EstimatedPositionEntity(
                    activeSessionId,
                    state.getTimestamp(),
                    state.getLatitude(),
                    state.getLongitude(),
                    state.getVelocity(),
                    state.getHeading(),
                    state.getSource(),
                    state.isGnssAvailable(),
                    state.getNavigationMode()
            );
            positionRepository.save(entity);
        }

        // Broadcast to WebSocket subscribers
        try {
            messagingTemplate.convertAndSend("/topic/telemetry", state);
        } catch (Exception e) {
            // Ignore if broker has no active clients
        }
    }

    public synchronized void logEvent(String eventType, String message, String severity) {
        SystemEventDTO event = new SystemEventDTO(eventType, message, severity);
        recentEvents.addFirst(event);
        if (recentEvents.size() > 200) {
            recentEvents.removeLast();
        }

        // Persist event
        NavigationEventEntity entity = new NavigationEventEntity(
                activeSessionId,
                event.getTimestamp(),
                eventType,
                message,
                severity
        );
        eventRepository.save(entity);

        // Broadcast event
        try {
            messagingTemplate.convertAndSend("/topic/events", event);
        } catch (Exception ignored) {}

        log.info("[{}] {}", eventType, message);
    }

    public NavigationStateDTO getCurrentState() { return currentState; }
    public PipelineStatusDTO getPipelineStatus() { return pipelineStatus; }
    public List<SystemEventDTO> getRecentEvents() { return new ArrayList<>(recentEvents); }
    public List<NavigationStateDTO> getTrajectoryHistory() { return new ArrayList<>(trajectoryHistory); }
    public String getSessionStatus() { return sessionStatus; }
    public String getActiveSessionId() { return activeSessionId; }
    public String getNavigationMode() { return navigationMode; }
    public boolean isGnssAvailable() { return gnssAvailable; }
    public String getSessionMode() { return sessionMode; }

    // Route state accessors
    public String getActiveRouteId() { return activeRouteId; }
    public void setActiveRouteId(String activeRouteId) { this.activeRouteId = activeRouteId; }

    public String getRouteNavigationStatus() { return routeNavigationStatus; }
    public void setRouteNavigationStatus(String routeNavigationStatus) {
        this.routeNavigationStatus = routeNavigationStatus;
    }

    public double getRouteProgress() { return routeProgress; }
    public void setRouteProgress(double routeProgress) { this.routeProgress = routeProgress; }

    public double getDistanceRemainingMeters() { return distanceRemainingMeters; }
    public void setDistanceRemainingMeters(double distanceRemainingMeters) {
        this.distanceRemainingMeters = distanceRemainingMeters;
    }
}
