# Lite backend

Java 21, Spring Boot 4.1.1, Maven, PostgreSQL and Flyway. This folder builds independently; the edition root supplies Docker Compose and the frontend.

## Quick start

For the complete app, follow `../README.md`: copy the edition's `.env.example` to `.env`, generate private credentials, then run `docker compose up --build` from the edition root. No account or password is seeded unless explicitly configured.

To run only the API, install Java 21, start PostgreSQL, and export `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` and `APP_ORIGIN` in your shell. Spring Boot does **not** load `.env` files automatically. Set `JWT_SECRET` to at least 32 unpredictable bytes. Run:

```sh
./mvnw spring-boot:run
```

On Windows use `mvnw.cmd`. Local defaults are API port 8080, database URL `jdbc:postgresql://localhost:5433/starter`, and browser origin `http://localhost:4200`. Production origins require HTTPS and `COOKIE_SECURE=true`.

- Swagger UI through the frontend proxy: `http://localhost:4200/swagger-ui/index.html`
- OpenAPI JSON through the frontend proxy: `http://localhost:4200/v3/api-docs`
- Minimal health: `http://localhost:8080/actuator/health`

Use the web origin for Swagger's interactive authentication requests so it matches `APP_ORIGIN`. The direct API documentation URLs remain available for reading, but browser auth requests from the API port fail the origin check. Swagger supplies the required `X-Requested-With` header and offers bearer-token authorization.

## Verification

```sh
./mvnw test
./mvnw package
```

Tests use a real PostgreSQL 17.11 Testcontainers instance, requiring Docker. A missing Docker daemon fails the suite. If a dedicated test PostgreSQL already exists, export `TEST_DB_URL`, `TEST_DB_USERNAME` and `TEST_DB_PASSWORD`. Tests run migrations and create fixture rows; never point them at production or a shared application database. No H2 substitution or silent integration-test skips are used.

## Security boundaries

Access JWTs stay in browser memory. Random refresh tokens are HttpOnly cookies; only SHA-256 hashes are stored. Rotation is single-use, replay revokes the family, and logout serializes with refresh before revoking that family. A previously issued access token remains valid until expiry. Passwords use BCrypt with a 12-character minimum and 72 UTF-8-byte maximum. Authorization uses current database roles and every project query includes its owner.

Authentication POST requests require `X-Requested-With: XMLHttpRequest`; browser Origin must match `APP_ORIGIN`. Auth responses are not cacheable. `AUTH_RATE_LIMIT` defaults to 30 requests per remote address per minute with at most 10,000 tracked addresses. This process-local limiter does not trust forwarded IP headers. Behind a reverse proxy its address can represent multiple users, so configure a trusted edge limiter before scaling; restart clears counters.

`APP_SEED_ENABLED=true` requires an explicit `APP_SEED_ADMIN_EMAIL` and strong `APP_SEED_ADMIN_PASSWORD`. It only creates a missing development account and never changes an existing account's role/password. Disable seed after setup.

Collections currently return at most 200 entries. Add pagination for larger datasets. Database backups, email verification, abuse defenses and operational monitoring remain deployment responsibilities.
