# Working on Phabdev SaaS Starter Kit

Read `docs/starter-kit/api-contract.md` before changing either API or UI. In the source monorepo, update both editions and the contract when shared behavior changes. In a standalone distribution, work on the included edition. Lite and Pro must each build and run from their own extracted directory.

For authentication changes read `docs/starter-kit/auth.md`; verify token expiry, refresh rotation, logout, cross-user project isolation and admin denial. Keep access tokens in frontend memory and refresh tokens in HttpOnly cookies. Enforce authorization on the server.

For a new feature read `docs/starter-kit/architecture.md` and `docs/starter-kit/add-new-entity.md`. Keep Java package-by-feature, explicit DTOs and constructor injection. Use migrations for database changes and Angular standalone components with typed API services. Keep secrets in ignored local environment files.

Run the affected edition's Maven tests and Angular tests/build before reporting completion. Run integration checks against PostgreSQL for auth/schema changes. Record commands, pass/fail/skip counts and environmental blockers in `QA_REPORT.md`; mark untested paths explicitly.

Feature branches start from `develop` and return to `develop` through a PR. Release branches start from `develop` and merge into `master` and `develop` with a version tag; hotfix branches start from `master` and return to both. Use Conventional Commits. Keep production claims proportional to executed evidence.

For public copy in the monorepo, read `marketing/README.md`; the marketing directory is not part of the standalone source distributions. Describe implemented behavior, distinguish optional integrations from verified services, and disclose AI assistance. Sales activation requires real delivery, support, privacy and refund configuration. Preserve the user's explicit decisions over archived notes.
