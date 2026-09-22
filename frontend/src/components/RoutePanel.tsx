import React, { useState, useCallback } from 'react';
import { MapPin, Navigation, RotateCcw, Search, CheckCircle, Loader2, AlertTriangle, Route } from 'lucide-react';
import { RouteGeometry, RouteRequest } from '../types/navigation';

interface RoutePanelProps {
  activeRoute: RouteGeometry | null;
  navigationStatus: string; // "IDLE"|"ROUTE_SELECTED"|"NAVIGATION_ACTIVE"|"COMPLETED"|"STOPPED"
  navigationMode: string;   // "GNSS"|"DEAD_RECKONING"|"FUSION"
  gnssAvailable: boolean;
  routeProgress: number;
  distanceRemainingMeters: number;
  onCreateRoute: (request: RouteRequest) => Promise<void>;
  onStartNavigation: () => Promise<void>;
  onStopNavigation: () => Promise<void>;
  onResetDemo: () => Promise<void>;
  onGnssToggle: () => Promise<void>;
}

interface NominatimResult {
  lat: string;
  lon: string;
  display_name: string;
}

const NOMINATIM_BASE = 'https://nominatim.openstreetmap.org/search';

const DEFAULT_LANDMARKS: Record<string, { lat: string; lon: string; display_name: string }> = {
  'india gate': { lat: '28.6129', lon: '77.2295', display_name: 'India Gate, Rajpath, New Delhi' },
  'connaught place': { lat: '28.6315', lon: '77.2167', display_name: 'Connaught Place, New Delhi' },
  'red fort': { lat: '28.6562', lon: '77.2410', display_name: 'Red Fort, Netaji Subhash Marg, Old Delhi' },
  'qutub minar': { lat: '28.5244', lon: '77.1855', display_name: 'Qutub Minar, Mehrauli, New Delhi' },
  'rashtrapati bhavan': { lat: '28.6143', lon: '77.1994', display_name: 'Rashtrapati Bhavan, New Delhi' },
  'aiims': { lat: '28.5672', lon: '77.2100', display_name: 'AIIMS, Sri Aurobindo Marg, New Delhi' },
  'delhi': { lat: '28.6139', lon: '77.2090', display_name: 'New Delhi, Delhi, India' },
  'mumbai': { lat: '19.0760', lon: '72.8777', display_name: 'Mumbai, Maharashtra, India' },
  'bengaluru': { lat: '12.9716', lon: '77.5946', display_name: 'Bengaluru, Karnataka, India' },
  'bangalore': { lat: '12.9716', lon: '77.5946', display_name: 'Bengaluru, Karnataka, India' },
  'hyderabad': { lat: '17.3850', lon: '78.4867', display_name: 'Hyderabad, Telangana, India' },
  'chennai': { lat: '13.0827', lon: '80.2707', display_name: 'Chennai, Tamil Nadu, India' },
  'kolkata': { lat: '22.5726', lon: '88.3639', display_name: 'Kolkata, West Bengal, India' },
  'pune': { lat: '18.5204', lon: '73.8567', display_name: 'Pune, Maharashtra, India' },
};

async function geocode(query: string): Promise<NominatimResult[]> {
  const trimmed = query.trim();
  if (!trimmed) return [];

  // Direct coordinate match (e.g. "28.6129, 77.2295" or "28.6129 77.2295")
  const coordMatch = trimmed.match(/^(-?\d+(?:\.\d+)?)[,\s]+(-?\d+(?:\.\d+)?)$/);
  if (coordMatch) {
    return [{
      lat: coordMatch[1],
      lon: coordMatch[2],
      display_name: `Coordinates (${coordMatch[1]}, ${coordMatch[2]})`,
    }];
  }

  // Try Nominatim API
  try {
    const url = `${NOMINATIM_BASE}?q=${encodeURIComponent(trimmed)}&format=json&limit=5&countrycodes=in`;
    const res = await fetch(url, { headers: { 'Accept-Language': 'en' } });
    if (res.ok) {
      const data: NominatimResult[] = await res.json();
      if (data && data.length > 0) return data;
    }
  } catch {
    // Network or CORS issue with Nominatim, fall through to presets
  }

  // Fallback preset landmarks
  const lower = trimmed.toLowerCase();
  for (const [key, val] of Object.entries(DEFAULT_LANDMARKS)) {
    if (lower.includes(key) || key.includes(lower)) {
      return [val];
    }
  }

  // Generic fallback if unknown text
  return [{
    lat: '28.6139',
    lon: '77.2090',
    display_name: `${trimmed} (Delhi region default)`,
  }];
}

