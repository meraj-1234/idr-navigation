import React, { useRef, useEffect } from 'react';
import { Terminal, Trash2 } from 'lucide-react';
import { SystemEvent } from '../types/navigation';

interface EventLogConsoleProps {
  events: SystemEvent[];
  onClear: () => void;
}

export const EventLogConsole: React.FC<EventLogConsoleProps> = ({ events, onClear }) => {
  const scrollRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = 0; // Recent events are at top
    }
  }, [events]);

  const getSeverityStyle = (severity: string) => {
    switch (severity) {
      case 'ALERT':
        return 'text-rose-400 bg-rose-950/30 border-rose-800/40';
      case 'WARN':
        return 'text-amber-400 bg-amber-950/20 border-amber-800/30';
      case 'SUCCESS':
        return 'text-emerald-400 bg-emerald-950/20 border-emerald-800/30';
      case 'INFO':
      default:
        return 'text-sky-300 bg-slate-900/60 border-slate-800';
    }
  };

  return (
    <div className="bg-idr-panel border border-idr-border rounded-lg p-4 shadow-md">
      <div className="flex items-center justify-between border-b border-idr-border/60 pb-2 mb-2">
        <div className="flex items-center gap-2">
          <Terminal className="w-4 h-4 text-blue-400" />
          <h2 className="text-xs font-mono font-bold uppercase tracking-wider text-slate-200">
            Real-Time System Event Log
          </h2>
        </div>
        <div className="flex items-center gap-3">
          <span className="text-[10px] font-mono text-slate-400">
            {events.length} Events Recorded
          </span>
          <button
            onClick={onClear}
            className="p-1 text-slate-500 hover:text-slate-300 transition-colors"
            title="Clear console view"
          >
            <Trash2 className="w-3.5 h-3.5" />
          </button>
        </div>
      </div>

      <div
        ref={scrollRef}
        className="h-36 overflow-y-auto font-mono text-[11px] space-y-1 pr-1"
      >
        {events.length === 0 ? (
          <div className="text-slate-600 italic py-4 text-center">
            Awaiting navigation session events...
          </div>
        ) : (
          events.map((ev, i) => (
            <div
              key={`${ev.timestamp}-${i}`}
              className={`px-2 py-1 rounded border flex items-start gap-2 ${getSeverityStyle(
                ev.severity
              )}`}
            >
              <span className="text-[10px] text-slate-400 flex-shrink-0 font-semibold">
                {ev.formattedTime || new Date(ev.timestamp).toLocaleTimeString()}
              </span>
              <span className="font-bold flex-shrink-0">[{ev.eventType}]</span>
              <span className="text-slate-200">{ev.message}</span>
            </div>
          ))
        )}
      </div>
    </div>
  );
};
