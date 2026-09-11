# Troubleshooting

| Symptom | Check and next step |
|---|---|
| Docker named pipe/socket missing | Run `docker version`; both Client and Server must be present. Start the installed engine. A broken Desktop installation is an environment blocker, not a passing Compose test. |
| Angular refuses Node | Use Node 24 LTS. Check `node --version` in the same shell running npm. |
| API exits at startup | Read `docker compose logs --tail=100 backend`; confirm database health, required env values and Flyway errors. Do not publish the full environment. |
| DB password rejected | An existing volume preserves its original credentials. Follow the deliberate rotation procedure in docker.md. |
| Refresh returns 403 | Match APP_ORIGIN to the browser origin, use the custom auth header and same-origin proxy, and check cookie Secure vs local HTTP. |
| Refresh returns 401 | Cookie expired, consumed, logged out or missing. Log in again. Do not retry indefinitely. |
| API 404 for Project | The ID may not exist or belongs to another user. This indistinguishable result is intentional. |
| Admin page forbidden | Register creates USER. Bootstrap a local admin deliberately; changing UI state is insufficient. |
| Billing returns 503 | Test billing is disabled or incomplete. Configure your own test account values; there is no built-in working merchant account. |
| Password email absent | Check APP_EMAIL_ENABLED, SMTP host/port and Mailpit. A generic forgot-password response does not disclose whether an account exists. |
| A port is already bound | Identify the owning process. Stop only a process you own or change the relevant host port and frontend origin/proxy consistently. |
| Testcontainers cannot connect | Start Docker and confirm its context. Read test output for skips; never count skipped integration checks as passed. |

If reporting a bug include edition, commit, OS, Java/Node/Docker versions, the smallest reproduction, expected/actual behavior and redacted logs. Never send `.env`, cookies or Authorization headers.
