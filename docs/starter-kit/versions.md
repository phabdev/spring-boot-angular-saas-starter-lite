# Version decisions

Verified against official documentation and registries on 2026-09-11. The manifests and lockfiles are authoritative for exact patch versions used in a build.

| Component | Decision | Evidence |
|---|---|---|
| Java | 21 LTS conservative default | Spring Boot 4 supports Java 21; upgrading to 25 is unnecessary for this scope. |
| Spring Boot | stable supported 4.x | [System requirements](https://docs.spring.io/spring-boot/system-requirements.html) |
| Angular | stable 22.x, Node 24 LTS | [Compatibility](https://angular.dev/reference/versions), [support policy](https://angular.dev/reference/releases) |
| OpenAPI | springdoc 3.x with Boot 4 | [Compatibility matrix](https://springdoc.org/) |
| Database | PostgreSQL, one Flyway migration chain per edition | Real PostgreSQL integration checks required for schema behavior. |

The initial machine had Node 22.22.2, below Angular 22's documented minimum for that line. Validation uses Node 24 instead. A portable Temurin Java 21 was downloaded from Adoptium with SHA-256 verification for local builds; it is excluded from distributions.

Versions being supported does not establish that every integration works. QA_REPORT.md records actual build/runtime evidence separately.
