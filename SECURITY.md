# Security

This starter is not a security certification. Its threat model includes stolen/expired refresh tokens, cross-user project access, role escalation, forged webhooks and accidental secret publication.

Before deployment enable TLS and secure cookies, set the exact public origin, use fresh signing/database secrets, disable dev seed and public development services, configure backups and review dependency updates. Use a gateway for distributed rate limiting; the Pro in-memory limiter protects a single application instance only. Access tokens remain valid until expiry after logout unless the application adds immediate revocation.

Refresh and reset tokens must be random, hashed in storage, expire and rotate or be consumed once. Keep access tokens out of browser persistent storage. Treat XSS as a security issue even with HttpOnly cookies. The custom auth request header plus strict Origin validation and SameSite cookies protect cookie-authenticated requests; do not loosen CORS to wildcard credentials.

Never include keys, tokens, customer data or password-reset links in public reports. A private vulnerability reporting destination must be configured before distribution. Until then contact the repository owner privately through the established project channel; do not disclose an unpatched exploit in a public issue. No response-time guarantee is currently offered.

See QA_REPORT.md for checks actually executed and remaining risks. Perform an independent security review before processing production personal or billing data.
