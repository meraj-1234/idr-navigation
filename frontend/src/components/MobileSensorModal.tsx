import React, { useState, useEffect } from 'react';
import { Smartphone, X, AlertTriangle, Wifi } from 'lucide-react';
import { DeviceSensorBroadcaster, DeviceSensorStatus } from '../services/deviceSensors';

interface MobileSensorModalProps {
  isOpen: boolean;
  onClose: () => void;
  sessionId: string;
}

export const MobileSensorModal: React.FC<MobileSensorModalProps> = ({
  isOpen,
  onClose,
  sessionId,
}) => {
  const [broadcaster, setBroadcaster] = useState<DeviceSensorBroadcaster | null>(null);
  const [status, setStatus] = useState<DeviceSensorStatus>({
    hasMotion: false,
    hasOrientation: false,
    hasGeolocation: false,
    permissionGranted: false,
    isStreaming: false,
    error: null,
  });

  useEffect(() => {
    if (isOpen && !broadcaster) {
      const b = new DeviceSensorBroadcaster((newStatus) => {
        setStatus(newStatus);
      });
      setBroadcaster(b);
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const handleRequestPermission = async () => {
    if (broadcaster) {
      await broadcaster.requestPermissions();
    }
  };

  const handleToggleStream = () => {
    if (!broadcaster) return;
    if (status.isStreaming) {
      broadcaster.stopStreaming();
    } else {
      broadcaster.startStreaming(sessionId);
    }
  };

  const hostUrl = `${window.location.protocol}//${window.location.hostname}:${window.location.port || '5173'}`;

  return (
    <div className="fixed inset-0 z-[2000] bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-idr-panel border border-idr-border rounded-xl max-w-md w-full p-6 shadow-2xl space-y-4">
        <div className="flex items-center justify-between border-b border-idr-border pb-3">
          <div className="flex items-center gap-2 text-white">
            <Smartphone className="w-5 h-5 text-blue-400" />
            <h3 className="font-mono font-bold text-sm">Physical Smartphone Sensor Transmitter</h3>
          </div>
          <button
            onClick={() => {
              if (broadcaster && status.isStreaming) {
                broadcaster.stopStreaming();
              }
              onClose();
            }}
            className="text-slate-400 hover:text-white p-1"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <p className="text-xs text-idr-textMuted leading-relaxed">
          Stream live hardware accelerometer, gyroscope, compass, and GPS fixes directly from this
          browser into the backend preprocessing and dead-reckoning pipeline.
        </p>

        {/* Local Network URL */}
        <div className="bg-slate-900 border border-idr-border p-3 rounded-lg space-y-1">
          <div className="flex items-center gap-2 text-xs font-mono text-slate-300 font-semibold">
            <Wifi className="w-3.5 h-3.5 text-blue-400" />
            <span>Connect Smartphone on Local Wi-Fi</span>
          </div>
          <p className="text-[11px] text-slate-400 font-mono select-all bg-black/50 p-1.5 rounded border border-slate-800">
            {hostUrl}
          </p>
          <span className="text-[10px] text-slate-500">
            Open the URL above in Chrome/Safari on your smartphone while connected to the same Wi-Fi.
          </span>
        </div>

        {/* Sensor Capability Status */}
        <div className="space-y-1.5 text-xs font-mono">
          <div className="flex items-center justify-between p-2 rounded bg-slate-900 border border-slate-800">
            <span className="text-slate-400">Device Motion (Accelerometer/Gyro):</span>
            <span className={status.hasMotion ? 'text-emerald-400 font-bold' : 'text-amber-500'}>
              {status.hasMotion ? 'DETECTED' : 'UNAVAILABLE'}
            </span>
          </div>

          <div className="flex items-center justify-between p-2 rounded bg-slate-900 border border-slate-800">
            <span className="text-slate-400">Orientation (Compass / Tilt):</span>
            <span className={status.hasOrientation ? 'text-emerald-400 font-bold' : 'text-amber-500'}>
              {status.hasOrientation ? 'DETECTED' : 'UNAVAILABLE'}
            </span>
          </div>

          <div className="flex items-center justify-between p-2 rounded bg-slate-900 border border-slate-800">
            <span className="text-slate-400">Browser Geolocation (GPS):</span>
            <span className={status.hasGeolocation ? 'text-emerald-400 font-bold' : 'text-rose-500'}>
              {status.hasGeolocation ? 'AVAILABLE' : 'UNSUPPORTED'}
            </span>
          </div>
        </div>

        {status.error && (
          <div className="flex items-start gap-2 p-2.5 rounded bg-rose-950/40 border border-rose-800/50 text-rose-300 text-xs font-mono">
            <AlertTriangle className="w-4 h-4 flex-shrink-0" />
            <span>{status.error}</span>
          </div>
        )}

        {/* Action Buttons */}
        <div className="space-y-2 pt-2">
          {!status.permissionGranted && (
            <button
              onClick={handleRequestPermission}
              className="w-full py-2.5 bg-blue-600 hover:bg-blue-500 text-white rounded font-mono text-xs font-semibold uppercase tracking-wider transition-colors shadow-md"
            >
              Grant Device Sensor Permissions
            </button>
          )}

          <button
            onClick={handleToggleStream}
            disabled={!status.hasMotion && !status.hasGeolocation}
            className={`w-full py-2.5 rounded font-mono text-xs font-bold uppercase tracking-wider transition-all shadow-md ${
              status.isStreaming
                ? 'bg-rose-600 hover:bg-rose-500 text-white shadow-rose-900/40'
                : 'bg-emerald-600 hover:bg-emerald-500 text-white shadow-emerald-900/40'
            }`}
          >
            {status.isStreaming ? 'Stop Streaming Sensor Telemetry' : 'Start Streaming Telemetry to Server'}
          </button>
        </div>
      </div>
    </div>
  );
};
