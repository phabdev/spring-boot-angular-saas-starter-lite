# Docker

Each edition has its own Compose project name, network and named database volume. Its backend build uses Maven/Java 21 and its frontend uses Node 24 followed by Nginx. Nginx proxies `/api` to the backend, so browser requests and refresh cookies use one origin.

```sh
node scripts/init-env.mjs
docker compose config --quiet
docker compose up --build -d
docker compose ps
docker compose logs --tail=100 backend
docker compose stop
```

`stop` preserves data. `docker compose down` removes the edition's containers/network but keeps its database volume. **Adding `--volumes` deletes its database data.** Back up first; do not use it as a generic troubleshooting step.

The database healthcheck gates backend startup. Published development ports bind to localhost. Lite and Pro can run together using different host ports. Pro includes Mailpit as a local SMTP sink. Do not expose PostgreSQL or Mailpit publicly.

Changing `DB_PASSWORD` after a database volume is initialized does not automatically change the database role's password. Rotate the actual PostgreSQL role password deliberately and keep `.env` aligned. Do not delete an existing volume to fix a password mismatch.

The Compose files are a local development starting point. Production needs TLS, backups, a public-origin reverse proxy, secret delivery, image updates and a restore procedure. Container startup has to be verified on the target platform; successful YAML validation alone is not runtime evidence.
