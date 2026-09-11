# Contributing

Start a `feature/<topic>` from `develop`. Keep changes scoped, describe the user-visible outcome and use Conventional Commits. Follow AGENTS.md for the edition boundaries and required verification. Do not merge feature work directly to production.

Run the affected backend's Maven tests, frontend tests and production build. Auth or database changes also need PostgreSQL integration checks. Document skipped tests and commands in the PR. Add a regression test when it protects meaningful behavior, such as owner isolation or refresh replay rejection.

Keep API behavior in docs/starter-kit/api-contract.md aligned with both clients. Modify each edition independently: packaging must work without parent source directories. Check both licenses before moving Pro-only source into Lite. Reports of security vulnerabilities follow SECURITY.md.