function formatDistance(m: number): string {
  if (m >= 1000) return `${(m / 1000).toFixed(2)} km`;
  return `${Math.round(m)} m`;
}

function formatDuration(secs: number): string {
  const mins = Math.round(secs / 60);
  if (mins >= 60) return `${Math.floor(mins / 60)}h ${mins % 60}m`;
  return `${mins} min`;
}

const modeConfig: Record<string, { label: string; color: string; dot: string; icon: string }> = {
  GNSS:            { label: 'GNSS Active',      color: 'text-emerald-400', dot: 'bg-emerald-400', icon: '📡' },
  DEAD_RECKONING:  { label: 'Dead Reckoning',   color: 'text-amber-400',   dot: 'bg-amber-400',   icon: '🔴' },
  FUSION:          { label: 'Kalman Fusion',    color: 'text-sky-400',     dot: 'bg-sky-400',     icon: '🔵' },
};

const PRESET_ROUTES = [
  {
    name: 'India Gate → Connaught Place',
    from: { lat: '28.6129', lon: '77.2295', display_name: 'India Gate, New Delhi' },
    to:   { lat: '28.6315', lon: '77.2167', display_name: 'Connaught Place, New Delhi' },
  },
  {
    name: 'Red Fort → Chandni Chowk',
    from: { lat: '28.6562', lon: '77.2410', display_name: 'Red Fort, Delhi' },
    to:   { lat: '28.6506', lon: '77.2303', display_name: 'Chandni Chowk, Delhi' },
  },
  {
    name: 'Lotus Temple → Qutub Minar',
    from: { lat: '28.5535', lon: '77.2588', display_name: 'Lotus Temple, New Delhi' },
    to:   { lat: '28.5245', lon: '77.1855', display_name: 'Qutub Minar, New Delhi' },
  },
];

