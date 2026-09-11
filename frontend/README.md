# Phabdev SaaS Starter Kit Lite · Angular

A standalone Angular 22.1.6 and Angular Material frontend. Registration, login, session restoration, a personal dashboard and Projects CRUD use the real Spring Boot API.

## Run locally

Use Node 24.15+ (tested 24.19), npm 11, and the Lite backend running on port 8080. Angular also supports Node 22.22.3+, but 22.22.2 is too old. Run these commands from this directory:

```sh
npm ci
npm start
```

Open http://localhost:4200. The development proxy sends `/api/**` to `http://localhost:8080` and preserves the browser Origin. Set backend `APP_ORIGIN=http://localhost:4200` and `COOKIE_SECURE=false` for local HTTP. The edition root Compose configuration builds both servers with PostgreSQL. No sibling edition or repository-root dependency is needed.

```sh
npm test
npm run build
```

The production browser output is `dist/starter/browser`. The included multi-stage Dockerfile builds with Node 24 and serves with nginx. nginx proxies `/api/` to the Compose service `backend:8080` and falls back to index.html for Angular routes.

## Structure and extension

- `src/app/core`: typed DTOs, API service, session, interceptor and guards.
- `src/app/features/auth`: validated login and registration.
- `src/app/features/projects`: server-backed dashboard and owner-scoped Projects CRUD.
- `src/app/features/layout`: responsive navigation and account menu.
- `src/environments`: public build configuration. Never place secrets here.

`environment.ts` is production; the development build replaces it with `environment.development.ts`. Both use the relative `/api` base so cookies stay same-origin. Read the edition's bundled docs/API contract before changing payloads. For another entity, add explicit DTOs and a typed service, then a standalone component with loading, failure, validation and empty states.

## Session behavior

Access tokens exist only in memory. Reload uses the HttpOnly refresh cookie, then verifies `/api/me`; no token is written to localStorage or sessionStorage. A shared refresh request coordinates simultaneous API failures and each API request retries at most once. Auth calls send `X-Requested-With: XMLHttpRequest` and bypass the interceptor. Web Locks serialize cookie-changing auth requests across tabs on localhost/HTTPS. Browsers without Web Locks retain same-tab coordination only; use a supported current browser.

The client respects the returned token lifetime. Logout clears memory immediately and revokes the cookie on the server. Route guards improve navigation; backend ownership and role checks remain authoritative. Registration rejects whitespace names and passwords over 72 UTF-8 bytes in addition to length limits.

## Verification and limitations

Vitest covers memory-only sessions, cookie request headers, refresh on reload, concurrent and late 401s, one-retry behavior, expiry, logout/refresh races, third-party credential isolation, and Projects loading/validation/write states. These tests mock HTTP; they do not prove database behavior, cookie browser policy or production security. See the repository QA report for separately executed API/browser checks.

Open Swagger at http://localhost:4200/swagger-ui/index.html through the web proxy. The development and nginx proxies also forward OpenAPI and the minimal health endpoint. The same browser origin is required for auth mutations; use Swagger's Bearer Authorize input with an access token for protected operations.
