# Installation

## Prerequisites

Docker Desktop or Docker Engine with Compose v2, Git and Node 24 LTS. Container builds install Java/Maven and frontend dependencies; a host JDK is unnecessary for the container path. For native development install Java 21 and use the Maven wrapper. Reserve ports listed below and ensure Docker has adequate memory for two concurrent builds.

## Start one edition

```sh
cd products/phabdev-saas-starter-lite
node scripts/init-env.mjs
docker compose up --build -d
docker compose ps
```

For Pro substitute `phabdev-saas-starter-pro`. From an extracted ZIP, start in its root. The initializer writes a new ignored `.env` with random database password and signing key, and refuses to overwrite existing configuration. `.env.example` documents variables. Do not commit `.env`.

| Service | Lite | Pro |
|---|---|---|
| Web | http://localhost:4200 | http://localhost:4300 |
| API | http://localhost:8080 | http://localhost:8081 |
| Swagger (same-origin UI proxy) | http://localhost:4200/swagger-ui/index.html | http://localhost:4300/swagger-ui/index.html |
| PostgreSQL host port | 5433 | 5434 |
| Local mail inbox | unavailable | http://localhost:8025 |

Open the web app, register with an email, display name and a password of at least 12 characters, then create a Project. Open a second browser session with another account and confirm that it cannot see your projects. Registration creates USER only. There is no shared demo password.

For an optional local admin, set `APP_SEED_ENABLED=true`, your own `APP_SEED_ADMIN_EMAIL` and a strong `APP_SEED_ADMIN_PASSWORD` in `.env`, then restart the backend. Disable seed and remove its password after bootstrap. Use this only in local development.

## Native development

Start the edition's database with `docker compose up -d db`. Export variables from the edition's `.env` in your shell without printing secrets. Set `DB_URL=jdbc:postgresql://localhost:5433/starter` for Lite or port 5434 for Pro, and the corresponding `APP_ORIGIN`. In `backend`, run `./mvnw spring-boot:run` (Windows: `./mvnw.cmd spring-boot:run`); in `frontend`, run `npm ci` and `npm start`. Pro native API must use `PORT=8081` to match its dev proxy.

For native Pro email, start `docker compose up -d mailpit` and override `SMTP_HOST=localhost` (`SMTP_PORT=1025`). The example host `mailpit` is resolvable inside Compose only. If no SMTP server is available, set `APP_EMAIL_ENABLED=false` explicitly for local work; password reset delivery is then unavailable. Enabled email with an unreachable SMTP server correctly reports unhealthy.

Run `./mvnw test` in the backend and `npm test` / `npm run build` in the frontend. Testcontainers checks require a running Docker daemon. See QA_REPORT.md in the source repository for the tested environment and any explicit skips.

Setup is intentionally short. Cold Docker image and Maven/npm downloads depend on network speed and can exceed ten minutes.
