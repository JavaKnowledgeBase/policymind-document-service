# Commands Used on 2026-02-28

This file lists the main commands used during setup/debug/deploy, with a one-line reason for each.

## 1) Repository discovery and backend endpoint checks

`Get-ChildItem -Force | Select-Object Name,Mode,Length`  
Why: Listed root project contents to confirm this is a backend repo and find where to add frontend.

`rg --files`  
Why: Quickly indexed all tracked files to understand existing structure.

`Get-Content src\main\java\com\policymind\document\security\AuthController.java`  
Why: Confirmed login endpoint shape (`POST /auth/login?username=...`).

`Get-Content src\main\java\com\policymind\document\controller\DocumentController.java`  
Why: Confirmed upload endpoint shape (`POST /upload` with multipart `file`).

`Get-Content src\main\java\com\policymind\document\security\SecurityConfig.java`  
Why: Reviewed current security/CORS setup while debugging login failures.

`Get-Content src\main\resources\application.yml`  
Why: Verified backend runtime config and added logging settings.

`Get-Content pom.xml`  
Why: Checked Java/Spring dependencies and build configuration.

`Get-Content docker-compose.yml`  
Why: Reviewed current container setup before adding frontend service.

## 2) Frontend scaffold creation

`New-Item -ItemType Directory -Force -Path ...` and multiple `Set-Content ...` commands under `frontend/`  
Why: Created React + Vite frontend files (3 pages, routing, API client, styles, env example).

`Get-ChildItem -Recurse -File frontend | Select-Object FullName`  
Why: Verified all frontend files were created.

`Get-Content frontend\package.json`  
Why: Validated required frontend libraries (`react`, `react-router-dom`, `axios`, `vite`).

`Get-Content frontend\src\App.jsx`  
Why: Verified page routing and protected upload route.

## 3) Local runtime and encoding fix

`npm install` (in `frontend/`)  
Why: Attempted to install frontend dependencies (initially failed before Node/npm install).

`node -v; where.exe node; where.exe npm`  
Why: Checked whether Node.js/npm existed on the machine after install issues.

`npm run dev` (in `frontend/`)  
Why: Started the Vite dev server locally.

PowerShell UTF-8 no-BOM rewrite for `frontend\package.json`  
Why: Fixed JSON parse failure caused by BOM (`Unexpected token '﻿'`).

## 4) README/documentation updates

`Get-Content README.md` and `Get-Content README.md | Select-Object -Last ...`  
Why: Reviewed README before/after updates.

`Set-Content README.md ...` and `apply_patch` updates  
Why: Added setup docs, troubleshooting, health checks, and PowerShell `Invoke-RestMethod` examples.

## 5) Backend fixes for login failures

`Get-Content src\main\java\com\policymind\document\security\JwtAuthenticationFilter.java`  
Why: Investigated JWT parsing errors from logs.

`Get-Content src\main\java\com\policymind\document\security\JwtService.java`  
Why: Confirmed token parsing/signing behavior.

`apply_patch` on `SecurityConfig.java`  
Why: Enabled CORS for frontend origin (`http://localhost:5173`).

`apply_patch` on `JwtAuthenticationFilter.java`  
Why: Fixed bug using full `Authorization` header instead of stripped bearer token.

## 6) Docker full-stack deployment

`docker compose config`  
Why: Validated compose syntax and resolved warnings/issues before startup.

`docker compose up -d --build`  
Why: Built and started PostgreSQL + backend + frontend containers.

`docker compose ps`  
Why: Confirmed which services were running and what ports were published.

`docker compose logs --no-color --tail=80 policymind`  
Why: Verified backend container startup success and runtime health.

`docker compose up -d`  
Why: Retried start after transient host-port conflict on `8080`.

## 7) Commands you ran during setup

`node -v`  
Why: Verified Node.js installation.

`npm run dev`  
Why: Started local frontend development server.

## Optional daily ops commands (used/recommended repeatedly)

`docker compose logs -f policymind`  
Why: Live backend logs while testing login/upload.

`Get-Content .\logs\policymind-document-service.log -Wait`  
Why: Tail file-based backend logs in local (non-container) mode.
