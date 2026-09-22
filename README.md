# IDR NAV — Intelligent Dead Reckoning System
### *Smartphone-Based Navigation for GNSS-Denied Environments*

[![Java](https://img.shields.io/badge/Java-17%2F19%2B-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-blue.svg)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5-blue.svg)](https://www.typescriptlang.org/)
[![Leaflet](https://img.shields.io/badge/Leaflet-1.9-green.svg)](https://leafletjs.com/)
[![License](https://img.shields.io/badge/License-MIT-lightgrey.svg)](LICENSE)

---

## 1. Project Overview & Problem Statement

In modern intelligent transportation and autonomous systems, satellite navigation (GNSS/GPS) is vulnerable to line-of-sight signal degradation and complete blackouts in:
- **Urban canyons** (skyscrapers causing extreme multipath error)
- **Subterranean tunnels and highway underpasses**
- **Multi-story indoor parking complexes**
- **Dense forest canopy or deliberate jamming/spoofing environments**

When GNSS drops, conventional navigation apps freeze or snap erratically. 

**IDR NAV** is an end-to-end full-stack navigation engineering system designed for Smart India Hackathon (SIH). It maintains continuous, smooth dead-reckoning trajectory estimation by fusing commercial smartphone IMU sensors (3-axis Accelerometer, 3-axis Gyroscope, Orientation) with sporadic GNSS fixes through an intelligent preprocessing, event detection, inertial integration, and Kalman sensor-fusion pipeline.

---

## 2. End-to-End System Architecture

```
USER / VEHICLE
      │
      ▼
SMARTPHONE SENSORS
[ Accelerometer (X, Y, Z) | Gyroscope (X, Y, Z) | Gravity/Orientation | GNSS Fixes ]
      │
      ▼
SENSOR INGESTION (REST & WebSocket /api/sensors)
      │
      ▼
TIMESTAMP SYNCHRONIZATION (Interpolation, dt computation, clamp limits)
      │
      ▼
NOISE FILTERING (Exponential smoothing low-pass filtering)
      │
      ▼
COORDINATE TRANSFORMATION (Phone-to-Vehicle alignment: Mount Euler rotation)
      │
      ▼
SENSOR WINDOW BUFFER (Temporal sliding window for ML features)
      │
      ▼
┌────────────────────────────────────────────────────────────────────────┐
│ ML EVENT DETECTION (Interface: EventDetectionService)                  │
│                                                                        │
│ Road Events: Normal, Pothole, Bump, Speed Breaker                      │
│ Vibration State: Normal, Engine Vibration, Road Vibration              │
│ Status: MOCK MODEL — TRAINED MODEL NOT INTEGRATED (Confidence: null)   │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
ABNORMAL SIGNAL HANDLING (Transient shock damping, Kalman process noise inflation)
                                    │
                                    ▼
INERTIAL DEAD RECKONING (Gravity removal, ZUPT stationary clamp, velocity integration)
                                    │
                                    ▼
KALMAN SENSOR FUSION (State [lat, lon, ve, vn], GNSS measurement correction)
                                    │
                                    ▼
MAP MATCHING (Candidate road polyline geometric projection)
                                    │
                                    ▼
NAVIGATION STATE OUTPUT & TELEMETRY STREAM
                                    │
                                    ▼
FRONTEND TECHNICAL DASHBOARD (React + Leaflet + Real-time Oscilloscope + Live Event Log)
```

---

## 3. Strict Engineering & ML Integrity Policy

> [!IMPORTANT]
> **Trained ML Model Status**:
> Because trained deep learning weights are not yet integrated into this prototype, the event detection service is explicitly implemented as `MockMLModelService` behind the `EventDetectionService` interface.
> - **In Code and UI**: Tagged with **"MOCK MODEL — TRAINED MODEL NOT INTEGRATED"**.
> - **Confidence**: Kept strictly `null` (never fabricated).
> - **No Fake Metrics**: No benchmark accuracy, fake drift percentages, or artificial dataset statistics are invented.
> - **Deterministic Heuristic**: The mock model detects vertical acceleration jerks deterministically to test downstream shock damping and Kalman noise adaptation.

---

## 4. Technology Stack

### Backend
- **Framework**: Spring Boot 3.2.5 (Java 17 / 19 / 21+)
- **Architecture**: Clean layered architecture (`controller`, `service`, `repository`, `dto`, `entity`, `sensor`, `ml`, `navigation`, `fusion`, `mapping`, `simulation`, `exception`)
- **Real-Time Streaming**: Spring WebSocket with STOMP message broker (`/topic/telemetry`, `/topic/events`)
- **Persistence**: Spring Data JPA with H2 (out-of-the-box zero setup) and PostgreSQL profile
- **Build System**: Apache Maven

### Frontend
- **Framework**: React 18 with Vite and TypeScript
- **Styling**: Tailwind CSS (Dark technical instrumentation theme)
- **Map Engine**: Leaflet with dark OpenStreetMap / CartoDB tiles, directional vehicle marker, and multi-trace paths
- **Sensors**: W3C Geolocation API, DeviceMotionEvent, DeviceOrientationEvent
- **Oscilloscope**: High-performance HTML5 Canvas real-time multi-channel strip chart

---

## 5. Directory Structure

```
idr-nav/
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/idr/nav/
│       │   │   ├── IdrNavApplication.java
│       │   │   ├── config/          # CORS & WebSocket STOMP broker
│       │   │   ├── controller/      # REST API Controllers
│       │   │   ├── dto/             # Clean Data Transfer Objects
│       │   │   ├── entity/          # JPA Persistence Entities
│       │   │   ├── exception/       # Global Exception Handler
│       │   │   ├── fusion/          # Kalman Filter Service & Covariance
│       │   │   ├── mapping/         # Road Matching & Snapping
│       │   │   ├── ml/              # ML Interface & Mock Implementation
│       │   │   ├── navigation/      # Dead Reckoning Engine & Geodetic math
│       │   │   ├── repository/      # Spring Data Repositories
│       │   │   ├── sensor/          # Ingestion, Alignment, Noise Filter, Sync
│       │   │   └── simulation/      # Kinematic Sensor Simulator
│       │   └── resources/
│       │       ├── application.yml          # Dev embedded database (H2)
│       │       ├── application-postgres.yml # Production PostgreSQL profile
│       │       └── schema.sql               # Database schema
│       └── test/                            # Comprehensive unit & integration tests
├── frontend/
│   ├── package.json
│   ├── vite.config.ts
│   ├── tailwind.config.js
│   ├── src/
│   │   ├── App.tsx
│   │   ├── components/
│   │   │   ├── Header.tsx               # Status badges & mock model banner
│   │   │   ├── LiveMap.tsx              # Leaflet OSM map & multi-trace paths
│   │   │   ├── GnssStatusCard.tsx       # Satellite reception & outage timer
│   │   │   ├── NavigationStateCard.tsx  # Position, speed, heading readout
│   │   │   ├── SensorMonitoringCard.tsx # 3D IMU & Canvas oscilloscope
│   │   │   ├── MlEventPanel.tsx         # Road & vibration anomaly panel
│   │   │   ├── PipelineVisualizer.tsx   # 9-stage pipeline status monitor
│   │   │   ├── GnssControls.tsx         # GNSS ON / LOSS / RESTORE triggers
│   │   │   ├── SessionControls.tsx      # Start / Pause / Stop / Reset
│   │   │   ├── EventLogConsole.tsx      # Monospace live event stream
│   │   │   └── MobileSensorModal.tsx    # Smartphone sensor hub
│   │   ├── services/                    # REST API, WebSocket & DeviceMotion
│   │   └── types/                       # TypeScript interfaces
└── README.md
```

---

## 6. Getting Started & Running the System

### Prerequisites
- Java JDK 17, 19, or 21+ installed (`java -version`)
- Apache Maven 3.8+ (`mvn -v`)
- Node.js 18+ and npm (`node -v`, `npm -v`)

### Step 1: Start Backend (Spring Boot)
```bash
cd backend
# Compiles, runs automated tests, and starts the server on port 8080
mvn spring-boot:run
```
*Note: Backend will start on `http://localhost:8080` with embedded H2 database enabled.*

### Step 2: Start Frontend (React + Vite)
```bash
cd frontend
npm install
npm run dev
```
*Frontend will start on `http://localhost:5173`.*

---

## 7. PostgreSQL Setup (Optional Production Persistence)

By default, the application runs immediately using H2 in-memory storage. To persist sessions and telemetry into PostgreSQL:
1. Create database:
   ```sql
   CREATE DATABASE idr_nav;
   ```
2. Run backend with `postgres` Spring profile:
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=postgres \
     -Dspring-boot.run.arguments="--SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/idr_nav --SPRING_DATASOURCE_USERNAME=postgres --SPRING_DATASOURCE_PASSWORD=yourpassword"
   ```

---

## 8. Step-by-Step Demonstration Guide

### Demonstration Flow (Simulation Mode)
1. Open `http://localhost:5173` in your browser.
2. Verify that:
   - Header indicates `🟢 TELEMETRY LIVE`.
   - Navigation Mode reads `KALMAN FUSION ACTIVE`.
   - Banner displays `MODEL: MOCK (TRAINED MODEL NOT INTEGRATED)`.
3. Click **`Start Navigation`** (Simulation Mode).
4. Observe:
   - Vehicle advances smoothly along the urban route on the Leaflet map.
   - Live Canvas oscilloscope displays incoming 3-axis accelerometer waveforms.
   - Velocity, Heading, and Coordinates update dynamically in the telemetry cards.
   - Processing Pipeline displays all 9 stages active.
5. Click **`[ GNSS LOSS ]`**:
   - Header badge switches to `⚠️ DEAD RECKONING (GNSS DENIED)`.
   - GNSS Card alerts: `GNSS SIGNAL OUTAGE DETECTED` with outage timestamp.
   - Leaflet map places a red `⚠️ GNSS LOST` marker.
   - Amber dashed line continues drawing the estimated trajectory purely from inertial integration.
   - Event Log records: `[GNSS_LOST] GNSS SIGNAL LOST! Switched to Dead Reckoning inertial navigation.`
6. Click **`[ GNSS RESTORE ]`**:
   - Navigation mode transitions back to `KALMAN FUSION ACTIVE`.
   - Leaflet map places a green `📡 GNSS RESTORED` marker.
   - Kalman filter incorporates new GPS measurement and corrects dead reckoning drift.
   - Event Log records: `[GNSS_RESTORED] GNSS Signal Restored. Kalman sensor fusion re-engaged.`

---

## 9. Real Smartphone Sensor Mode

To collect and navigate using physical smartphone IMU and GPS sensors:
1. Ensure your laptop and smartphone are connected to the same Wi-Fi network.
2. Open `http://<your-laptop-ip>:5173` on your smartphone browser (Chrome on Android or Safari on iOS).
3. Toggle mode to **`Real Sensor Mode`** and tap **`Mobile Sensor Hub`**.
4. Tap **`Grant Device Sensor Permissions`** (Required on iOS for DeviceMotionEvent).
5. Tap **`Start Streaming Telemetry to Server`**.
6. Move the smartphone (tilt, turn, accelerate): the main dashboard updates live with hardware IMU forces and GPS fixes!

---

## 10. How to Replace Mock ML Model with a Trained Deep Learning Model

The system was intentionally architected for clean separation. To integrate a trained model (e.g. 1D-CNN, ResNet, or LSTM trained on PyTorch/TensorFlow):

1. **Keep the Interface**:
   ```java
   package com.idr.nav.ml;

   public interface EventDetectionService {
       MLPredictionDTO predict(SensorWindow window);
   }
   ```

2. **Add Dependencies** (e.g., ONNX Runtime for Java in `backend/pom.xml`):
   ```xml
   <dependency>
       <groupId>com.microsoft.onnxruntime</groupId>
       <artifactId>onnxruntime</artifactId>
       <version>1.17.1</version>
   </dependency>
   ```

3. **Implement `TrainedMLModelService`**:
   ```java
   package com.idr.nav.ml;

   import org.springframework.context.annotation.Primary;
   import org.springframework.stereotype.Service;

   @Service
   @Primary // Replaces MockMLModelService automatically
   public class TrainedMLModelService implements EventDetectionService {

       private final OrtSession session;

       public TrainedMLModelService() {
           // Load exported ONNX model weights
           OrtEnvironment env = OrtEnvironment.getEnvironment();
           this.session = env.createSession("models/road_classifier.onnx", new OrtSession.SessionOptions());
       }

       @Override
       public MLPredictionDTO predict(SensorWindow window) {
           // 1. Extract feature tensor from window (N samples x 6 channels)
           // 2. Run session.run(...)
           // 3. Return MLPredictionDTO with actual model predictions
       }
   }
   ```
4. No changes are required in `DeadReckoningEngine`, `KalmanFilterService`, or `SensorIngestionService`!

---

## 11. REST API Specification

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/system/status` | System health, active session ID, and mock ML status |
| `POST` | `/api/navigation/session/start` | Starts new navigation session |
| `POST` | `/api/navigation/session/pause` | Pauses active session |
| `POST` | `/api/navigation/session/stop` | Terminates active session and records stop time |
| `POST` | `/api/navigation/session/reset` | Resets telemetry buffers and map polylines |
| `POST` | `/api/navigation/gnss/loss` | Triggers GNSS outage and activates Dead Reckoning |
| `POST` | `/api/navigation/gnss/restore` | Restores GNSS fix and resumes Kalman fusion |
| `GET` | `/api/navigation/state` | Retrieves current fused navigation state |
| `GET` | `/api/navigation/trajectory` | Retrieves historical trajectory point array |
| `GET` | `/api/navigation/events` | Retrieves recent system event log |
| `GET` | `/api/navigation/pipeline` | Retrieves current status of all 9 pipeline stages |
| `POST` | `/api/sensors/imu` | Ingests raw/mobile IMU sample |
| `POST` | `/api/sensors/gnss` | Ingests raw/mobile GNSS fix |
| `POST` | `/api/simulation/start` | Starts background vehicle kinematic simulation |
| `POST` | `/api/simulation/stop` | Stops background simulation |

### WebSocket Telemetry Endpoint
- **Broker Endpoint**: `ws://localhost:8080/ws-telemetry` (with SockJS fallback)
- **Topic Subscriptions**:
  - `/topic/telemetry`: Live `NavigationStateDTO` at 10 Hz
  - `/topic/events`: Live `SystemEventDTO` on mode transitions and anomalies

---

## 12. Engineering Assumptions & Limitations

1. **Smartphone Accelerometer Bias**: Smartphone MEMS sensors have inherent zero-bias drift. Without wheel tick odometry or zero-velocity updates (ZUPT), pure double integration will drift over prolonged periods.
2. **Mount Orientation**: The coordinate transformation layer assumes a typical windshield or console mount. Dynamic orientation compensation requires active device gravity vector tracking.
3. **Map Matching**: Baseline implementation provides geometric perpendicular snapping to candidate corridors. Real-world lane-level matching requires an integrated routing network graph.

---

## 13. License
Developed for Smart India Hackathon (SIH). Open-source under the MIT License.
