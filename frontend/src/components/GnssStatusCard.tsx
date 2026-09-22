import React from 'react';
import { Satellite, AlertOctagon, CheckCircle2, Clock } from 'lucide-react';
import { NavigationState } from '../types/navigation';

interface GnssStatusCardProps {
  state: NavigationState;
}

export const GnssStatusCard: React.FC<GnssStatusCardProps> = ({ state }) => {
  const isAvailable = state.gnssAvailable;

  const formatCoord = (val?: number | null) => (val !== null && val !== undefined ? val.toFixed(6) : 'N/A');
  const formatAcc = (val?: number | null) => (val !== null && val !== undefined ? `±${val.toFixed(1)} m` : 'N/A');

  return (
    <div className="bg-idr-panel border border-idr-border rounded-lg p-4 shadow-md">
      {/* Card Header */}
      <div className="flex items-center justify-between border-b border-idr-border/60 pb-2 mb-3">
        <div className="flex items-center gap-2">
          <Satellite className={`w-4 h-4 ${isAvailable ? 'text-sky-400' : 'text-rose-500'}`} />
          <h2 className="text-xs font-mono font-bold uppercase tracking-wider text-slate-200">
            GNSS Receiver Telemetry
          </h2>
        </div>
        <div className="flex items-center gap-1.5">
          {isAvailable ? (
            <span className="flex items-center gap-1 px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-400 border border-emerald-500/40 text-[11px] font-mono font-bold">
              <CheckCircle2 className="w-3 h-3" />
              GNSS: ON
            </span>
          ) : (
            <span className="flex items-center gap-1 px-2 py-0.5 rounded bg-rose-500/20 text-rose-400 border border-rose-500/40 text-[11px] font-mono font-bold animate-pulse">
              <AlertOctagon className="w-3 h-3" />
              GNSS: OFF
            </span>
          )}
        </div>
      </div>

      {isAvailable ? (
        <div className="space-y-2 text-xs font-mono">
          <div className="grid grid-cols-2 gap-2">
            <div className="bg-slate-900 p-2 rounded border border-idr-border/40">
              <span className="text-[10px] text-idr-textMuted block">GNSS LATITUDE</span>
              <span className="text-sky-400 font-semibold">{formatCoord(state.gnssLatitude)}</span>
            </div>
            <div className="bg-slate-900 p-2 rounded border border-idr-border/40">
              <span className="text-[10px] text-idr-textMuted block">GNSS LONGITUDE</span>
              <span className="text-sky-400 font-semibold">{formatCoord(state.gnssLongitude)}</span>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-2">
            <div className="bg-slate-900 p-2 rounded border border-idr-border/40">
              <span className="text-[10px] text-idr-textMuted block">REPORTED ACCURACY</span>
              <span className="text-slate-200 font-medium">{formatAcc(state.gnssAccuracy)}</span>
            </div>
            <div className="bg-slate-900 p-2 rounded border border-idr-border/40">
              <span className="text-[10px] text-idr-textMuted block">POSITION SOURCE</span>
              <span className="text-sky-300 font-medium">GNSS</span>
            </div>
          </div>
        </div>
      ) : (
        <div className="bg-rose-950/30 border border-rose-800/40 rounded p-3 text-xs font-mono space-y-2">
          <div className="flex items-center gap-2 text-rose-400 font-bold">
            <AlertOctagon className="w-4 h-4" />
            <span>GNSS SIGNAL OUTAGE DETECTED</span>
          </div>
          <p className="text-[11px] text-slate-300">
            GNSS signal-loss simulation is active. The marker continues using the dummy
            dead-reckoning / predicted position — not a live satellite fix.
          </p>
          <div className="text-[11px] text-amber-300 font-bold">
            POSITION SOURCE: DEAD RECKONING / PREDICTED
          </div>
          <div className="flex items-center gap-1.5 text-[11px] text-rose-300 pt-1 border-t border-rose-900/40">
            <Clock className="w-3 h-3" />
            <span>
              Loss Registered:{' '}
              {state.gnssLossTimestamp
                ? new Date(state.gnssLossTimestamp).toLocaleTimeString()
                : 'Active Outage'}
            </span>
          </div>
        </div>
      )}
    </div>
  );
};
