# Starter kit documentation

This documentation covers the [Free Lite](../../README.md) source in this repository. The paid Pro edition is described on the [PHABDEV pricing page](https://starter.phabdev.com/pricing/?utm_source=github&utm_medium=organic&utm_campaign=lite_docs).

- [Installation](installation.md): local prerequisites, setup and first request.
- [Architecture](architecture.md): directory and module boundaries.
- [API contract](api-contract.md): exact request and response fields.
- [Authentication](auth.md): access/refresh, cookies and password reset.
- [Authentication design decisions](auth-decisions.md): why each auth and ownership choice was made, the alternatives, the tests that cover it and its limits.
- [RBAC](rbac.md): roles, permissions and ownership.
- [Add an entity](add-new-entity.md): extend one complete vertical slice.
- [Docker](docker.md) and [deployment](deployment.md): local containers and production adaptation.
- [Troubleshooting](troubleshooting.md): environment and common failures.
- [License](license.md), [changelog](changelog.md) and [roadmap](roadmap.md).
- [Version decisions](versions.md): primary sources and actual pinned versions.

The implementation was written largely by AI coding agents, directed by a developer with 14+ years of experience who defined the scope and evaluated and approved the architecture, security model and test plan. The reasoning is recorded in [Authentication design decisions](auth-decisions.md). Review the implementation and test evidence for your own use case; agent-readable documentation is a maintenance aid, not proof of correctness.
