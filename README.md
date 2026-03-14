# PolicyMind Document Service

Spring Boot backend for document upload/processing plus a React frontend for login and upload flows.

## Project Structure

- `src/` - Java Spring Boot backend
- `frontend/` - React + Vite frontend
- `docker-compose.yml` - PostgreSQL and backend container setup

## Prerequisites

- Java 17+
- Maven 3.9+
- Node.js LTS (npm included)
- PostgreSQL 15+ (or Docker)
- Redis (only if your runtime path uses Redis-backed features)

## Environment Variables

Backend reads values from `.env` in the repository root.

Required keys:

```env
POSTGRES_DB=policymind
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
OPENAI_API_KEY=your_openai_api_key
JWT_SECRET=your_long_random_secret
FRONTEND_OAUTH_CALLBACK_URL=http://localhost:5173/auth/callback

GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=
MICROSOFT_CLIENT_ID=
MICROSOFT_CLIENT_SECRET=
FACEBOOK_CLIENT_ID=
FACEBOOK_CLIENT_SECRET=
LINKEDIN_CLIENT_ID=
LINKEDIN_CLIENT_SECRET=
TWITTER_CLIENT_ID=
TWITTER_CLIENT_SECRET=
```

## Run Backend (Local)

1. Start PostgreSQL on port `5432`.
2. (Optional) Start Redis on port `6379`.
3. Run Spring Boot app:

```bash
mvn spring-boot:run
```

Backend URL: `http://localhost:8080`

Backend logs are written to:

- `logs/policymind-document-service.log` (relative to repository root)

## Run Backend Tests

Use the repo-local helper so Maven dependencies and PDFBox font cache stay inside the workspace:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-tests.ps1
```

To run only a focused subset of tests:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-tests.ps1 -Test "DocumentServiceTest,AuthServiceTest"
```

Notes:

- the helper uses the checked-in Maven path at `.tools/apache-maven-3.9.9`
- dependencies are cached under `.m2/repository`
- PDFBox font cache is redirected to `.pdfbox-cache` to avoid profile-directory permission issues

## Run Frontend (Local)

From `frontend/`:

```bash
npm install
npm run dev
```

Frontend URL: `http://localhost:5173`

Frontend API base URL defaults to `http://localhost:8080`.

To override, create `frontend/.env`:

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_GOOGLE_AUTH_URL=
VITE_MICROSOFT_AUTH_URL=
VITE_FACEBOOK_AUTH_URL=
VITE_LINKEDIN_AUTH_URL=
VITE_TWITTER_AUTH_URL=
```

If social URL variables are empty, frontend defaults to backend OAuth routes:

- `/oauth2/authorization/google`
- `/oauth2/authorization/microsoft`
- `/oauth2/authorization/facebook`
- `/oauth2/authorization/linkedin`
- `/oauth2/authorization/twitter`

## Run with Docker

For local development:

```bash
docker compose up --build
```

This starts:

- PostgreSQL on `5432`
- Redis on `6379`
- Backend on `8080`
- Frontend on `5173`

For EC2-style production deployment with `.env.production` and mounted GCP credentials:

```bash
docker compose -f docker-compose.yml -f docker-compose.ec2.yml up --build -d
```

Notes:

- Base compose uses `${COMPOSE_ENV_FILE:-.env}` by default, so local Docker runs work with your normal `.env`.
- The EC2 override adds `.env.production` and mounts `./secrets/policymind-ai-80ed72ade163.json`.
- If you do not need ADC credentials, the base compose file can run with `GCP_BEARER_TOKEN` instead.

## Current Frontend Pages

- `/` - Login page
- `/auth/callback` - OAuth callback token handoff
- `/about` - Architecture overview and interview speaker notes
- `/upload` - File upload page
- `/error` - Error page

## API Endpoints Used by Frontend

- `POST /auth/login?username=<name>`
- `POST /upload` (multipart form-data with `file`)
- `GET /oauth2/authorization/{provider}` (social login start)
- `GET /login/oauth2/code/{provider}` (OAuth callback handled by Spring Security)

## Third-Party OAuth Setup

For each provider, create an app in the provider developer console and configure redirect URI:

`http://localhost:8080/login/oauth2/code/{provider}`

Examples:

- Google: `.../google`
- Microsoft: `.../microsoft`
- Facebook: `.../facebook`
- LinkedIn: `.../linkedin`
- Twitter/X: `.../twitter`

After updating `.env`, restart backend (or `docker compose up -d --build`).

## Quick Start

