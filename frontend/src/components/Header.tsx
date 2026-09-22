import React from 'react';
import { Navigation2, Cpu, Activity, AlertTriangle, ShieldCheck } from 'lucide-react';

interface HeaderProps {
  isConnected: boolean;
  navigationMode: string;
  gnssAvailable: boolean;
  sessionStatus: string;
}

export const Header: React.FC<HeaderProps> = ({
  isConnected,
  navigationMode,
  gnssAvailable,
  sessionStatus,
}) => {
  const getModeBadge = () => {
    switch (navigationMode) {
      case 'DEAD_RECKONING':
        return (
          <span className="flex items-center gap-1.5 px-3 py-1 rounded bg-amber-500/20 text-amber-400 border border-amber-500/40 text-xs font-mono font-semibold uppercase tracking-wider animate-pulse">
            <AlertTriangle className="w-3.5 h-3.5" />
            DEAD RECKONING (GNSS DENIED)
          </span>
        );
      case 'FUSION':
        return (
          <span className="flex items-center gap-1.5 px-3 py-1 rounded bg-emerald-500/20 text-emerald-400 border border-emerald-500/40 text-xs font-mono font-semibold uppercase tracking-wider">
            <ShieldCheck className="w-3.5 h-3.5" />
            KALMAN FUSION ACTIVE
          </span>
        );
      case 'GNSS':
      default:
        return (
          <span className="flex items-center gap-1.5 px-3 py-1 rounded bg-blue-500/20 text-blue-400 border border-blue-500/40 text-xs font-mono font-semibold uppercase tracking-wider">
            <Navigation2 className="w-3.5 h-3.5" />
            GNSS FIX
          </span>
        );
    }
  };

  return (
    <header className="bg-idr-panel border-b border-idr-border px-6 py-3 shadow-lg">
      <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-3">
        {/* Title and Branding */}
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-lg bg-blue-600/20 border border-blue-500/40 flex items-center justify-center text-blue-400">
            <Navigation2 className="w-6 h-6 transform rotate-45" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-xl font-bold tracking-tight text-white font-mono">IDR NAV</h1>
              <span className="text-[11px] font-mono px-2 py-0.5 rounded bg-slate-800 text-slate-300 border border-slate-700">
                v1.0-PROTOTYPE
              </span>
            </div>
            <p className="text-xs text-idr-textMuted tracking-wide">
              Intelligent Dead Reckoning System for Seamless Navigation in GNSS-Denied Environments
            </p>
          </div>
        </div>

        {/* Telemetry Status Bar */}
        <div className="flex flex-wrap items-center gap-3">
          {/* Connection Status */}
          <div className="flex items-center gap-2 px-3 py-1 rounded bg-slate-900 border border-idr-border text-xs font-mono">
            <span
              className={`w-2 h-2 rounded-full ${
                isConnected ? 'bg-emerald-400 shadow-[0_0_8px_rgba(52,211,153,0.8)]' : 'bg-rose-500'
              }`}
            />
            <span className={isConnected ? 'text-slate-200' : 'text-rose-400'}>
              {isConnected ? 'TELEMETRY LIVE' : 'DISCONNECTED'}
            </span>
          </div>

          {/* Session State */}
          <div className="flex items-center gap-1.5 px-3 py-1 rounded bg-slate-900 border border-idr-border text-xs font-mono">
            <Activity className="w-3.5 h-3.5 text-blue-400" />
            <span className="text-slate-400">SESSION:</span>
            <span
              className={`font-semibold ${
                sessionStatus === 'ACTIVE'
                  ? 'text-emerald-400'
                  : sessionStatus === 'PAUSED'
                  ? 'text-amber-400'
                  : 'text-slate-400'
              }`}
            >
              {sessionStatus}
            </span>
          </div>

          {/* Navigation Mode */}
          {getModeBadge()}

          {/* GNSS Hardware Status */}
          <div className="flex items-center gap-1.5 px-3 py-1 rounded bg-slate-900 border border-idr-border text-xs font-mono">
            <span
              className={`w-2 h-2 rounded-full ${
                gnssAvailable ? 'bg-sky-400' : 'bg-rose-500 animate-pulse'
              }`}
            />
            <span className={gnssAvailable ? 'text-sky-300' : 'text-rose-400 font-bold'}>
              GNSS: {gnssAvailable ? 'ON' : 'OFF'}
            </span>
          </div>

          {/* Explicit Mock Model Banner */}
          <div className="flex items-center gap-1.5 px-3 py-1 rounded bg-rose-500/15 text-rose-300 border border-rose-500/30 text-xs font-mono font-medium">
            <Cpu className="w-3.5 h-3.5 text-rose-400" />
            <span>MODEL: MOCK (TRAINED MODEL NOT INTEGRATED)</span>
          </div>
        </div>
      </div>
    </header>
  );
};
