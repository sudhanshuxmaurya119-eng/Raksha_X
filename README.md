# RakshaX

RakshaX is a native Android personal-safety app that connects an ESP32-C3 emergency button over BLE with live location, trusted-contact escalation, AuroraSafe intelligence, and emergency notifications.

The Android project is in [`android/`](android/). The detailed phase roadmap, setup instructions, and demo flow are in [`android/README.md`](android/README.md).

## Build

From the `android` directory:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

The repository intentionally excludes local secrets, Firebase configuration, Google Maps keys, Gradle caches, IDE metadata, and generated build output.
