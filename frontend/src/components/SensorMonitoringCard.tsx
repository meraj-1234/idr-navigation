import React, { useEffect, useRef } from 'react';
import { Activity } from 'lucide-react';
import { IMUSample } from '../types/navigation';

interface SensorMonitoringCardProps {
  latestImu?: IMUSample | null;
}

export const SensorMonitoringCard: React.FC<SensorMonitoringCardProps> = ({ latestImu }) => {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const historyRef = useRef<{ ax: number; ay: number; az: number }[]>([]);

  // Update Canvas Waveform Strip Chart
  useEffect(() => {
    if (!latestImu) return;

    // Buffer latest 80 samples for the oscilloscope view
    const history = historyRef.current;
    history.push({
      ax: latestImu.accelerometerX,
      ay: latestImu.accelerometerY,
      az: latestImu.accelerometerZ,
    });
    if (history.length > 90) {
      history.shift();
    }

    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const width = canvas.width;
    const height = canvas.height;

    // Clear background
    ctx.fillStyle = '#090d16';
    ctx.fillRect(0, 0, width, height);

    // Draw center reference line
    ctx.strokeStyle = '#1e293b';
    ctx.lineWidth = 1;
    ctx.setLineDash([2, 2]);
    ctx.beginPath();
    ctx.moveTo(0, height / 2);
    ctx.lineTo(width, height / 2);
    ctx.stroke();
    ctx.setLineDash([]);

    if (history.length < 2) return;

    const stepX = width / 90;
    const centerY = height / 2;
    const scale = 2.4; // pixel scaling per m/s^2

    // Helper to draw a single channel curve
    const drawChannel = (key: 'ax' | 'ay' | 'az', color: string) => {
      ctx.strokeStyle = color;
      ctx.lineWidth = 1.5;
      ctx.beginPath();
      for (let i = 0; i < history.length; i++) {
        const x = i * stepX;
        // Az has gravity baseline around 9.8, offset so it fits canvas
        const val = key === 'az' ? history[i][key] - 9.80665 : history[i][key];
        const y = centerY - val * scale;
        if (i === 0) ctx.moveTo(x, y);
        else ctx.lineTo(x, y);
      }
      ctx.stroke();
    };

    drawChannel('ax', '#38bdf8'); // X: Cyan
    drawChannel('ay', '#a855f7'); // Y: Purple
    drawChannel('az', '#34d399'); // Z: Green
  }, [latestImu]);

  const ax = latestImu ? latestImu.accelerometerX.toFixed(2) : '0.00';
  const ay = latestImu ? latestImu.accelerometerY.toFixed(2) : '0.00';
  const az = latestImu ? latestImu.accelerometerZ.toFixed(2) : '0.00';
  const mag = latestImu
    ? Math.sqrt(
        latestImu.accelerometerX ** 2 +
          latestImu.accelerometerY ** 2 +
          latestImu.accelerometerZ ** 2
      ).toFixed(2)
    : '0.00';

  const gx = latestImu ? latestImu.gyroscopeX.toFixed(3) : '0.000';
  const gy = latestImu ? latestImu.gyroscopeY.toFixed(3) : '0.000';
  const gz = latestImu ? latestImu.gyroscopeZ.toFixed(3) : '0.000';

  const pitch = latestImu?.beta !== null && latestImu?.beta !== undefined ? `${latestImu.beta.toFixed(1)}°` : 'N/A';
  const roll = latestImu?.gamma !== null && latestImu?.gamma !== undefined ? `${latestImu.gamma.toFixed(1)}°` : 'N/A';
  const yaw = latestImu?.alpha !== null && latestImu?.alpha !== undefined ? `${latestImu.alpha.toFixed(1)}°` : 'N/A';

  return (
    <div className="bg-idr-panel border border-idr-border rounded-lg p-4 shadow-md">
      <div className="flex items-center justify-between border-b border-idr-border/60 pb-2 mb-3">
        <div className="flex items-center gap-2">
          <Activity className="w-4 h-4 text-cyan-400" />
          <h2 className="text-xs font-mono font-bold uppercase tracking-wider text-slate-200">
            Live Inertial Telemetry (IMU)
          </h2>
        </div>
        <div className="flex items-center gap-3 text-[10px] font-mono">
          <span className="flex items-center gap-1 text-sky-400">
            <span className="w-2 h-0.5 bg-sky-400" /> X
          </span>
          <span className="flex items-center gap-1 text-purple-400">
            <span className="w-2 h-0.5 bg-purple-400" /> Y
          </span>
          <span className="flex items-center gap-1 text-emerald-400">
            <span className="w-2 h-0.5 bg-emerald-400" /> Z (dyn)
          </span>
        </div>
      </div>

      {/* Real-time Oscilloscope Strip Chart */}
      <div className="mb-3 rounded overflow-hidden border border-idr-border/50">
        <canvas ref={canvasRef} width={420} height={70} className="w-full h-[70px] block" />
      </div>

      {/* Numerical Sensor Readouts */}
      <div className="grid grid-cols-3 gap-2 text-xs font-mono">
        {/* Accelerometer */}
        <div className="bg-slate-900 p-2 rounded border border-idr-border/40">
          <span className="text-[10px] text-idr-textMuted block border-b border-slate-800 pb-1 mb-1 font-bold">
            ACCEL (m/s²)
          </span>
          <div className="space-y-0.5 text-[11px]">
            <div className="flex justify-between">
              <span className="text-slate-400">X:</span>
              <span className="text-sky-400">{ax}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-400">Y:</span>
              <span className="text-purple-400">{ay}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-400">Z:</span>
              <span className="text-emerald-400">{az}</span>
            </div>
            <div className="flex justify-between border-t border-slate-800 pt-0.5">
              <span className="text-slate-500">|A|:</span>
              <span className="text-slate-200 font-bold">{mag}</span>
            </div>
          </div>
        </div>

        {/* Gyroscope */}
        <div className="bg-slate-900 p-2 rounded border border-idr-border/40">
          <span className="text-[10px] text-idr-textMuted block border-b border-slate-800 pb-1 mb-1 font-bold">
            GYRO (rad/s)
          </span>
          <div className="space-y-0.5 text-[11px]">
            <div className="flex justify-between">
              <span className="text-slate-400">ωX:</span>
              <span className="text-slate-200">{gx}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-400">ωY:</span>
              <span className="text-slate-200">{gy}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-400">ωZ:</span>
              <span className="text-slate-200">{gz}</span>
            </div>
          </div>
        </div>

        {/* Orientation */}
        <div className="bg-slate-900 p-2 rounded border border-idr-border/40">
          <span className="text-[10px] text-idr-textMuted block border-b border-slate-800 pb-1 mb-1 font-bold">
            ORIENTATION
          </span>
          <div className="space-y-0.5 text-[11px]">
            <div className="flex justify-between">
              <span className="text-slate-400">Pitch:</span>
              <span className="text-slate-200">{pitch}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-400">Roll:</span>
              <span className="text-slate-200">{roll}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-400">Yaw:</span>
              <span className="text-slate-200">{yaw}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
