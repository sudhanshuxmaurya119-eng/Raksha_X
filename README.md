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

## Deployment

### FastAPI on Render

1. Push this repository to GitHub and choose **New + > Blueprint** in Render.
2. Connect this repository and apply `render.yaml`.
3. The Blueprint creates a private Render Postgres database and wires its connection URL into the API automatically. This demo database uses Render's free tier, which expires after 30 days; upgrade it before storing real user or SOS data, or the database and its data will eventually be deleted.
4. The API health check is `/health`. Render's free web service may sleep when idle.
5. Add `FIREBASE_SERVICE_ACCOUNT_JSON` in Render only if live FCM push delivery is needed.

### Android APK on GitHub Releases

Create these Actions repository secrets before running **Actions > Android Release > Run workflow**:

- `GOOGLE_SERVICES_JSON`: the updated Firebase config for `com.rakshax.app`, with Google sign-in enabled and an OAuth web client included.
- `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, and `ANDROID_KEY_PASSWORD`: the release signing key and credentials. Keep a secure backup of the keystore; future updates must use the same key.

Enter the Render API HTTPS URL and a new tag such as `v1.0.1` in the workflow inputs. The workflow builds a signed APK and publishes it as a GitHub Release asset. The downloadable link will be `https://github.com/sudhanshuxmaurya119-eng/Raksha_X/releases/latest/download/app-release.apk` after the first successful release.

Never add `google-services.json`, signing keys, database URLs, or service-account credentials to Git. The local Firebase config is ignored by Git; Actions receives its copy from the repository secret.
