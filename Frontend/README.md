`# RakshaX Android Architecture & Phase Implementation Roadmap

## 📌 Project Overview
**RakshaX** is an end-to-end personal safety ecosystem connecting a physical **ESP32-C3** hardware panic button via Bluetooth Low Energy (BLE) to this native Android application and integrating with the **RakshaX FastAPI** safety intelligence backend.

---

## 🚀 Phase-by-Phase Roadmap

### [x] Phase 0 — Existing Backend Audit & Reuse Plan
* Audited FastAPI backend (`models`, `ml`, `routers`, `schemas`, `services`).
* Identified direct reuse of XGBoost risk models, DBSCAN clustering, OSRM safe route A* heuristics, and emergency NGO directory.
* Established token-efficient architecture avoiding duplicate boilerplate.

---

### [x] Phase 1 — RakshaX Android Frontend UI (Kotlin + Jetpack Compose)
* **Design System:** Safety-focused dark palette (`#0A0F1D`) with high-contrast emergency crimson (`#EF4444`), safe green (`#10B981`), and amber warning accents.
* **Core Screens Built:**
  1. **Splash Screen:** Animated RakshaX shield branding, AuroraSafe status pill, auto-navigation.
  2. **Onboarding Screen:** 3 slides covering Physical SOS Button, Location-Aware Safety, and Priority Escalation with 112 dialing.
  3. **Auth Screen:** Seamless Sign In / Register tabs, password toggles, and Hackathon Demo bypass.
  4. **Home Dashboard:**
     * Prominent animated pulsing SOS button.
     * Direct **Call 112** national emergency button.
     * AuroraSafe risk score index & safety zone badge.
     * Live hardware BLE status strip.
     * Quick shortcuts to Safety Map & Trusted Contacts.
  5. **Device Screen:** Real-time ESP32-C3 hardware telemetry (Battery, RSSI, MAC, GPIO4 configuration, Connect/Disconnect controls).
  6. **Safety Map Screen:** Google Maps architecture mockup with live layer toggles for DBSCAN clusters, safety heatmap, and A* safe route comparison.
  7. **Trusted Contacts Screen:** Priority management (P1 Immediate, P2 Secondary, P3 Fallback), enable/disable switches, contact CRUD dialogs.
  8. **SOS Status Screen:** Live emergency pipeline tracking (Button detected -> Location found -> Alert sent -> Contact acknowledged -> Escalation paused).
  9. **Profile & Settings Screen:** Escalation timeout configuration, siren toggles, background permissions status, and mandatory legal safety disclaimer.

---

