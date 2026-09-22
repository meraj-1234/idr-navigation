import React from 'react';
import { Radio, AlertOctagon, RotateCw } from 'lucide-react';

interface GnssControlsProps {
  gnssAvailable: boolean;
  onGnssOn: () => void;
  onGnssLoss: () => void;
  onGnssRestore: () => void;
}

export const GnssControls: React.FC<GnssControlsProps> = ({
  gnssAvailable,
  onGnssOn,
  onGnssLoss,
  onGnssRestore,
}) => {
  return (
    <div className="bg-idr-panel border border-idr-border rounded-lg p-4 shadow-md">
      <div className="flex items-center justify-between mb-3 border-b border-idr-border/60 pb-2">
        <div className="flex items-center gap-2">
          <Radio className="w-4 h-4 text-blue-400" />
          <h2 className="text-xs font-mono font-bold uppercase tracking-wider text-slate-200">
            GNSS Outage Simulation Controls
          </h2>
        </div>
        <span
          className={`text-[10px] font-mono px-2 py-0.5 rounded font-bold uppercase ${
            gnssAvailable
              ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/40'
              : 'bg-rose-500/20 text-rose-400 border border-rose-500/40 animate-pulse'
          }`}
        >
          GNSS: {gnssAvailable ? 'ON' : 'OFF'}
        </span>
      </div>

      <p className="text-xs text-idr-textMuted mb-3">
        Manually trigger GNSS loss to observe instantaneous transition to Dead Reckoning navigation,
        or restore signal to observe Kalman fusion re-acquisition.
      </p>

      <div className="grid grid-cols-3 gap-2">
        <button
          onClick={onGnssOn}
          disabled={gnssAvailable}
          className={`flex items-center justify-center gap-1.5 py-2 px-3 rounded font-mono text-xs font-medium transition-all ${
            gnssAvailable
              ? 'bg-slate-800 text-slate-500 border border-slate-700/50 cursor-not-allowed'
              : 'bg-blue-600 hover:bg-blue-500 text-white shadow-md'
          }`}
        >
          <Radio className="w-3.5 h-3.5" />
          GNSS ON
        </button>

        <button
          onClick={onGnssLoss}
          disabled={!gnssAvailable}
          className={`flex items-center justify-center gap-1.5 py-2 px-3 rounded font-mono text-xs font-semibold transition-all ${
            !gnssAvailable
              ? 'bg-rose-950/40 text-rose-700 border border-rose-900/40 cursor-not-allowed'
              : 'bg-rose-600 hover:bg-rose-500 text-white shadow-md shadow-rose-950/50 animate-pulse'
          }`}
        >
          <AlertOctagon className="w-3.5 h-3.5" />
          GNSS LOSS
        </button>

        <button
          onClick={onGnssRestore}
          disabled={gnssAvailable}
          className={`flex items-center justify-center gap-1.5 py-2 px-3 rounded font-mono text-xs font-semibold transition-all ${
            gnssAvailable
              ? 'bg-slate-800 text-slate-500 border border-slate-700/50 cursor-not-allowed'
              : 'bg-emerald-600 hover:bg-emerald-500 text-white shadow-md shadow-emerald-950/50'
          }`}
        >
          <RotateCw className="w-3.5 h-3.5" />
          GNSS RESTORE
        </button>
      </div>
    </div>
  );
};
