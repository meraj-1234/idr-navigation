import React, { useEffect, useRef } from 'react';
import L from 'leaflet';
import { NavigationState, RoutePoint } from '../types/navigation';
import { Maximize2, Crosshair, Layers } from 'lucide-react';

interface LiveMapProps {
  currentState: NavigationState;
  trajectory: NavigationState[];
  gnssLossEvents: { lat: number; lon: number; timestamp: number }[];
  gnssRestoreEvents: { lat: number; lon: number; timestamp: number }[];
  // Route-based navigation props
  routePoints?: RoutePoint[];
  routeFromName?: string;
  routeToName?: string;
}

export const LiveMap: React.FC<LiveMapProps> = ({
  currentState,
  trajectory,
  gnssLossEvents,
  gnssRestoreEvents,
  routePoints,
  routeFromName,
  routeToName,
}) => {
  const mapContainerRef = useRef<HTMLDivElement>(null);
  const mapInstanceRef  = useRef<L.Map | null>(null);
  const vehicleMarkerRef = useRef<L.Marker | null>(null);

  // Trajectory polyline layer refs
  const fusedPolylineRef  = useRef<L.Polyline | null>(null);
  const drPolylineRef     = useRef<L.Polyline | null>(null);
  const gnssPolylineRef   = useRef<L.Polyline | null>(null);
  const lossMarkersLayerRef = useRef<L.LayerGroup | null>(null);

  // Route reference polyline + pin markers
  const routePolylineRef  = useRef<L.Polyline | null>(null);
  const startMarkerRef    = useRef<L.Marker | null>(null);
  const endMarkerRef      = useRef<L.Marker | null>(null);

  const isAutoCentered = useRef<boolean>(true);
  const lastFittedRouteKey = useRef<string>('');
  const animFrameRef = useRef<number | null>(null);
  const animFromRef = useRef<[number, number] | null>(null);
  const animToRef = useRef<[number, number] | null>(null);
  const animStartRef = useRef<number>(0);
  const lastGnssIconRef = useRef<boolean | null>(null);

  const gnssOn = currentState.gnssAvailable;
  const positionSourceLabel = gnssOn ? 'GNSS' : 'DEAD RECKONING / PREDICTED';

  const buildVehicleIcon = (available: boolean) => {
    const fill = available ? '#2563eb' : '#d97706';
    const ring = available ? '#60a5fa' : '#fbbf24';
    const glow = available ? 'rgba(37,99,235,0.8)' : 'rgba(217,119,6,0.8)';
    return L.divIcon({
      className: 'vehicle-marker-container',
      html: `
        <div id="vehicle-pointer" style="width: 32px; height: 32px; display: flex; align-items: center; justify-content: center; transform: rotate(0deg); transition: transform 0.08s linear;">
          <div style="width: 28px; height: 28px; background: ${fill}; border: 2.5px solid ${ring}; border-radius: 50%; box-shadow: 0 0 14px ${glow}; display: flex; align-items: center; justify-content: center;">
            <div style="width: 0; height: 0; border-left: 5px solid transparent; border-right: 5px solid transparent; border-bottom: 12px solid #ffffff; margin-bottom: 2px;"></div>
          </div>
        </div>
      `,
      iconSize: [32, 32],
      iconAnchor: [16, 16],
    });
  };

  // 1. Initialize Leaflet Map
  useEffect(() => {
    if (!mapContainerRef.current || mapInstanceRef.current) return;

    const initialLat = currentState.latitude || 28.6139;
    const initialLon = currentState.longitude || 77.2090;

    const map = L.map(mapContainerRef.current, {
      center: [initialLat, initialLon],
      zoom: 16,
      zoomControl: true,
      attributionControl: false,
    });

    // Dark technical CartoDB tiles
    L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
      maxZoom: 19,
      subdomains: 'abcd',
    }).addTo(map);

    // Selected route polyline — solid blue, drawn below live traces
    routePolylineRef.current = L.polyline([], {
      color: '#2563eb',
      weight: 6,
      opacity: 0.95,
      lineCap: 'round',
      lineJoin: 'round',
    }).addTo(map);

    // Live trajectory polylines drawn on top
    // Emerald green for Kalman Fused path
    fusedPolylineRef.current = L.polyline([], {
      color: '#10b981',
      weight: 4,
      opacity: 0.9,
      lineCap: 'round',
      lineJoin: 'round',
    }).addTo(map);

    // Amber/Orange for Pure Dead Reckoning path
    drPolylineRef.current = L.polyline([], {
      color: '#f59e0b',
      weight: 3.5,
      opacity: 0.85,
      dashArray: '6, 6',
      lineCap: 'round',
      lineJoin: 'round',
    }).addTo(map);

    // Cyan/Blue for raw GNSS path
    gnssPolylineRef.current = L.polyline([], {
      color: '#38bdf8',
      weight: 2.5,
      opacity: 0.6,
      dashArray: '2, 4',
    }).addTo(map);

    lossMarkersLayerRef.current = L.layerGroup().addTo(map);

    vehicleMarkerRef.current = L.marker([initialLat, initialLon], {
      icon: buildVehicleIcon(true),
    }).addTo(map);

    mapInstanceRef.current = map;

    return () => {
      map.remove();
      mapInstanceRef.current = null;
    };
  }, []);

  // 2. Update Vehicle Position & Heading (interpolate between telemetry frames)
  useEffect(() => {
    if (!mapInstanceRef.current || !vehicleMarkerRef.current) return;

    let lat = currentState.latitude;
    let lon = currentState.longitude;

    if (!currentState.gnssAvailable && currentState.deadReckoningLatitude && currentState.deadReckoningLongitude) {
      lat = currentState.deadReckoningLatitude;
      lon = currentState.deadReckoningLongitude;
    }

    const pinToStart = currentState.routeNavigationStatus === 'ROUTE_SELECTED'
      || (!currentState.routeNavigationStatus && routePoints && routePoints.length > 0 && (currentState.velocity ?? 0) === 0);
    if (pinToStart && routePoints && routePoints.length > 0) {
      lat = routePoints[0].lat;
      lon = routePoints[0].lon;
    }

    if (lat == null || lon == null) return;

    if (lastGnssIconRef.current !== currentState.gnssAvailable) {
      vehicleMarkerRef.current.setIcon(buildVehicleIcon(currentState.gnssAvailable));
      lastGnssIconRef.current = currentState.gnssAvailable;
    }

    const heading = currentState.heading ?? 0;
    const pointer = document.getElementById('vehicle-pointer');
    if (pointer) {
      pointer.style.transform = `rotate(${heading}deg)`;
    }

    const current = vehicleMarkerRef.current.getLatLng();
    animFromRef.current = [current.lat, current.lng];
    animToRef.current = [lat, lon];
    animStartRef.current = performance.now();

    if (animFrameRef.current != null) {
      cancelAnimationFrame(animFrameRef.current);
    }

    const durationMs = 120;
    const step = (now: number) => {
      if (!vehicleMarkerRef.current || !animFromRef.current || !animToRef.current) return;
      const t = Math.min(1, (now - animStartRef.current) / durationMs);
      const eased = t * t * (3 - 2 * t);
      const nlat = animFromRef.current[0] + (animToRef.current[0] - animFromRef.current[0]) * eased;
      const nlon = animFromRef.current[1] + (animToRef.current[1] - animFromRef.current[1]) * eased;
      vehicleMarkerRef.current.setLatLng([nlat, nlon]);
      if (t < 1) {
        animFrameRef.current = requestAnimationFrame(step);
      } else if (mapInstanceRef.current && (isAutoCentered.current || currentState.routeNavigationStatus === 'NAVIGATION_ACTIVE')) {
        mapInstanceRef.current.panTo([nlat, nlon], { animate: true, duration: 0.15 });
      }
    };
    animFrameRef.current = requestAnimationFrame(step);

    return () => {
      if (animFrameRef.current != null) {
        cancelAnimationFrame(animFrameRef.current);
      }
    };
  }, [
    currentState.latitude,
    currentState.longitude,
    currentState.heading,
    currentState.gnssAvailable,
    currentState.deadReckoningLatitude,
    currentState.deadReckoningLongitude,
    currentState.routeNavigationStatus,
    currentState.velocity,
    routePoints,
  ]);

  // When navigation starts, zoom in and center directly on the vehicle
  useEffect(() => {
    if (currentState.routeNavigationStatus === 'NAVIGATION_ACTIVE') {
      isAutoCentered.current = true;
      if (mapInstanceRef.current && currentState.latitude && currentState.longitude) {
        mapInstanceRef.current.setView([currentState.latitude, currentState.longitude], 17, { animate: true });
      }
    }
  }, [currentState.routeNavigationStatus]);

  // 3. Update Polylines from Trajectory History
  useEffect(() => {
    if (!fusedPolylineRef.current || !drPolylineRef.current || !gnssPolylineRef.current) return;

    const fusedCoords: [number, number][] = [];
    const drCoords:    [number, number][] = [];
    const gnssCoords:  [number, number][] = [];

    trajectory.forEach((pt) => {
      if (pt.latitude && pt.longitude) {
        fusedCoords.push([pt.latitude, pt.longitude]);
      }
      if (pt.deadReckoningLatitude && pt.deadReckoningLongitude) {
        drCoords.push([pt.deadReckoningLatitude, pt.deadReckoningLongitude]);
      }
      if (pt.gnssLatitude && pt.gnssLongitude) {
        gnssCoords.push([pt.gnssLatitude, pt.gnssLongitude]);
      }
    });

    fusedPolylineRef.current.setLatLngs(fusedCoords);
    drPolylineRef.current.setLatLngs(drCoords);
    gnssPolylineRef.current.setLatLngs(gnssCoords);
  }, [trajectory]);

  // 4. Update Reference Route Polyline and A/B pins
  useEffect(() => {
    if (!mapInstanceRef.current || !routePolylineRef.current) return;

    // Remove old start/end markers
    if (startMarkerRef.current) {
      startMarkerRef.current.remove();
      startMarkerRef.current = null;
    }
    if (endMarkerRef.current) {
      endMarkerRef.current.remove();
      endMarkerRef.current = null;
    }

    if (!routePoints || routePoints.length === 0) {
      routePolylineRef.current.setLatLngs([]);
      return;
    }

    const coords: [number, number][] = routePoints.map(p => [p.lat, p.lon]);
    routePolylineRef.current.setLatLngs(coords);

    // A - Start pin
    const startPt = routePoints[0];
    const startIcon = L.divIcon({
      className: '',
      html: `<div style="background:#059669;color:white;border-radius:50%;width:26px;height:26px;display:flex;align-items:center;justify-content:center;font-size:12px;font-weight:bold;border:2px solid #6ee7b7;box-shadow:0 0 8px rgba(5,150,105,0.7);">A</div>`,
      iconSize: [26, 26],
      iconAnchor: [13, 13],
    });
    const startMarker = L.marker([startPt.lat, startPt.lon], { icon: startIcon }).addTo(mapInstanceRef.current);
    if (routeFromName) startMarker.bindPopup(`<b>Start:</b> ${routeFromName}`);
    startMarkerRef.current = startMarker;

    // B - End pin
    const endPt = routePoints[routePoints.length - 1];
    const endIcon = L.divIcon({
      className: '',
      html: `<div style="background:#e11d48;color:white;border-radius:50%;width:26px;height:26px;display:flex;align-items:center;justify-content:center;font-size:12px;font-weight:bold;border:2px solid #fda4af;box-shadow:0 0 8px rgba(225,29,72,0.7);">B</div>`,
      iconSize: [26, 26],
      iconAnchor: [13, 13],
    });
    const endMarker = L.marker([endPt.lat, endPt.lon], { icon: endIcon }).addTo(mapInstanceRef.current);
    if (routeToName) endMarker.bindPopup(`<b>Destination:</b> ${routeToName}`);
    endMarkerRef.current = endMarker;

    const routeKey = `${coords[0][0]},${coords[0][1]}-${coords[coords.length - 1][0]},${coords[coords.length - 1][1]}-${coords.length}`;
    if (lastFittedRouteKey.current !== routeKey) {
      const bounds = L.latLngBounds(coords);
      mapInstanceRef.current.fitBounds(bounds, { padding: [50, 50] });
      lastFittedRouteKey.current = routeKey;
      isAutoCentered.current = false;
    }

    if (vehicleMarkerRef.current && (!currentState.routeNavigationStatus || currentState.routeNavigationStatus === 'ROUTE_SELECTED')) {
      vehicleMarkerRef.current.setLatLng(coords[0]);
    }

  }, [routePoints, routeFromName, routeToName, currentState.routeNavigationStatus]);

  // 5. Update GNSS Loss & Restore Event Markers
  useEffect(() => {
    if (!lossMarkersLayerRef.current) return;
    lossMarkersLayerRef.current.clearLayers();

    // Render GNSS Loss Markers
    gnssLossEvents.forEach((ev) => {
      const lossIcon = L.divIcon({
        className: 'gnss-loss-icon',
        html: `
          <div style="background: #e11d48; color: white; border-radius: 4px; padding: 2px 5px; font-family: monospace; font-size: 10px; font-weight: bold; border: 1px solid #fda4af; box-shadow: 0 0 10px rgba(225,29,72,0.8); display: flex; align-items: center; gap: 3px; white-space: nowrap;">
            <span>⚠️ GNSS LOST</span>
          </div>
        `,
        iconSize: [80, 20],
        iconAnchor: [40, 10],
      });
      const marker = L.marker([ev.lat, ev.lon], { icon: lossIcon });
      marker.bindPopup(`<b>GNSS Signal Outage</b><br/>Time: ${new Date(ev.timestamp).toLocaleTimeString()}<br/>Mode switched to pure Dead Reckoning.`);
      lossMarkersLayerRef.current?.addLayer(marker);
    });

    // Render GNSS Restore Markers
    gnssRestoreEvents.forEach((ev) => {
      const restoreIcon = L.divIcon({
        className: 'gnss-restore-icon',
        html: `
          <div style="background: #059669; color: white; border-radius: 4px; padding: 2px 5px; font-family: monospace; font-size: 10px; font-weight: bold; border: 1px solid #6ee7b7; box-shadow: 0 0 10px rgba(5,150,105,0.8); display: flex; align-items: center; gap: 3px; white-space: nowrap;">
            <span>📡 GNSS RESTORED</span>
          </div>
        `,
        iconSize: [95, 20],
        iconAnchor: [47, 10],
      });
      const marker = L.marker([ev.lat, ev.lon], { icon: restoreIcon });
      marker.bindPopup(`<b>GNSS Signal Restored</b><br/>Time: ${new Date(ev.timestamp).toLocaleTimeString()}<br/>Kalman fusion re-engaged.`);
      lossMarkersLayerRef.current?.addLayer(marker);
    });
  }, [gnssLossEvents, gnssRestoreEvents]);

  const handleRecenter = () => {
    if (mapInstanceRef.current && currentState.latitude && currentState.longitude) {
      mapInstanceRef.current.setView([currentState.latitude, currentState.longitude], 17);
      isAutoCentered.current = true;
    }
  };

  const handleFitBounds = () => {
    if (mapInstanceRef.current && trajectory.length > 0) {
      const bounds = L.latLngBounds(trajectory.map((p) => [p.latitude, p.longitude]));
      mapInstanceRef.current.fitBounds(bounds, { padding: [40, 40] });
      isAutoCentered.current = false;
    }
  };

  return (
    <div className="relative w-full h-[520px] rounded-lg overflow-hidden border border-idr-border shadow-xl bg-slate-950">
      {/* Map DOM target */}
      <div ref={mapContainerRef} className="w-full h-full" />

      {/* Floating Map Controls */}
      <div className="absolute top-3 right-3 z-[1000] flex flex-col gap-2">
        <button
          onClick={handleRecenter}
          className="p-2 rounded bg-slate-900/90 hover:bg-slate-800 text-blue-400 border border-slate-700 shadow-md backdrop-blur-sm transition-colors"
          title="Recenter vehicle"
        >
          <Crosshair className="w-4 h-4" />
        </button>
        <button
          onClick={handleFitBounds}
          className="p-2 rounded bg-slate-900/90 hover:bg-slate-800 text-slate-300 border border-slate-700 shadow-md backdrop-blur-sm transition-colors"
          title="Fit entire trajectory"
        >
          <Maximize2 className="w-4 h-4" />
        </button>
      </div>

      {/* Live Navigation Telemetry HUD */}
      {currentState.routeNavigationStatus === 'NAVIGATION_ACTIVE' && (
        <div className="absolute top-3 left-1/2 -translate-x-1/2 z-[1000] bg-slate-900/95 border border-emerald-500/60 rounded-lg px-4 py-2 text-xs font-mono backdrop-blur-md shadow-2xl flex items-center gap-4 animate-in fade-in duration-300">
          <div className="flex items-center gap-2">
            <span className="w-2.5 h-2.5 rounded-full bg-emerald-400 animate-ping" />
            <div>
              <div className="text-[10px] text-slate-400 uppercase font-semibold">Vehicle Speed</div>
              <div className="text-lg font-bold text-emerald-400 tracking-wider">
                {((currentState.velocity ?? 0) * 3.6).toFixed(1)} <span className="text-xs text-slate-400">km/h</span>
              </div>
            </div>
          </div>
          <div className="border-l border-slate-700 pl-3">
            <div className="text-[10px] text-slate-400 uppercase font-semibold">Heading</div>
            <div className="text-sm font-bold text-sky-300">
              {Math.round(currentState.heading ?? 0)}°
            </div>
          </div>
          <div className="border-l border-slate-700 pl-3">
            <div className="text-[10px] text-slate-400 uppercase font-semibold">Mode</div>
            <div className={`text-xs font-bold ${currentState.navigationMode === 'DEAD_RECKONING' ? 'text-amber-400' : 'text-emerald-400'}`}>
              {currentState.navigationMode}
            </div>
          </div>
        </div>
      )}

      {/* GNSS / position-source overlay */}
      <div className="absolute top-3 left-3 z-[1000] bg-slate-900/92 border border-idr-border rounded px-3 py-2 text-xs font-mono backdrop-blur-sm shadow-lg space-y-1">
        <div className={`font-bold ${gnssOn ? 'text-emerald-400' : 'text-rose-400 animate-pulse'}`}>
          GNSS: {gnssOn ? 'ON' : 'OFF'}
        </div>
        <div className={gnssOn ? 'text-sky-300' : 'text-amber-300'}>
          POSITION SOURCE: {positionSourceLabel}
        </div>
      </div>

      {/* Trajectory Layers Legend */}
      <div className="absolute bottom-3 left-3 z-[1000] bg-slate-900/90 border border-idr-border rounded px-3 py-2 text-xs font-mono backdrop-blur-sm shadow-lg space-y-1.5">
        <div className="flex items-center gap-1.5 text-slate-300 font-bold border-b border-slate-800 pb-1 mb-1">
          <Layers className="w-3.5 h-3.5 text-blue-400" />
          <span>TRAJECTORY LAYERS</span>
        </div>
        {routePoints && routePoints.length > 0 && (
          <div className="flex items-center gap-2">
            <span className="w-3.5 h-1 bg-blue-500 rounded" />
            <span className="text-blue-400">Selected Route</span>
          </div>
        )}
        <div className="flex items-center gap-2">
          <span className="w-3.5 h-1 bg-emerald-500 rounded" />
          <span className="text-emerald-400">Kalman Fused State</span>
        </div>
        <div className="flex items-center gap-2">
          <span className="w-3.5 h-1 bg-amber-500 rounded border-dashed" />
          <span className="text-amber-400">Dead Reckoning (INS)</span>
        </div>
        <div className="flex items-center gap-2">
          <span className="w-3.5 h-1 bg-sky-400 rounded opacity-60" />
          <span className="text-sky-400">Raw GNSS Fixes</span>
        </div>
      </div>
    </div>
  );
};
