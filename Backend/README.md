# RakshaX Backend

The RakshaX backend is a FastAPI service for authentication, SOS events, trusted contacts, live safety intelligence, safer routes, incident data, and emergency push notifications.

This service started from the reusable AuraSafe FastAPI modules. It is now maintained as the RakshaX backend and keeps the existing Android API contract stable while the production infrastructure is upgraded.

## Architecture

```text
RakshaX Android app
        |
        | HTTPS / JSON + bearer JWT
        v
RakshaX FastAPI API
        |-- SQLAlchemy async database
        |-- Risk, hotspot, and safer-route services
        |-- Background news and weather scheduler
        |-- Firebase Cloud Messaging delivery
        v
Supabase PostgreSQL (production target)
```

Local development uses SQLite by default. Supabase PostgreSQL is the production database target because the project has relational users, incidents, SOS events, contacts, routes, and dashboard queries. Firebase is used for FCM push delivery, not as the primary database.

## Local Setup

From the repository root:

```powershell
cd Backend
python -m venv .venv
.\.venv\Scripts\Activate.ps1
Copy-Item .env.example .env
python -m pip install -r requirements.txt
python -m uvicorn main:app --reload --port 8000
```

Open the API contract at `http://127.0.0.1:8000/docs`.

Seed data is created automatically when the local database has no incidents. The scheduler, weather/news integrations, Gemini classification, and FCM delivery degrade gracefully when their keys are not configured.

## Environment

Edit `Backend/.env` locally. Never commit it.

```env
GEMINI_API_KEY=
SECRET_KEY=replace-with-a-long-random-secret
DATABASE_URL=sqlite+aiosqlite:///./rakshax.db
FIREBASE_SERVICE_ACCOUNT_JSON=
NEWS_API_KEY=
GNEWS_API_KEY=
OPENWEATHER_API_KEY=
```

To connect this backend to Supabase, open the Supabase dashboard and copy the PostgreSQL connection string from `Project Settings > Database > Connection pooling`. The HTTPS project URL (`https://<project-ref>.supabase.co`) is not a SQLAlchemy database URL. Use the transaction pooler string for the deployed API, change the scheme to `postgresql+asyncpg`, and set SSL on:

```env
DATABASE_URL=postgresql+asyncpg://postgres:<password>@<host>:5432/postgres
DATABASE_SSL_REQUIRED=true
```

Keep the Supabase connection string server-side. The Android app should call this FastAPI service and should not receive database credentials.

## API Areas

- `/auth` - registration and JWT login
- `/sos` - SOS trigger, status, acknowledgement, and trusted contacts
- `/notifications` - device push-token registration
- `/safety` - risk scores, nearby alerts, heatmap data, and hotspot zones
- `/routes` and `/saved-routes` - safer route generation and saved routes
- `/incidents` - incident reporting and voting
- `/dashboard` - aggregate safety metrics
- `/data-sources` - live data refresh and service diagnostics
- `/health` - scheduler and API health

## Backend Phases

### [x] Phase 0 - Extract and Rename

- Moved the reusable FastAPI service from the ignored `AuraSafe/backend` folder into `Backend`.
- Renamed the local database default to `rakshax.db`.
- Updated API identity, root metadata, and repository documentation for RakshaX.

### [x] Phase 1 - FastAPI Foundation

- FastAPI application and Swagger documentation.
- CORS configuration and environment-based settings.
- Startup database initialization and graceful shutdown.
- `/health` and root service endpoints.

### [x] Phase 2 - Local Persistence

- Async SQLAlchemy session management.
- User, incident, SOS, trusted-contact, push-token, and saved-route models.
- Automatic local seed data for development.

### [x] Phase 3 - Authentication and Device Identity

- Password hashing and JWT login/register flow.
- Bearer-token validation for protected endpoints.
- Android device push-token registration.

### [x] Phase 4 - SOS and Escalation API

- SOS creation with location and trusted contacts.
- Status polling and acknowledgement handling.
- Priority-based contact escalation and delivery tracking.

### [x] Phase 5 - Safety Intelligence

- Risk scoring from incident history.
- DBSCAN hotspot detection.
- Nearby incident and heatmap endpoints.
- Safer route generation around high-risk zones.
- News, weather, and OpenStreetMap service integrations.

### [x] Phase 6 - Firebase Notifications

- Firebase Admin initialization from a service-account environment variable.
- High-priority SOS data notifications through FCM.
- Graceful disabled mode when Firebase credentials are absent.

### [x] Phase 7 - Supabase Database Adapter

- Added the `asyncpg` driver and PostgreSQL URL support.
- Added SSL configuration for Supabase connections.
- Disabled asyncpg statement caching for Supabase transaction-pooler compatibility.

### [ ] Phase 8 - Supabase Project Provisioning

- Create the Supabase PostgreSQL project.
- Put the project connection string in `Backend/.env`.
- Validate schema creation and seed migration against PostgreSQL.
- Add production migration tooling and backups.

### [ ] Phase 9 - Production Security

- Replace development secrets and tighten CORS origins.
- Add request rate limits, audit logging, and structured error responses.
- Move Firebase credentials to the hosting provider's secret manager.
- Add HTTPS deployment and health checks.

### [ ] Phase 10 - Backend Test and Operations Coverage

- Add API tests for auth, SOS, acknowledgement, and notification flows.
- Add PostgreSQL integration tests.
- Add scheduler failure and retry tests.
- Add deployment configuration and monitoring.

## Android Connection

Set the API URL in `Frontend/local.properties`:

```properties
AURORA_SAFE_BASE_URL=http://10.0.2.2:8000
```

Use `http://10.0.2.2:8000` for the Android emulator when the backend runs on the host machine. Use the host computer's LAN IP for a physical Android device on the same network. Release builds must use an HTTPS API URL.
