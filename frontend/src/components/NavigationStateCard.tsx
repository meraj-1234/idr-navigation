import React from 'react';
import { Compass, Gauge, MapPin, Layers } from 'lucide-react';
import { NavigationState } from '../types/navigation';

interface NavigationStateCardProps {
  state: NavigationState;
}

export const NavigationStateCard: React.FC<NavigationStateCardProps> = ({ state }) => {
  const velMs = state.velocity !== null && state.velocity !== undefined ? state.velocity : null;
  const velKmh = velMs !== null ? velMs * 3.6 : null;
  const heading = state.heading !== null && state.heading !== undefined ? state.heading : null;

  return (
    <div className="bg-idr-panel border border-idr-border rounded-lg p-4 shadow-md">
      <div className="flex items-center justify-between border-b border-idr-border/60 pb-2 mb-3">
        <div className="flex items-center gap-2">
          <MapPin className="w-4 h-4 text-emerald-400" />
          <h2 className="text-xs font-mono font-bold uppercase tracking-wider text-slate-200">
            Estimated Navigation State
          </h2>
        </div>
        <span className="text-[11px] font-mono px-2 py-0.5 rounded bg-blue-900/40 text-blue-300 border border-blue-700/50">
          POSITION SOURCE: {state.gnssAvailable ? 'GNSS' : 'DEAD RECKONING / PREDICTED'}
        </span>
      </div>

      <div className="space-y-2 text-xs font-mono">
        {/* Estimated Coordinates */}
        <div className="grid grid-cols-2 gap-2">
          <div className="bg-slate-900 p-2 rounded border border-idr-border/40">
            <span className="text-[10px] text-idr-textMuted block">ESTIMATED LATITUDE</span>
            <span className="text-emerald-400 font-bold text-sm">
              {state.latitude ? state.latitude.toFixed(6) : 'N/A'}
            </span>
          </div>
          <div className="bg-slate-900 p-2 rounded border border-idr-border/40">
            <span className="text-[10px] text-idr-textMuted block">ESTIMATED LONGITUDE</span>
            <span className="text-emerald-400 font-bold text-sm">
              {state.longitude ? state.longitude.toFixed(6) : 'N/A'}
            </span>
          </div>
        </div>

        {/* Speed and Heading */}
        <div className="grid grid-cols-2 gap-2">
          <div className="bg-slate-900 p-2 rounded border border-idr-border/40 flex items-center justify-between">
            <div>
              <span className="text-[10px] text-idr-textMuted block">VELOCITY</span>
              <span className="text-slate-100 font-bold">
                {velMs !== null ? `${velMs.toFixed(1)} m/s` : 'N/A'}
              </span>
              <span className="text-[10px] text-slate-400 block">
                {velKmh !== null ? `(${velKmh.toFixed(1)} km/h)` : ''}
              </span>
            </div>
            <Gauge className="w-4 h-4 text-slate-500" />
          </div>

          <div className="bg-slate-900 p-2 rounded border border-idr-border/40 flex items-center justify-between">
            <div>
              <span className="text-[10px] text-idr-textMuted block">HEADING / BEARING</span>
              <span className="text-slate-100 font-bold">
                {heading !== null ? `${heading.toFixed(1)}°` : 'N/A'}
              </span>
              <span className="text-[10px] text-slate-400 block">
                {heading !== null ? getCompassDirection(heading) : ''}
              </span>
            </div>
            <Compass
              className="w-5 h-5 text-slate-400 transition-transform duration-200"
              style={{ transform: `rotate(${heading ?? 0}deg)` }}
            />
          </div>
        </div>

        {/* Map Matching Snapped Status */}
        <div className="bg-slate-900 p-2 rounded border border-idr-border/40 flex items-center justify-between">
          <div className="flex items-center gap-1.5">
            <Layers className="w-3.5 h-3.5 text-slate-400" />
            <span className="text-[11px] text-idr-textMuted">MAP MATCHED SNAP:</span>
          </div>
          <span className="text-[11px] font-semibold text-slate-200">
            {state.mapMatchedLatitude
              ? `${state.mapMatchedLatitude.toFixed(6)}, ${state.mapMatchedLongitude?.toFixed(6)}`
              : 'BASELINE ROAD CORRIDOR'}
          </span>
        </div>
      </div>
    </div>
  );
};

function getCompassDirection(degrees: number): string {
  const dirs = ['N', 'NNE', 'NE', 'ENE', 'E', 'ESE', 'SE', 'SSE', 'S', 'SSW', 'SW', 'WSW', 'W', 'WNW', 'NW', 'NNW'];
  const index = Math.round(((degrees % 360) / 22.5)) % 16;
  return dirs[index];
}
