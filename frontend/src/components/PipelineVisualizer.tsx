import React from 'react';
import { GitCommit, ArrowRight } from 'lucide-react';
import { PipelineStatus } from '../types/navigation';

interface PipelineVisualizerProps {
  status?: PipelineStatus | null;
  gnssAvailable: boolean;
}

interface StageDefinition {
  id: keyof PipelineStatus;
  label: string;
  sublabel: string;
}

const STAGES: StageDefinition[] = [
  { id: 'sensorIngestion', label: 'Sensor Input', sublabel: 'IMU / GNSS' },
  { id: 'synchronization', label: 'Sync & dt', sublabel: 'Temporal Alignment' },
  { id: 'noiseFiltering', label: 'Noise Filter', sublabel: 'Low-Pass' },
  { id: 'coordinateTransform', label: 'Coord Transform', sublabel: 'Phone-to-Vehicle' },
  { id: 'mlEventDetection', label: 'ML Event Detection', sublabel: 'Mock Model' },
  { id: 'deadReckoning', label: 'Dead Reckoning', sublabel: 'Inertial INS' },
  { id: 'kalmanFusion', label: 'Kalman Fusion', sublabel: 'GNSS + INS' },
  { id: 'mapMatching', label: 'Map Matching', sublabel: 'Road Projection' },
  { id: 'navigationOutput', label: 'Nav Output', sublabel: 'Telemetry Stream' },
];

export const PipelineVisualizer: React.FC<PipelineVisualizerProps> = ({ status, gnssAvailable }) => {
  const getBadgeStyle = (stageId: keyof PipelineStatus, rawStatus?: string) => {
    if (stageId === 'mlEventDetection') {
      return 'bg-purple-950/60 text-purple-300 border-purple-600/50';
    }

    if (stageId === 'kalmanFusion' && !gnssAvailable) {
      return 'bg-amber-950/60 text-amber-300 border-amber-600/50 animate-pulse';
    }

    switch (rawStatus?.toUpperCase()) {
      case 'ACTIVE':
        return 'bg-emerald-950/60 text-emerald-300 border-emerald-600/50';
      case 'GNSS_DENIED':
        return 'bg-amber-950/60 text-amber-300 border-amber-600/50';
      case 'STANDBY':
      default:
        return 'bg-slate-900 text-slate-500 border-slate-800';
    }
  };

  const getStatusText = (stageId: keyof PipelineStatus, rawStatus?: string) => {
    if (stageId === 'mlEventDetection') return 'MOCK';
    if (stageId === 'kalmanFusion' && !gnssAvailable) return 'GNSS DENIED';
    return rawStatus || 'STANDBY';
  };

  return (
    <div className="bg-idr-panel border border-idr-border rounded-lg p-4 shadow-md">
      <div className="flex items-center justify-between border-b border-idr-border/60 pb-2 mb-3">
        <div className="flex items-center gap-2">
          <GitCommit className="w-4 h-4 text-emerald-400" />
          <h2 className="text-xs font-mono font-bold uppercase tracking-wider text-slate-200">
            System Processing Pipeline Architecture
          </h2>
        </div>
        <span className="text-[10px] font-mono text-idr-textMuted">
          9-Stage Integrated Pipeline
        </span>
      </div>

      {/* Horizontal Flowchart */}
      <div className="overflow-x-auto pb-2">
        <div className="flex items-center gap-1.5 min-w-[900px]">
          {STAGES.map((stage, idx) => {
            const rawVal = status ? status[stage.id] : 'STANDBY';
            const badgeClass = getBadgeStyle(stage.id, rawVal);
            const statusLabel = getStatusText(stage.id, rawVal);

            return (
              <React.Fragment key={stage.id}>
                <div className={`flex-1 p-2.5 rounded border ${badgeClass} transition-colors min-w-[95px]`}>
                  <div className="text-[10.5px] font-mono font-bold truncate">{stage.label}</div>
                  <div className="text-[9px] text-slate-400 truncate mb-1.5">{stage.sublabel}</div>
                  <div className="text-[9px] font-mono font-semibold px-1.5 py-0.5 rounded bg-black/40 inline-block">
                    {statusLabel}
                  </div>
                </div>

                {idx < STAGES.length - 1 && (
                  <ArrowRight className="w-3.5 h-3.5 text-slate-600 flex-shrink-0" />
                )}
              </React.Fragment>
            );
          })}
        </div>
      </div>
    </div>
  );
};