1. Configure root `.env`.
2. Start backend (`mvn spring-boot:run` or `docker compose up --build`).
3. If using local frontend:

```bash
cd frontend
npm install
npm run dev
```

4. Open `http://localhost:5173`.

## Troubleshooting

### 1) Backend fails to start (database connection errors)

Symptoms:

- `Connection refused` to PostgreSQL
- `FATAL: password authentication failed`

Checks:

- Confirm PostgreSQL is running on `localhost:5432`.
- Verify `.env` values for `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`.
- If using Docker for DB, ensure container is healthy:

```bash
docker ps
```

### 2) Backend fails due to missing OpenAI key

Symptoms:

- Startup/runtime errors referencing `OPENAI_API_KEY`

Fix:

- Set `OPENAI_API_KEY` in root `.env`.
- Restart backend after updating env values.

### 3) JWT/auth issues

Symptoms:

- Login works but secured calls fail
- Token parsing/signature errors

Fix:

- Ensure `JWT_SECRET` exists in root `.env` and is sufficiently long/random.
- Clear browser local storage token and login again.

### 4) Frontend cannot reach backend

Symptoms:

- Login/upload fails with network error
- Browser console shows request to wrong host/port

Fix:

- Ensure backend is running on `http://localhost:8080`.
- Set `frontend/.env`:

```env
VITE_API_BASE_URL=http://localhost:8080
```

- Restart Vite dev server after changing `.env`.

### 5) CORS errors in browser

Symptoms:

- Browser console shows CORS blocked request from `http://localhost:5173` to backend

Fix:

- Add/adjust backend CORS configuration to allow the frontend origin.
- Typical allowed origin during local development: `http://localhost:5173`.

### 6) Upload fails (`400` or `415`)

Symptoms:

- Upload endpoint returns bad request or unsupported media type

Fix:

- Ensure form uses `multipart/form-data` and field name is exactly `file`.
- Try supported file types from UI (`.pdf`, `.doc`, `.docx`, `.txt`).

### 7) Node/npm command not found

Symptoms:

- `node` or `npm` not recognized in terminal

Fix:

- Install Node.js LTS from `https://nodejs.org`.
- Restart terminal and verify:

```bash
node -v
npm -v
```

### 8) JSON/BOM parse errors when starting frontend

Symptoms:

- Errors like `Unexpected token '﻿'` while reading JSON config

Fix:

- Save JSON files (`package.json`, etc.) as UTF-8 without BOM.

### 9) Backend startup seems hung or inconsistent

Symptoms:

- App takes a long time to come up
- Startup fails intermittently
- You suspect port conflicts, stale logs, or test/build noise

Fix:

- Run the local diagnostics script:
  - one-time snapshot:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\diagnose-startup.ps1
```

  - live watch during startup:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\diagnose-startup.ps1 -Watch
```

- This checks:
  - whether port `8080` is already in use
  - recent `WARN`/`ERROR` lines from `logs/policymind-document-service.log`
  - latest startup markers like `Started DocumentServiceApplication`
  - failing Surefire test summaries from `target/surefire-reports`
  - in watch mode, newly appended startup/error log lines every few seconds

### 10) EC2 build/deploy visibility

Use the EC2 deploy helper to stream build output live, save a deploy log on the server, and optionally follow container logs after the stack comes up.

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\deploy-ec2.ps1
```

To keep following container logs after deploy:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\deploy-ec2.ps1 -FollowLogs
```

What it does:

- backs up `.env.production` and `secrets/` on the EC2 host
- hard-resets the repo to `origin/main`
- runs `docker-compose ... up --build -d` with live output
- saves the full deploy output to `/home/ec2-user/policymind-deploy-<timestamp>.log`
- optionally tails live compose logs after deployment

## Health Checks

Use these quick commands after backend startup to verify core routes.

### Login endpoint

```bash
curl -X POST "http://localhost:8080/auth/login?username=testuser"
```

PowerShell:

```powershell
$token = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/auth/login?username=testuser"
$token
```

Expected result:

- HTTP `200`
- Response body contains a JWT token string

### Upload endpoint

```bash
curl -X POST "http://localhost:8080/upload" \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -F "file=@/absolute/path/to/sample.pdf"
```

PowerShell:

```powershell
$token = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/auth/login?username=testuser"
$headers = @{ Authorization = "Bearer $token" }
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/upload" -Headers $headers -Form @{
  file = Get-Item "C:\absolute\path\to\sample.pdf"
}
```

Expected result:

- HTTP `200`
- Response body contains backend processing status/result text
