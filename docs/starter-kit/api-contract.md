# API contract v1

JSON API under `/api`. Nginx/Angular dev proxy keeps browser calls same-origin. IDs are UUID strings; dates ISO-8601. Error responses are ProblemDetail with `status`, `title`, `detail`, optional `errors`. Request bodies use lowerCamelCase. Collections initially return JSON arrays, bounded server-side where needed.

## Authentication (both)

- POST `/api/auth/register` `{email,password,displayName}` -> 201 session.
- POST `/api/auth/login` `{email,password}` -> 200 session.
- POST `/api/auth/refresh` empty body, cookie -> 200 session; rotate refresh cookie.
- POST `/api/auth/logout` empty body, cookie -> 204, revoke refresh and clear cookie.
- GET `/api/me` bearer -> user.
- Session `{accessToken,tokenType:"Bearer",expiresIn:900,user:{id,email,displayName,role}}`.
- Role `USER|ADMIN`. Cookie `refresh_token`, HttpOnly, SameSite=Strict, path `/api/auth`, Secure configurable (false only on localhost). Refresh accepts only same-origin browser requests; require custom header `X-Requested-With: XMLHttpRequest` for cookie-authenticated auth mutations and reject disallowed Origin. Frontend sends this header on auth requests. Access JWT lifetime configurable, server's returned expiresIn authoritative.
- Registration password 12..72 characters; displayName 1..100; valid email max 254. Registration never accepts a role. Frontend access token only in memory; refresh on reload and one retry on 401 with a shared in-flight refresh.

## Projects (both)

- GET `/api/projects` -> `Project[]` owned by current user.
- POST `/api/projects` `{name,description}` -> 201 Project.
- PUT `/api/projects/{id}` `{name,description}` -> Project.
- DELETE `/api/projects/{id}` -> 204.
- Project `{id,name,description,createdAt,updatedAt}`. Name 1..120, description max 2000. Other user's IDs return 404. ADMIN does not bypass ownership.

## Pro

- GET `/api/admin/users` -> User[]; PATCH `/api/admin/users/{id}/role` `{role}` -> User. Admin only; reject self-demotion to protect access.
- GET `/api/admin/audit` -> `[{id,actorEmail,action,target,createdAt}]`; admin only.
- GET `/api/settings` -> `{displayName,timezone}`; PUT same body -> saved settings. Timezone valid IANA, max 64.
- GET `/api/permissions` -> `{permissions:string[]}`; USER: `projects:read`, `projects:write`, `settings:write`, `billing:read`; ADMIN adds `users:manage`, `audit:read`.
- POST `/api/auth/forgot-password` `{email}` -> 202 generic response; POST `/api/auth/reset-password` `{token,password}` -> 204. Same custom auth header.
- GET `/api/billing/subscription` -> `{status,plan,configured}`. Initial `status:"none",plan:null,configured:false`.
- POST `/api/billing/checkout` empty -> `{url}`. Server uses configured test price; disabled integration returns 503.
- POST `/api/billing/portal` empty -> `{url}`; unavailable customer returns 409.
- POST `/api/billing/webhook` raw Stripe body plus signature, no JWT; signature required and verified; test events only, idempotent handling.
- GET `/actuator/health` -> minimal status.

## Local ports

Lite web 4200, API 8080, PostgreSQL 5433. Pro web 4300, API 8081, PostgreSQL 5434; optional dev SMTP Mailpit UI 8025. Container backend listens 8080 in both. DB name `starter`, DB user `starter`. Variables `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `APP_ORIGIN`, `COOKIE_SECURE`, `APP_SEED_ENABLED`, `APP_SEED_ADMIN_EMAIL`, `APP_SEED_ADMIN_PASSWORD`.
