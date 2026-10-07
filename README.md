# RakshaX

RakshaX is a personal-safety platform that connects an ESP32 emergency button over BLE with live location, trusted-contact escalation, safety intelligence, and emergency notifications.

The repository is organized into two application layers:

- [`Frontend/`](Frontend/) contains the native Android app and ESP32 firmware.
- [`Backend/`](Backend/) contains the FastAPI API, database models, safety intelligence, scheduler, and FCM delivery service.

The backend phase plan and setup instructions are in [`Backend/README.md`](Backend/README.md). The Android phase roadmap is in [`Frontend/README.md`](Frontend/README.md).

## Backend Quick Start

From the `Backend` directory:

```powershell
cd Backend
python -m venv .venv
.\.venv\Scripts\Activate.ps1
Copy-Item .env.example .env
python -m pip install -r requirements.txt
python -m uvicorn main:app --reload --port 8000
```

API documentation is available at `http://127.0.0.1:8000/docs`.

## Frontend Build

From the `Frontend` directory:

```powershell
cd Frontend
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

The repository intentionally excludes local secrets, Firebase configuration, Google Maps keys, Gradle caches, IDE metadata, generated build output, backend databases, and APK files.