### [x] Phase 2 — Android Location Services
* `FusedLocationProviderClient` integration.
* Runtime permissions flow (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`).
* Honest status reporting (GPS disabled / poor accuracy handling).
* SOS location object payload (`lat`, `lng`, `accuracy`, `timestamp`).

---

### [x] Phase 3 — ESP32-C3 BLE Communication
* Arduino / ESP-IDF firmware (`esp32/rakshax_button.ino`) running on ESP32-C3.
* Push button wired to GPIO4 -> GND with hardware debounce and 2-second long-press threshold.
* BLE GATT Service & SOS Characteristic with Notify enabled.
* Android BLE client scanning, auto-reconnecting, and receiving the SOS byte string.

Implementation notes:
* Android runtime Bluetooth permissions are requested on the Device screen for Android 12+.
* The BLE client filters for the RakshaX service, connects to the selected device, enables SOS notifications, and retries a dropped connection.
* Location is requested when the user enables it or triggers SOS. An SOS remains visible when no fix is available instead of reporting a fabricated coordinate.

---

### [x] Phase 4 — Background & Locked-Phone SOS Service
* Android Foreground Service (`BleSosListenerService`) with sticky notification.
* `PARTIAL_WAKE_LOCK` for screen-off event processing.
* Battery optimization exclusion guidance.
* Immediate emergency trigger processing when screen is locked.

Implementation notes:
* Enable **Background Foreground Service** from Settings to keep the ESP32 listener alive after the app is closed.
* The service restores the last paired device, runs as `START_STICKY`, and processes BLE SOS notifications while the activity is not visible.
* A short partial wake lock protects the SOS location capture and state update, then releases automatically.
* Battery optimization guidance is available in Settings; Android notification permission is requested before enabling the listener.

---

### [x] Phase 5 — RakshaX Backend Integration
* Retrofit / OkHttp client communicating with the RakshaX FastAPI endpoints.
* Endpoints: `POST /sos/trigger`, `GET /sos/{id}/status`, `POST /sos/{id}/acknowledge`, `GET /safety/risk-score`.
* Network resilience, timeout handling, and offline queuing.

---

### [x] Phase 6 — Firebase Cloud Messaging (FCM)
* Firebase token registration.
* FCM payload dispatch from FastAPI backend to trusted contact devices.
* High-priority heads-up emergency notification rendering.

---

### [x] Phase 7 — Trusted Contact Acknowledgement Flow
* In-notification and in-app acknowledgement action for trusted contacts.
* Bidirectional status sync updating the original victim's phone in real-time.

---

### [x] Phase 8 — Multi-Stage Priority Escalation Engine
* Sequential notification algorithm:
  * Stage 1: Notify Priority 1 contact.
  * Timeout (e.g. 45 seconds) without acknowledgement -> Trigger Priority 2.
  * Timeout -> Trigger Priority 3.
  * Stop escalation immediately upon any valid acknowledgement.

---

### [x] Phase 9 — RakshaX Intelligence Live Sync
* Real-time sync of XGBoost predictive risk scores.
* Dynamic DBSCAN hotspot markers loaded around user's active coordinates.

---

### [x] Phase 10 & 11 — Google Maps SDK & Safe Routing
* Native Google Maps SDK map view.
* Normal vs. A* Safer Route polyline comparison.

---

### [x] Phase 12 — Failure and Safety Handling
* Honest error reporting for Bluetooth off, GPS unavailable, or network loss.
* Accidental press prevention.

---

### [x] Phase 13 — Security & Data Privacy
* JWT authentication validation.
* Sensitive location data protection.

---

### [x] Phase 14 & 15 — End-to-End Testing & Hackathon Demo Mode
* Full live demo walkthrough from physical ESP32 button click to contact acknowledgement.

Implementation notes:
* OkHttp uses `AURORA_SAFE_BASE_URL` from `Frontend/local.properties` and retries failed SOS posts through an encrypted queue.
* FCM registration, high-priority SOS data notifications, notification acknowledgement, and backend status polling are implemented. Configure the Firebase Android app before testing live delivery.
* `SosEscalationService` keeps the 45-second P1 -> P2 -> P3 timer alive when the activity is backgrounded, and stops immediately on acknowledgement or resolve.
* The Safety Map syncs risk scores, nearby incidents, DBSCAN hotspot zones, and AuroraSafe route waypoints. A native Google `MapView` is used when `MAPS_API_KEY` is present; the cached visualizer remains available without it.
* GPS, BLE, and network failures are reported honestly. In-app SOS requires explicit confirmation, while the physical ESP32 path keeps its two-second long-press threshold.
* Android Keystore AES/GCM protects JWTs and queued location payloads. AuroraSafe validates bearer JWTs for push-token registration and release builds disable cleartext traffic.

Verification and demo flow:
1. Run the RakshaX backend from `../Backend`, then set `AURORA_SAFE_BASE_URL` and optionally `MAPS_API_KEY` in `Frontend/local.properties`.
2. Run `./gradlew :app:testDebugUnitTest :app:assembleDebug`.
3. Enable the background listener, pair the ESP32-C3, and trigger the two-second hardware press.
4. Confirm the live SOS screen, backend event, FCM notification, acknowledgement action, and escalation stop.
5. Without backend or Firebase configuration, use `TEST SOS PIPELINE` to demonstrate local escalation and the cached safety map.

The automated checks cover BLE signal parsing and escalation policy transitions. Physical BLE, GPS, FCM delivery, Google Maps rendering, and the full FastAPI/database path still require configured services or a connected device.

## Integration Setup

Android reads these optional values from `Frontend/local.properties`:

```properties
AURORA_SAFE_BASE_URL=http://10.0.2.2:8000
MAPS_API_KEY=your_google_maps_key
```

For live FCM, configure the Android Firebase application and set `FIREBASE_SERVICE_ACCOUNT_JSON` in `Backend/.env`. Do not commit credential files or secrets.