export const RoutePanel: React.FC<RoutePanelProps> = ({
  activeRoute,
  navigationStatus,
  navigationMode,
  gnssAvailable,
  routeProgress,
  distanceRemainingMeters,
  onCreateRoute,
  onStartNavigation,
  onStopNavigation,
  onResetDemo,
  onGnssToggle,
}) => {
  const [fromQuery, setFromQuery] = useState('India Gate');
  const [toQuery, setToQuery]     = useState('Connaught Place');

  const [fromResults, setFromResults] = useState<NominatimResult[]>([]);
  const [toResults, setToResults]     = useState<NominatimResult[]>([]);

  const [selectedFrom, setSelectedFrom] = useState<NominatimResult | null>({
    lat: '28.6129', lon: '77.2295', display_name: 'India Gate, New Delhi'
  });
  const [selectedTo, setSelectedTo]     = useState<NominatimResult | null>({
    lat: '28.6315', lon: '77.2167', display_name: 'Connaught Place, New Delhi'
  });

  const [isGeocoding, setIsGeocoding] = useState(false);
  const [isCreating, setIsCreating]   = useState(false);
  const [isStarting, setIsStarting]   = useState(false);
  const [error, setError]             = useState<string | null>(null);

  const searchFrom = useCallback(async () => {
    if (!fromQuery.trim()) return;
    setIsGeocoding(true);
    try {
      const results = await geocode(fromQuery);
      setFromResults(results);
    } catch { setError('Geocoding failed for "From" location'); }
    finally   { setIsGeocoding(false); }
  }, [fromQuery]);

  const searchTo = useCallback(async () => {
    if (!toQuery.trim()) return;
    setIsGeocoding(true);
    try {
      const results = await geocode(toQuery);
      setToResults(results);
    } catch { setError('Geocoding failed for "To" location'); }
    finally   { setIsGeocoding(false); }
  }, [toQuery]);

  const handleCreateRoute = async () => {
    setError(null);
    setIsCreating(true);
    try {
      let fromLoc = selectedFrom;
      if (!fromLoc) {
        if (fromQuery.trim()) {
          const fromList = await geocode(fromQuery);
          if (fromList.length > 0) fromLoc = fromList[0];
        }
        if (!fromLoc) {
          fromLoc = { lat: '28.6129', lon: '77.2295', display_name: fromQuery.trim() || 'India Gate' };
        }
        setSelectedFrom(fromLoc);
      }

      let toLoc = selectedTo;
      if (!toLoc) {
        if (toQuery.trim()) {
          const toList = await geocode(toQuery);
          if (toList.length > 0) toLoc = toList[0];
        }
        if (!toLoc) {
          toLoc = { lat: '28.6315', lon: '77.2167', display_name: toQuery.trim() || 'Connaught Place' };
        }
        setSelectedTo(toLoc);
      }

      const req: RouteRequest = {
        from: { lat: parseFloat(fromLoc.lat), lon: parseFloat(fromLoc.lon), name: fromLoc.display_name.split(',')[0] },
        to:   { lat: parseFloat(toLoc.lat),   lon: parseFloat(toLoc.lon),   name: toLoc.display_name.split(',')[0] },
      };
      await onCreateRoute(req);
      setFromResults([]);
      setToResults([]);
    } catch (e: unknown) {
      setError('Route creation failed. Check backend connection.');
      console.error(e);
    } finally { setIsCreating(false); }
  };

  const handleStartNavigation = async () => {
    setIsStarting(true);
    try { await onStartNavigation(); }
    catch (e: unknown) { setError('Failed to start navigation'); console.error(e); }
    finally { setIsStarting(false); }
  };

  const mode = modeConfig[navigationMode] ?? modeConfig['FUSION'];
  const isNavActive = navigationStatus === 'NAVIGATION_ACTIVE';
  const isCompleted = navigationStatus === 'COMPLETED';

  return (
    <div className="bg-idr-card border border-idr-border rounded-lg p-4 space-y-4">
      {/* Header */}
      <div className="flex items-center gap-2 border-b border-idr-border pb-3">
        <Route className="w-4 h-4 text-blue-400" />
        <h2 className="text-sm font-bold font-mono tracking-widest text-slate-200 uppercase">Route Navigation Demo</h2>
      </div>

      {/* Route Selector — shown only when no route active or completed */}
      {(!activeRoute || isCompleted) && (
        <div className="space-y-3">
          {/* Quick Preset Buttons */}
          <div className="space-y-1.5 pb-1">
            <span className="text-[11px] font-mono text-slate-400 font-semibold block">QUICK DEMO PRESETS:</span>
            <div className="flex flex-wrap gap-1.5">
              {PRESET_ROUTES.map((p, idx) => (
                <button
                  key={idx}
                  type="button"
                  onClick={() => {
                    setSelectedFrom(p.from);
                    setFromQuery(p.from.display_name.split(',')[0]);
                    setSelectedTo(p.to);
                    setToQuery(p.to.display_name.split(',')[0]);
                    setError(null);
                  }}
                  className={`text-xs px-2.5 py-1 rounded font-mono border transition-all ${
                    selectedFrom?.lat === p.from.lat && selectedTo?.lat === p.to.lat
                      ? 'bg-blue-600/30 border-blue-400 text-blue-200 shadow-sm'
                      : 'bg-slate-800/80 hover:bg-slate-700 border-slate-700 text-slate-300'
                  }`}
                >
                  📍 {p.name}
                </button>
              ))}
            </div>
          </div>
          {/* FROM field */}
          <div>
            <label className="text-xs text-slate-400 font-mono mb-1 block">FROM</label>
            <div className="flex gap-2">
              <input
                type="text"
                value={fromQuery}
                onChange={e => { setFromQuery(e.target.value); setSelectedFrom(null); setFromResults([]); }}
                onKeyDown={e => e.key === 'Enter' && searchFrom()}
                placeholder="e.g. India Gate, New Delhi"
                className="flex-1 bg-slate-900 border border-slate-700 rounded px-3 py-1.5 text-sm text-slate-100 placeholder-slate-500 font-mono focus:outline-none focus:border-blue-500"
              />
              <button
                onClick={searchFrom}
                disabled={isGeocoding}
                className="p-2 rounded bg-slate-800 hover:bg-slate-700 border border-slate-600 text-blue-400 transition-colors"
              >
                {isGeocoding ? <Loader2 className="w-4 h-4 animate-spin" /> : <Search className="w-4 h-4" />}
              </button>
            </div>
            {selectedFrom && (
              <p className="text-xs text-emerald-400 mt-1 font-mono flex items-center gap-1">
                <CheckCircle className="w-3 h-3" />
                {selectedFrom.display_name.slice(0, 60)}…
              </p>
            )}
            {fromResults.length > 0 && !selectedFrom && (
              <ul className="mt-1 bg-slate-900 border border-slate-700 rounded text-xs font-mono max-h-32 overflow-y-auto">
                {fromResults.map((r, i) => (
                  <li
                    key={i}
                    onClick={() => { setSelectedFrom(r); setFromQuery(r.display_name.split(',')[0]); setFromResults([]); }}
                    className="px-3 py-1.5 hover:bg-slate-800 cursor-pointer text-slate-300 border-b border-slate-800 last:border-0"
                  >
                    📍 {r.display_name.slice(0, 70)}
                  </li>
                ))}
              </ul>
            )}
          </div>

          {/* TO field */}
          <div>
            <label className="text-xs text-slate-400 font-mono mb-1 block">TO</label>
            <div className="flex gap-2">
              <input
                type="text"
                value={toQuery}
                onChange={e => { setToQuery(e.target.value); setSelectedTo(null); setToResults([]); }}
                onKeyDown={e => e.key === 'Enter' && searchTo()}
                placeholder="e.g. Connaught Place, New Delhi"
                className="flex-1 bg-slate-900 border border-slate-700 rounded px-3 py-1.5 text-sm text-slate-100 placeholder-slate-500 font-mono focus:outline-none focus:border-blue-500"
              />
              <button
                onClick={searchTo}
                disabled={isGeocoding}
                className="p-2 rounded bg-slate-800 hover:bg-slate-700 border border-slate-600 text-blue-400 transition-colors"
              >
                {isGeocoding ? <Loader2 className="w-4 h-4 animate-spin" /> : <Search className="w-4 h-4" />}
              </button>
            </div>
            {selectedTo && (
              <p className="text-xs text-emerald-400 mt-1 font-mono flex items-center gap-1">
                <CheckCircle className="w-3 h-3" />
                {selectedTo.display_name.slice(0, 60)}…
              </p>
            )}
            {toResults.length > 0 && !selectedTo && (
              <ul className="mt-1 bg-slate-900 border border-slate-700 rounded text-xs font-mono max-h-32 overflow-y-auto">
                {toResults.map((r, i) => (
                  <li
                    key={i}
                    onClick={() => { setSelectedTo(r); setToQuery(r.display_name.split(',')[0]); setToResults([]); }}
                    className="px-3 py-1.5 hover:bg-slate-800 cursor-pointer text-slate-300 border-b border-slate-800 last:border-0"
                  >
                    🏁 {r.display_name.slice(0, 70)}
                  </li>
                ))}
              </ul>
            )}
          </div>

          {error && (
            <div className="flex items-center gap-2 text-xs text-red-400 font-mono">
              <AlertTriangle className="w-3.5 h-3.5" /> {error}
            </div>
          )}

          <button
            onClick={handleCreateRoute}
            disabled={isCreating || (!selectedFrom && !fromQuery.trim()) || (!selectedTo && !toQuery.trim())}
            className="w-full py-2 rounded bg-blue-700 hover:bg-blue-600 disabled:opacity-40 disabled:cursor-not-allowed text-white font-bold text-sm font-mono tracking-wider flex items-center justify-center gap-2 transition-colors"
          >
            {isCreating ? <Loader2 className="w-4 h-4 animate-spin" /> : <MapPin className="w-4 h-4" />}
            {isCreating ? 'ROUTING…' : 'CREATE ROUTE'}
          </button>
        </div>
      )}

      {/* Route Info — shown after route is created */}
      {activeRoute && !isCompleted && (
        <div className="space-y-3">
          {/* Route metadata */}
          <div className="bg-slate-900/60 rounded p-3 space-y-1.5 font-mono text-xs border border-slate-800">
            <div className="flex items-center gap-2 text-slate-300">
              <span className="text-emerald-400">▶ FROM</span>
              <span className="truncate">{activeRoute.fromName}</span>
            </div>
            <div className="flex items-center gap-2 text-slate-300">
              <span className="text-rose-400">■ TO</span>
              <span className="truncate">{activeRoute.toName}</span>
            </div>
            <div className="flex gap-4 text-slate-400 border-t border-slate-800 pt-1.5 mt-1">
              <span>📏 {formatDistance(activeRoute.totalDistanceMeters)}</span>
              <span>⏱ {formatDuration(activeRoute.estimatedDurationSeconds)}</span>
              <span className={activeRoute.routeSource === 'OSRM' ? 'text-emerald-500' : 'text-amber-500'}>
                {activeRoute.routeSource === 'OSRM' ? '🗺 OSRM' : '⚠ Interpolated'}
              </span>
            </div>
          </div>

          {/* Navigation Mode Indicator */}
          {isNavActive && (
            <>
            <div className={`flex items-center gap-2 px-3 py-2 rounded border font-mono text-xs font-bold
              ${navigationMode === 'GNSS' ? 'border-emerald-700 bg-emerald-950/40 text-emerald-300'
              : navigationMode === 'DEAD_RECKONING' ? 'border-amber-700 bg-amber-950/40 text-amber-300'
              : 'border-sky-700 bg-sky-950/40 text-sky-300'}`}
            >
              <span className={`w-2 h-2 rounded-full ${mode.dot} animate-pulse`} />
              <span>{mode.icon} {mode.label}</span>
              <span className="ml-auto text-slate-400">{gnssAvailable ? 'GNSS: ON' : 'GNSS: OFF'}</span>
            </div>
            <div className={`px-3 py-1.5 rounded border font-mono text-[11px] font-bold ${
              gnssAvailable
                ? 'border-sky-800 bg-sky-950/30 text-sky-300'
                : 'border-amber-800 bg-amber-950/30 text-amber-300'
            }`}>
              POSITION SOURCE: {gnssAvailable ? 'GNSS' : 'DEAD RECKONING / PREDICTED'}
            </div>
            </>
          )}

          {/* Progress bar */}
          {isNavActive && (
            <div className="space-y-1">
              <div className="flex justify-between text-xs font-mono text-slate-400">
                <span>Route Progress</span>
                <span>{Math.round(routeProgress * 100)}%</span>
              </div>
              <div className="w-full h-2 bg-slate-800 rounded-full overflow-hidden">
                <div
                  className={`h-2 rounded-full transition-all duration-300 ${
                    navigationMode === 'DEAD_RECKONING' ? 'bg-amber-500' :
                    navigationMode === 'GNSS'           ? 'bg-emerald-500' : 'bg-sky-500'
                  }`}
                  style={{ width: `${routeProgress * 100}%` }}
                />
              </div>
              <div className="text-xs font-mono text-slate-400">
                Remaining: <span className="text-slate-200 font-bold">{formatDistance(distanceRemainingMeters)}</span>
              </div>
            </div>
          )}

          {/* GNSS Toggle Button (single button) */}
          {isNavActive && (
            <button
              onClick={onGnssToggle}
              className={`w-full py-2 rounded font-bold text-sm font-mono tracking-wider flex items-center justify-center gap-2 transition-colors border ${
                gnssAvailable
                  ? 'bg-rose-900/60 hover:bg-rose-800/70 border-rose-700 text-rose-300'
                  : 'bg-emerald-900/60 hover:bg-emerald-800/70 border-emerald-700 text-emerald-300'
              }`}
            >
              {gnssAvailable ? '🔴 TURN GNSS OFF' : '📡 TURN GNSS ON'}
            </button>
          )}

          {/* Action buttons */}
          <div className="flex gap-2">
            {navigationStatus === 'ROUTE_SELECTED' && (
              <button
                onClick={handleStartNavigation}
                disabled={isStarting}
                className="flex-1 py-2 rounded bg-emerald-700 hover:bg-emerald-600 disabled:opacity-40 text-white font-bold text-sm font-mono tracking-wider flex items-center justify-center gap-2 transition-colors"
              >
                {isStarting ? <Loader2 className="w-4 h-4 animate-spin" /> : <Navigation className="w-4 h-4" />}
                {isStarting ? 'STARTING…' : 'START NAVIGATION'}
              </button>
            )}

            {isNavActive && (
              <button
                onClick={onStopNavigation}
                className="flex-1 py-2 rounded bg-slate-700 hover:bg-slate-600 text-slate-200 font-bold text-sm font-mono tracking-wider flex items-center justify-center gap-2 transition-colors"
              >
                ■ STOP
              </button>
            )}

            <button
              onClick={onResetDemo}
              className="py-2 px-3 rounded bg-slate-800 hover:bg-slate-700 border border-slate-600 text-slate-300 font-mono text-sm flex items-center gap-1.5 transition-colors"
              title="Reset Demo"
            >
              <RotateCcw className="w-4 h-4" /> RESET
            </button>
          </div>
        </div>
      )}

      {/* Completed state */}
      {isCompleted && (
        <div className="text-center py-3 space-y-2">
          <p className="text-emerald-400 font-mono text-sm font-bold">✅ DESTINATION REACHED</p>
          <button
            onClick={onResetDemo}
            className="w-full py-2 rounded bg-slate-700 hover:bg-slate-600 text-slate-200 font-bold text-sm font-mono tracking-wider flex items-center justify-center gap-2 transition-colors"
          >
            <RotateCcw className="w-4 h-4" /> RESET DEMO
          </button>
        </div>
      )}
    </div>
  );
};
