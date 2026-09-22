import React from 'react';
import { Play, Pause, Square, RotateCcw, Smartphone, Cpu } from 'lucide-react';

interface SessionControlsProps {
  sessionStatus: string;
  sessionMode: string;
  isSimulating: boolean;
  onStart: () => void;
  onPause: () => void;
  onStop: () => void;
  onReset: () => void;
  onToggleMode: (mode: 'SIMULATION' | 'REAL_SENSOR') => void;
  onOpenMobileSensorModal: () => void;
}

export const SessionControls: React.FC<SessionControlsProps> = ({
  sessionStatus,
  sessionMode,
  isSimulating,
  onStart,
  onPause,
  onStop,
  onReset,
  onToggleMode,
  onOpenMobileSensorModal,
}) => {
  const isRunning = sessionStatus === 'ACTIVE';

  return (
    <div className="bg-idr-panel border border-idr-border rounded-lg p-4 shadow-md flex flex-wrap items-center justify-between gap-4">
      {/* Primary Lifecycle Controls */}
      <div className="flex items-center gap-2">
        {!isRunning ? (
          <button
            onClick={onStart}
            className="flex items-center gap-2 px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded font-mono text-xs font-semibold uppercase tracking-wider transition-all shadow-md shadow-emerald-900/30"
          >
            <Play className="w-4 h-4 fill-current" />
            Start Navigation
          </button>
        ) : (
          <button
            onClick={onPause}
            className="flex items-center gap-2 px-4 py-2 bg-amber-600 hover:bg-amber-500 text-white rounded font-mono text-xs font-semibold uppercase tracking-wider transition-all shadow-md shadow-amber-900/30"
          >
            <Pause className="w-4 h-4 fill-current" />
            Pause
          </button>
        )}

        <button
          onClick={onStop}
          disabled={sessionStatus === 'STOPPED'}
          className="flex items-center gap-1.5 px-3 py-2 bg-slate-800 hover:bg-slate-700 disabled:opacity-40 disabled:cursor-not-allowed text-slate-200 rounded font-mono text-xs tracking-wider transition-colors border border-slate-700"
        >
          <Square className="w-3.5 h-3.5 fill-current" />
          Stop
        </button>

        <button
          onClick={onReset}
          className="flex items-center gap-1.5 px-3 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded font-mono text-xs tracking-wider transition-colors border border-slate-700"
          title="Reset session and clear map trajectories"
        >
          <RotateCcw className="w-3.5 h-3.5" />
          Reset
        </button>
      </div>

      {/* Input Mode Selector */}
      <div className="flex items-center gap-3">
        <div className="flex items-center rounded bg-slate-900 p-1 border border-idr-border">
          <button
            onClick={() => onToggleMode('SIMULATION')}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded text-xs font-mono transition-all ${
              sessionMode === 'SIMULATION'
                ? 'bg-blue-600 text-white font-medium shadow-sm'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <Cpu className="w-3.5 h-3.5" />
            Simulation Mode {isSimulating && <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-ping ml-1" />}
          </button>

          <button
            onClick={() => onToggleMode('REAL_SENSOR')}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded text-xs font-mono transition-all ${
              sessionMode === 'REAL_SENSOR'
                ? 'bg-blue-600 text-white font-medium shadow-sm'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <Smartphone className="w-3.5 h-3.5" />
            Real Sensor Mode
          </button>
        </div>

        {sessionMode === 'REAL_SENSOR' && (
          <button
            onClick={onOpenMobileSensorModal}
            className="flex items-center gap-1 px-3 py-1.5 bg-blue-600/20 hover:bg-blue-600/30 text-blue-400 border border-blue-500/40 rounded font-mono text-xs transition-colors"
          >
            <Smartphone className="w-3.5 h-3.5" />
            Mobile Sensor Hub
          </button>
        )}
      </div>
    </div>
  );
};
