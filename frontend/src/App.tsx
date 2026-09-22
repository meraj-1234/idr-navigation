import React, { useState, useEffect, useCallback, useRef } from 'react';
import { api } from './services/api';
import { TelemetryWebSocket } from './services/websocket';
import { NavigationState, SystemEvent, SystemStatus } from './types/navigation';
import { Header } from './components/Header';
import { RoutePanel } from './components/RoutePanel';
import { SessionControls } from './components/SessionControls';
import { GnssControls } from './components/GnssControls';
import { LiveMap } from './components/LiveMap';
import { GnssStatusCard } from './components/GnssStatusCard';
import { NavigationStateCard } from './components/NavigationStateCard';
import { SensorMonitoringCard } from './components/SensorMonitoringCard';
import { MlEventPanel } from './components/MlEventPanel';
import { PipelineVisualizer } from './components/PipelineVisualizer';
import { EventLogConsole } from './components/EventLogConsole';
import { MobileSensorModal } from './components/MobileSensorModal';
import { RouteGeometry, RouteRequest } from './types/navigation';

export const App: React.FC = () => {
  // Navigation telemetry state
  const [currentState, setCurrentState] = useState<NavigationState>({
    sessionId: 'SESSION-DEFAULT',
    timestamp: Date.now(),
    latitude: 28.6139,
    longitude: 77.209,
    velocity: 0.0,
    heading: 0.0,
    source: 'FUSION',
    gnssAvailable: true,
    navigationMode: 'FUSION',
  });

  const [activeRoute, setActiveRoute] = useState<RouteGeometry | null>(null);

  const [trajectory, setTrajectory] = useState<NavigationState[]>([]);
  const [events, setEvents] = useState<SystemEvent[]>([]);
  const [systemStatus, setSystemStatus] = useState<SystemStatus | null>(null);
  const [isConnected, setIsConnected] = useState<boolean>(false);
  const [isMobileModalOpen, setIsMobileModalOpen] = useState<boolean>(false);

  // GNSS loss/restore map pin events
  const [gnssLossPins, setGnssLossPins] = useState<{ lat: number; lon: number; timestamp: number }[]>([]);
  const [gnssRestorePins, setGnssRestorePins] = useState<{ lat: number; lon: number; timestamp: number }[]>([]);

  const wsRef = useRef<TelemetryWebSocket | null>(null);
  const isConnectedRef = useRef<boolean>(false);

  // Handle incoming live telemetry frame from WebSocket
  const handleTelemetry = useCallback((state: NavigationState) => {
    setCurrentState(state);
    setTrajectory((prev) => {
      const next = [...prev, state];
      return next.length > 1500 ? next.slice(next.length - 1500) : next;
    });
  }, []);

  // Handle incoming live system event from WebSocket
  const handleEvent = useCallback((event: SystemEvent) => {
    setEvents((prev) => [event, ...prev].slice(0, 150));

    if (event.eventType === 'GNSS_LOST') {
      setCurrentState((curr) => {
        if (curr.latitude && curr.longitude) {
          setGnssLossPins((pins) => [
            ...pins,
            { lat: curr.latitude, lon: curr.longitude, timestamp: event.timestamp },
          ]);
        }
        return curr;
      });
    } else if (event.eventType === 'GNSS_RESTORED') {
      setCurrentState((curr) => {
        if (curr.latitude && curr.longitude) {
          setGnssRestorePins((pins) => [
            ...pins,
            { lat: curr.latitude, lon: curr.longitude, timestamp: event.timestamp },
          ]);
        }
        return curr;
      });
    }
  }, []);

  // Fetch baseline status via REST
  const refreshStatus = useCallback(async () => {
    try {
      const status = await api.getSystemStatus();
      setSystemStatus(status);
    } catch {
      // Backend may still be initializing
    }
  }, []);

  // Initialize WebSocket connection and polling fallback
  useEffect(() => {
    const ws = new TelemetryWebSocket({
      onTelemetry: handleTelemetry,
      onEvent: handleEvent,
      onStatusChange: (connected) => {
        isConnectedRef.current = connected;
        setIsConnected(connected);
      },
    });

    ws.connect();
    wsRef.current = ws;
    refreshStatus();

    // Check for active route on initial load
    api.getActiveRoute()
      .then((route) => {
        if (route) setActiveRoute(route);
      })
      .catch(() => {});

    // Secondary fallback polling if WebSocket is temporarily disconnected
    const pollInterval = setInterval(() => {
      if (!isConnectedRef.current) {
        api.getCurrentState()
          .then(handleTelemetry)
          .catch(() => {});
        api.getEvents()
          .then((evs) => setEvents(evs))
          .catch(() => {});
      }
      refreshStatus();
    }, 2000);

    return () => {
      clearInterval(pollInterval);
      ws.disconnect();
    };
  }, [handleTelemetry, handleEvent, refreshStatus]);

  // Session Control Actions
  const handleStartSession = async () => {
    try {
      const mode = systemStatus?.sessionMode || 'SIMULATION';
      await api.startSession('IDR Nav Demo Run', mode);
      if (mode === 'SIMULATION') {
        await api.startSimulation();
      }
      await refreshStatus();
    } catch (e) {
      console.error('Failed to start session', e);
    }
  };

  const handlePauseSession = async () => {
    try {
      await api.pauseSession();
      await refreshStatus();
    } catch (e) {
      console.error('Failed to pause session', e);
    }
  };

  const handleStopSession = async () => {
    try {
      await api.stopSession();
      await api.stopSimulation();
      await refreshStatus();
    } catch (e) {
      console.error('Failed to stop session', e);
    }
  };

  const handleResetSession = async () => {
    try {
      await api.resetSession();
      setTrajectory([]);
      setGnssLossPins([]);
      setGnssRestorePins([]);
      setEvents([]);
      await refreshStatus();
    } catch (e) {
      console.error('Failed to reset session', e);
    }
  };

  const handleToggleMode = async (mode: 'SIMULATION' | 'REAL_SENSOR') => {
    if (systemStatus?.isSimulating) {
      await api.stopSimulation();
    }
    await api.startSession('IDR Nav Run', mode);
    await refreshStatus();
  };

  // GNSS Control Actions
  const handleGnssLoss = async () => {
    try {
      await api.triggerGnssLoss();
      await refreshStatus();
    } catch (e) {
      console.error('Failed to trigger GNSS loss', e);
    }
  };

  const handleGnssRestore = async () => {
    try {
      await api.triggerGnssRestore();
      await refreshStatus();
    } catch (e) {
      console.error('Failed to trigger GNSS restore', e);
    }
  };

  // Route Navigation Demo Actions
  const handleCreateRoute = async (request: RouteRequest) => {
    try {
      const route = await api.createRoute(request);
      setActiveRoute(route);
      setCurrentState((prev) => ({
        ...prev,
        latitude: route.fromLat,
        longitude: route.fromLon,
        gnssLatitude: route.fromLat,
        gnssLongitude: route.fromLon,
        deadReckoningLatitude: route.fromLat,
        deadReckoningLongitude: route.fromLon,
        routeNavigationStatus: 'ROUTE_SELECTED',
        activeRouteId: route.routeId,
        routeProgress: 0,
        distanceRemainingMeters: route.totalDistanceMeters,
        gnssAvailable: true,
        source: 'GNSS',
      }));
      await refreshStatus();
    } catch (e) {
      console.error('Failed to create route', e);
      throw e;
    }
  };

  const handleStartRouteNavigation = async () => {
    try {
      await api.startRouteNavigation();
      await refreshStatus();
    } catch (e) {
      console.error('Failed to start route navigation', e);
      throw e;
    }
  };

  const handleStopRouteNavigation = async () => {
    try {
      await api.stopRouteNavigation();
      await refreshStatus();
    } catch (e) {
      console.error('Failed to stop route navigation', e);
      throw e;
    }
  };

  const handleResetRouteDemo = async () => {
    try {
      await api.resetDemo();
      setActiveRoute(null);
      setTrajectory([]);
      setGnssLossPins([]);
      setGnssRestorePins([]);
      setEvents([]);
      await refreshStatus();
    } catch (e) {
      console.error('Failed to reset route demo', e);
      throw e;
    }
  };

  const handleGnssToggle = async () => {
    if (currentState.gnssAvailable) {
      await handleGnssLoss();
    } else {
      await handleGnssRestore();
    }
  };

  return (
    <div className="min-h-screen bg-idr-bg text-slate-100 flex flex-col font-sans">
      {/* Top Header */}
      <Header
        isConnected={isConnected}
        navigationMode={currentState.navigationMode}
        gnssAvailable={currentState.gnssAvailable}
        sessionStatus={systemStatus?.sessionStatus || 'STOPPED'}
      />

      {/* Main Content Area */}
      <main className="flex-1 p-4 md:p-6 space-y-4 max-w-[1800px] w-full mx-auto">
        {/* Top Controls Grid: Route Demo Panel (Left) & Controls (Right) */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-4">
          <div className="lg:col-span-7">
            <RoutePanel
              activeRoute={activeRoute}
              navigationStatus={currentState.routeNavigationStatus || (activeRoute ? 'ROUTE_SELECTED' : 'IDLE')}
              navigationMode={currentState.navigationMode}
              gnssAvailable={currentState.gnssAvailable}
              routeProgress={currentState.routeProgress || 0}
              distanceRemainingMeters={currentState.distanceRemainingMeters ?? (activeRoute?.totalDistanceMeters || 0)}
              onCreateRoute={handleCreateRoute}
              onStartNavigation={handleStartRouteNavigation}
              onStopNavigation={handleStopRouteNavigation}
              onResetDemo={handleResetRouteDemo}
              onGnssToggle={handleGnssToggle}
            />
          </div>

          <div className="lg:col-span-5 space-y-4">
            <SessionControls
              sessionStatus={systemStatus?.sessionStatus || 'STOPPED'}
              sessionMode={systemStatus?.sessionMode || 'SIMULATION'}
              isSimulating={systemStatus?.isSimulating || false}
              onStart={handleStartSession}
              onPause={handlePauseSession}
              onStop={handleStopSession}
              onReset={handleResetSession}
              onToggleMode={handleToggleMode}
              onOpenMobileSensorModal={() => setIsMobileModalOpen(true)}
            />

            <GnssControls
              gnssAvailable={currentState.gnssAvailable}
              onGnssOn={handleGnssRestore}
              onGnssLoss={handleGnssLoss}
              onGnssRestore={handleGnssRestore}
            />
          </div>
        </div>

        {/* Central Layout: Live Map (Main Left) + Telemetry Panels (Right Column) */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-4">
          {/* Main Map */}
          <div className="lg:col-span-8 flex flex-col">
            <LiveMap
              currentState={currentState}
              trajectory={trajectory}
              gnssLossEvents={gnssLossPins}
              gnssRestoreEvents={gnssRestorePins}
              routePoints={activeRoute?.points}
              routeFromName={activeRoute?.fromName}
              routeToName={activeRoute?.toName}
            />
          </div>

          {/* Right Column Status Cards */}
          <div className="lg:col-span-4 space-y-4">
            <GnssStatusCard state={currentState} />
            <NavigationStateCard state={currentState} />
            <MlEventPanel prediction={currentState.mlPrediction} />
          </div>
        </div>

        {/* Lower Row: Sensor Waveforms (Left) & Event Log (Right) */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-4">
          <div className="lg:col-span-7">
            <SensorMonitoringCard latestImu={currentState.latestImuSample} />
          </div>

          <div className="lg:col-span-5">
            <EventLogConsole events={events} onClear={() => setEvents([])} />
          </div>
        </div>

        {/* Bottom Section: 9-Stage System Pipeline Visualizer */}
        <div>
          <PipelineVisualizer
            status={currentState.pipelineStatus}
            gnssAvailable={currentState.gnssAvailable}
          />
        </div>
      </main>

      {/* Mobile Sensor Broadcast Modal */}
      <MobileSensorModal
        isOpen={isMobileModalOpen}
        onClose={() => setIsMobileModalOpen(false)}
        sessionId={currentState.sessionId}
      />
    </div>
  );
};

export default App;
