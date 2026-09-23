# Spring Boot + Angular SaaS Starter (Free Lite)

[![CI](https://github.com/phabdev/spring-boot-angular-saas-starter-lite/actions/workflows/ci.yml/badge.svg)](https://github.com/phabdev/spring-boot-angular-saas-starter-lite/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-0f766e.svg)](LICENSE)
[![Java 21](https://img.shields.io/badge/Java-21-1f2937.svg)](backend/pom.xml)
[![Spring Boot 4.1](https://img.shields.io/badge/Spring_Boot-4.1-6db33f.svg)](backend/pom.xml)
[![Angular 22](https://img.shields.io/badge/Angular-22-dd0031.svg)](frontend/package.json)

A small, readable Spring Boot + Angular codebase with the parts every SaaS rewrites anyway: **registration and login, rotating refresh sessions, roles, a per-user CRUD, PostgreSQL migrations and Docker**. One complete vertical slice (Projects) shows the pattern, and you copy it for your own entities.

It is source code you own and edit. It is not a generator and not a hosted service. MIT licensed.

![Free Lite dashboard with development data](docs/lite-dashboard-desktop.png)

## Quick start

Requirements: Docker with Compose v2, and Node.js 24 to run the one-off secret generator.

```bash
git clone https://github.com/phabdev/spring-boot-angular-saas-starter-lite.git
cd spring-boot-angular-saas-starter-lite
node scripts/init-env.mjs
docker compose up --build -d
```

Then open:

| What | URL |
|---|---|
| App | http://localhost:4200 |
| Swagger UI | http://localhost:4200/swagger-ui/index.html |
| Health | http://localhost:8080/actuator/health |

Register an account and create a project. Then open a private window, register a second account and check that it cannot see the first account's projects.

`init-env.mjs` writes a local `.env` with a random database password and JWT signing key. It refuses to overwrite an existing `.env`, so don't create one by hand first. There are no default or shared credentials. The first build downloads Maven and npm dependencies and can take several minutes.

`docker compose down` stops everything. `docker compose down --volumes` also deletes the database.

## Who this is for

- You already work with **Spring Boot and Angular** and want to start a SaaS, an internal tool or a client project without rewriting auth and project setup again.
- You want to **read every line** of your foundation. The backend has fewer than 20 main classes.
- You work with AI coding agents. The repo includes `AGENTS.md` and task-oriented docs that tell an agent, or a new teammate, where things live and how to verify a change.

It is probably **not** for you if you want a different stack, microservices, multi-tenancy out of the box or a certified security baseline. See [what is not included](#what-is-not-included).

## What's inside

**Backend** (Java 21, Spring Boot 4.1)
- Register, login, logout and `/me`
- Short-lived JWT access tokens, plus random refresh tokens in an `HttpOnly`, `SameSite=Strict` cookie that are **rotated on every use** and stored hashed on the server
- BCrypt passwords, basic rate limiting on auth endpoints and origin checks on cookie-authenticated requests
- `USER` / `ADMIN` roles. Registration always grants `USER`
- Projects CRUD where **ownership is enforced on the server**, not only in the UI
- PostgreSQL 17 with Flyway migrations. Data access uses parameterised Spring JDBC queries, not JPA
- Bean Validation, `ProblemDetail` error responses, OpenAPI/Swagger and an actuator health check
- Integration tests against real PostgreSQL via Testcontainers

**Frontend** (Angular 22, Angular Material)
- Standalone components with lazy-loaded routes
- Login and registration screens, an auth guard and typed API services
- The access token is kept in memory only. On `401`, an HTTP interceptor makes a single shared refresh request, then retries
- Responsive dashboard and project management screens

**Tooling**
- Docker Compose for the full stack, with ports bound to `127.0.0.1`
- A native Java/Node workflow for day-to-day development
- GitHub Actions CI: backend `mvnw verify`, frontend tests and production build

## Project structure

```text
backend/src/main/java/com/phabdev/starter/
  auth/      login, registration, JWT, refresh rotation, security config
  user/      user account, roles, /me
  project/   the example vertical slice: controller → service → repository
  common/    error handling, OpenAPI config
backend/src/main/resources/db/migration/   Flyway SQL
frontend/src/app/
  core/      auth service, interceptor, guard, typed API models and services
  features/  auth, layout shell, projects
docs/starter-kit/                            architecture, auth, API contract, guides
```

The backend is organised by feature, with explicit DTOs and constructor injection. To add your own entity, follow [Add a new entity](docs/starter-kit/add-new-entity.md). It uses Projects as the template and covers the migration, JDBC queries, owner checks, two-account tests, the API contract and the Angular screen.

## How is this different from JHipster?

[JHipster](https://www.jhipster.tech/) is a mature generator with many options: databases, frontends, microservices and entity generation. If you want that breadth, use it.

This starter goes the other way: one fixed stack, a small codebase you can read in an afternoon, and no generator or DSL between you and the code. You change it like any other project. That trade-off suits some teams and not others.

## Native development

Run the database in Docker, and the backend and frontend on your machine:

```bash
node scripts/init-env.mjs        # skip if .env already exists
docker compose up -d db          # PostgreSQL on localhost:5433
```

The backend reads its configuration from environment variables, so load `.env` into your shell before starting it:

```bash
# macOS / Linux
set -a; . ./.env; set +a
cd backend && ./mvnw spring-boot:run
```

```powershell
# Windows PowerShell
Get-Content .env | Where-Object { $_ -match '^[A-Z_]+=' } | ForEach-Object { $k, $v = $_ -split '=', 2; Set-Item "env:$k" $v }
cd backend; ./mvnw.cmd spring-boot:run
```

In a second terminal:

```bash
cd frontend
npm ci
npm start                        # http://localhost:4200, proxies /api to :8080
```

For details and common problems, see [installation](docs/starter-kit/installation.md) and [troubleshooting](docs/starter-kit/troubleshooting.md).

## Run the checks

```bash
(cd backend && ./mvnw verify)                       # needs Docker for Testcontainers
(cd frontend && npm ci && npm test && npm run build)
```

CI runs the same checks on every push to `master` and on every pull request.

## What is not included

Lite is a foundation, not a finished product. It does not include:

- Password reset, email sending, user administration or audit history
- Billing, subscriptions or multi-tenancy (organisations/teams)
- A production deployment. [Deployment](docs/starter-kit/deployment.md) lists what you must configure yourself, starting with `COOKIE_SECURE=true` behind HTTPS
- A security certification. Review the code and the [security policy](SECURITY.md) against your own threat model before going live

## Need more? The Pro edition

A paid **Pro** edition builds on this codebase and adds admin user management, an audit log, user settings, a backend password-reset flow (the reset screen is not finished yet) and an optional Stripe **test-mode** billing reference. It is a one-off source download under a commercial license.

You can try its screens in the [interactive demo](https://demo.starter.phabdev.com/) (simulated data) and see the full comparison on the [pricing page](https://starter.phabdev.com/pricing/?utm_source=github&utm_medium=organic&utm_campaign=lite_readme). Lite does not depend on Pro.

## Documentation

[Architecture](docs/starter-kit/architecture.md) · [Authentication](docs/starter-kit/auth.md) · [Roles and ownership](docs/starter-kit/rbac.md) · [API contract](docs/starter-kit/api-contract.md) · [Docker](docs/starter-kit/docker.md) · [Deployment](docs/starter-kit/deployment.md) · [Version choices](docs/starter-kit/versions.md) · [Changelog](docs/starter-kit/changelog.md)

## Built with AI assistance

This codebase was developed with AI coding agents and reviewed by a human. Tests and CI are included so you can check its behaviour yourself. Treat them as evidence to inspect, not as a guarantee.

## Feedback and contributing

Found a bug? Open an [issue](https://github.com/phabdev/spring-boot-angular-saas-starter-lite/issues) with steps to reproduce. Setup questions and design feedback go in [Discussions](https://github.com/phabdev/spring-boot-angular-saas-starter-lite/discussions). If you tried the starter and something got in your way, that feedback is especially welcome. See [CONTRIBUTING.md](CONTRIBUTING.md) and [SECURITY.md](SECURITY.md).

## License

[MIT](LICENSE). You can use it in personal, commercial and client projects. The Pro edition is licensed separately and is not part of this repository.
