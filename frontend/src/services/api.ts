import { GNSSSample, IMUSample, NavigationState, PipelineStatus, SystemEvent, SystemStatus, RouteGeometry, RouteRequest } from '../types/navigation';

const API_BASE = '/api';

export const api = {
  async getSystemStatus(): Promise<SystemStatus> {
    const res = await fetch(`${API_BASE}/system/status`);
    if (!res.ok) throw new Error(`HTTP ${res.status}: Failed to fetch system status`);
    return res.json();
  },

  async startSession(sessionName = 'IDR Nav Session', mode = 'SIMULATION'): Promise<NavigationState> {
    const res = await fetch(`${API_BASE}/navigation/session/start`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ sessionName, mode }),
    });
    if (!res.ok) throw new Error(`HTTP ${res.status}: Failed to start session`);
    return res.json();
  },

  async pauseSession(): Promise<void> {
    const res = await fetch(`${API_BASE}/navigation/session/pause`, { method: 'POST' });
    if (!res.ok) throw new Error(`HTTP ${res.status}: Failed to pause session`);
  },

  async stopSession(): Promise<void> {
    const res = await fetch(`${API_BASE}/navigation/session/stop`, { method: 'POST' });
    if (!res.ok) throw new Error(`HTTP ${res.status}: Failed to stop session`);
  },

  async resetSession(): Promise<void> {
    const res = await fetch(`${API_BASE}/navigation/session/reset`, { method: 'POST' });
    if (!res.ok) throw new Error(`HTTP ${res.status}: Failed to reset session`);
  },

  async triggerGnssLoss(): Promise<NavigationState> {
    const res = await fetch(`${API_BASE}/navigation/gnss/loss`, { method: 'POST' });
    if (!res.ok) throw new Error(`HTTP ${res.status}: Failed to trigger GNSS loss`);
    return res.json();
  },

  async triggerGnssRestore(): Promise<NavigationState> {
    const res = await fetch(`${API_BASE}/navigation/gnss/restore`, { method: 'POST' });
    if (!res.ok) throw new Error(`HTTP ${res.status}: Failed to restore GNSS`);
    return res.json();
  },

  async getCurrentState(): Promise<NavigationState> {
    const res = await fetch(`${API_BASE}/navigation/state`);
    if (!res.ok) throw new Error(`HTTP ${res.status}: Failed to get navigation state`);
    return res.json();
  },

  async getTrajectory(): Promise<NavigationState[]> {
    const res = await fetch(`${API_BASE}/navigation/trajectory`);
    if (!res.ok) throw new Error(`HTTP ${res.status}: Failed to get trajectory`);
    return res.json();
  },

  async getEvents(): Promise<SystemEvent[]> {
    const res = await fetch(`${API_BASE}/navigation/events`);
    if (!res.ok) throw new Error(`HTTP ${res.status}: Failed to get events`);
    return res.json();
  },

  async getPipelineStatus(): Promise<PipelineStatus> {
    const res = await fetch(`${API_BASE}/navigation/pipeline`);
    if (!res.ok) throw new Error(`HTTP ${res.status}: Failed to get pipeline status`);
    return res.json();
  },

  async startSimulation(): Promise<void> {
    const res = await fetch(`${API_BASE}/simulation/start`, { method: 'POST' });
    if (!res.ok) throw new Error(`HTTP ${res.status}: Failed to start simulation`);
  },

  async stopSimulation(): Promise<void> {
    const res = await fetch(`${API_BASE}/simulation/stop`, { method: 'POST' });
    if (!res.ok) throw new Error(`HTTP ${res.status}: Failed to stop simulation`);
  },

  async sendIMUSample(sample: IMUSample): Promise<NavigationState> {
    const res = await fetch(`${API_BASE}/sensors/imu`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(sample),
    });
    if (!res.ok) throw new Error(`HTTP ${res.status}: Failed to ingest IMU sample`);
    return res.json();
  },

  async sendGNSSSample(sample: GNSSSample): Promise<void> {
    const res = await fetch(`${API_BASE}/sensors/gnss`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(sample),
    });
    if (!res.ok) throw new Error(`HTTP ${res.status}: Failed to ingest GNSS sample`);
  },

  // ---- Route Navigation API ----

  async createRoute(request: RouteRequest): Promise<RouteGeometry> {
    const res = await fetch(`${API_BASE}/navigation/routes`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request),
    });
    if (!res.ok) throw new Error(`HTTP ${res.status}: Failed to create route`);
    return res.json();
  },

  async getActiveRoute(): Promise<RouteGeometry | null> {
    const res = await fetch(`${API_BASE}/navigation/routes/active`);
    if (res.status === 204) return null;
    if (!res.ok) throw new Error(`HTTP ${res.status}: Failed to get active route`);
    return res.json();
  },

  async startRouteNavigation(): Promise<void> {
    const res = await fetch(`${API_BASE}/navigation/routes/navigation/start`, { method: 'POST' });
    if (!res.ok) throw new Error(`HTTP ${res.status}: Failed to start route navigation`);
  },

  async stopRouteNavigation(): Promise<void> {
    const res = await fetch(`${API_BASE}/navigation/routes/navigation/stop`, { method: 'POST' });
    if (!res.ok) throw new Error(`HTTP ${res.status}: Failed to stop route navigation`);
  },

  async resetDemo(): Promise<void> {
    const res = await fetch(`${API_BASE}/navigation/routes/navigation/reset`, { method: 'POST' });
    if (!res.ok) throw new Error(`HTTP ${res.status}: Failed to reset demo`);
  },
};
