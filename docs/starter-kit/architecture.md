# Architecture

Each edition is a standalone application: Angular browser client, Spring Boot JSON API and PostgreSQL database. The editions share documented behavior but have no runtime dependency on each other. This deliberate duplication makes a purchased ZIP usable on its own; shared fixes must be reviewed in both source trees.

Backend code is organized by feature. Controllers validate DTOs and delegate business operations; services enforce ownership/roles and transaction boundaries; JDBC repositories use parameterized SQL. Flyway owns schema history. Explicit SQL and small modules keep persistence behavior visible without a generator or reflection-heavy abstraction layer.

Angular uses standalone components, a typed API service and route guards. The server remains the authority for permissions. A session service keeps the access token in memory, restores a session using the refresh cookie and coordinates concurrent refreshes. The reverse proxy routes `/api` to the backend on the same origin.

Core modules: auth/users, projects and common errors/security. Pro adds admin/audit, settings, password reset/email, billing and rate limiting. `Project` belongs to a user; ADMIN does not implicitly gain access to another user's project. This is user-level isolation, not organizational multi-tenancy.

Build outputs and environment secrets are excluded from Git and source distributions. The marketing site is a separate static app under `apps/landing`; its commerce configuration does not configure the Pro sample's SaaS billing.

## Boundaries to preserve

- DTOs expose only public fields, never hashes or tokens other than the issued access token.
- Database changes are forward migrations; production uses migration validation, not automatic schema creation.
- SMTP and Stripe are replaceable external adapters; disabled integrations produce explicit unavailable states.
- Health responses expose minimal status. The admin audit is an application activity record, not a tamper-proof compliance ledger.
