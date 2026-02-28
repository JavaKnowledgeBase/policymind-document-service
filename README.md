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

## Run with Docker

```bash
docker compose up --build
```

This starts:

- PostgreSQL on `5432`
- Backend on `8080`
- Frontend on `5173`

## Current Frontend Pages

- `/` - Login page
- `/upload` - File upload page
- `/error` - Error page

## API Endpoints Used by Frontend

- `POST /auth/login?username=<name>`
- `POST /upload` (multipart form-data with `file`)

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
