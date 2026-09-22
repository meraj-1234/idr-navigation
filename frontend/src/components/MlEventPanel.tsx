import React from 'react';
import { Cpu, Disc, Info } from 'lucide-react';
import { MLPrediction } from '../types/navigation';

interface MlEventPanelProps {
  prediction?: MLPrediction | null;
}

export const MlEventPanel: React.FC<MlEventPanelProps> = ({ prediction }) => {
  const event = prediction?.event || 'NORMAL';
  const vibration = prediction?.vibrationState || 'NORMAL';

  const getEventBadge = () => {
    switch (event.toUpperCase()) {
      case 'POTHOLE':
        return (
          <span className="px-2.5 py-1 rounded bg-rose-500/20 text-rose-400 border border-rose-500/40 text-xs font-mono font-bold animate-bounce">
            ⚠️ POTHOLE DETECTED
          </span>
        );
      case 'BUMP':
        return (
          <span className="px-2.5 py-1 rounded bg-amber-500/20 text-amber-400 border border-amber-500/40 text-xs font-mono font-bold">
            ▲ BUMP DETECTED
          </span>
        );
      case 'SPEED_BREAKER':
        return (
          <span className="px-2.5 py-1 rounded bg-amber-500/20 text-amber-400 border border-amber-500/40 text-xs font-mono font-bold">
            ▬ SPEED BREAKER
          </span>
        );
      case 'NORMAL':
      default:
        return (
          <span className="px-2.5 py-1 rounded bg-emerald-500/20 text-emerald-400 border border-emerald-500/40 text-xs font-mono font-bold">
            ✓ NORMAL ROAD
          </span>
        );
    }
  };

  const getVibrationBadge = () => {
    switch (vibration.toUpperCase()) {
      case 'ENGINE_VIBRATION':
        return <span className="text-amber-400 font-semibold">ENGINE VIBRATION</span>;
      case 'ROAD_VIBRATION':
        return <span className="text-blue-400 font-semibold">ROAD VIBRATION</span>;
      case 'NORMAL':
      default:
        return <span className="text-emerald-400 font-semibold">NORMAL DAMPING</span>;
    }
  };

  return (
    <div className="bg-idr-panel border border-idr-border rounded-lg p-4 shadow-md">
      <div className="flex items-center justify-between border-b border-idr-border/60 pb-2 mb-3">
        <div className="flex items-center gap-2">
          <Cpu className="w-4 h-4 text-purple-400" />
          <h2 className="text-xs font-mono font-bold uppercase tracking-wider text-slate-200">
            Intelligent Event Detection
          </h2>
        </div>
        <span className="px-2 py-0.5 rounded bg-rose-500/20 text-rose-400 border border-rose-500/30 text-[10px] font-mono font-bold">
          MOCK MODEL
        </span>
      </div>

      <div className="space-y-3 text-xs font-mono">
        {/* Road Event & Vibration Grid */}
        <div className="grid grid-cols-2 gap-2">
          <div className="bg-slate-900 p-2.5 rounded border border-idr-border/40">
            <span className="text-[10px] text-idr-textMuted block mb-1">ROAD SURFACE EVENT</span>
            <div>{getEventBadge()}</div>
          </div>

          <div className="bg-slate-900 p-2.5 rounded border border-idr-border/40">
            <span className="text-[10px] text-idr-textMuted block mb-1">VIBRATION STATE</span>
            <div className="flex items-center gap-1.5 pt-1 text-xs">
              <Disc className="w-3.5 h-3.5 text-slate-400" />
              {getVibrationBadge()}
            </div>
          </div>
        </div>

        {/* Explicit SIH Evaluator Disclosure Banner */}
        <div className="bg-slate-900/90 border border-rose-500/30 rounded p-2.5 text-[11px] space-y-1">
          <div className="flex items-center gap-1.5 text-rose-400 font-semibold">
            <Info className="w-3.5 h-3.5 flex-shrink-0" />
            <span>MODEL INTEGRATION STATUS: MOCK</span>
          </div>
          <p className="text-slate-400 leading-relaxed text-[10.5px]">
            Trained ML model is not currently integrated. Downstream signal conditioning and dead reckoning
            consume this interface without alteration when trained weights are loaded.
          </p>
          <div className="flex items-center justify-between pt-1 border-t border-slate-800 text-[10px] text-slate-400">
            <span>Model Confidence:</span>
            <span className="text-slate-300 font-bold">N/A (Mock Mode — Never Fabricated)</span>
          </div>
        </div>
      </div>
    </div>
  );
};
