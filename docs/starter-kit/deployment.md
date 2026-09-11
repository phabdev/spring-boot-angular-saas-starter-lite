# Deployment

Deploy one edition as one API plus one PostgreSQL database and one static Angular frontend. Use a managed database or a backed-up volume. Start with a single instance; distributed scheduling and horizontal scale are outside this kit's V1.

1. Build reproducible artifacts from a reviewed commit with locked frontend dependencies. Run tests before pushing images.
2. Supply a fresh JWT secret and database password through your platform's secret mechanism. Set the exact HTTPS `APP_ORIGIN`, `COOKIE_SECURE=true`, and disable bootstrap seed.
3. Put the UI and `/api` behind one TLS origin. Keep upstream forwarding controlled; do not trust arbitrary client-supplied proxy headers.
4. Expose only intended public API routes and minimal health. Restrict or disable Swagger and development database/mail ports as needed for your environment.
5. Back up the database and test restoring it. Apply Flyway migrations once as part of the rollout; check compatibility before rolling back application code.
6. Configure logs, availability checks and alerts. Keep sensitive request bodies, tokens and reset links out of logs.
7. Verify real registration, login, refresh, owner isolation and logout on the deployed origin. Test SMTP delivery separately. Stripe remains test mode.

Before collecting personal data, finalize your privacy disclosures, retention and deletion procedures. Before taking payments, review terms, licensing, refund handling and applicable consumer requirements with a qualified reviewer. This document is an implementation checklist, not legal advice or proof of compliance.

There is no hosted service or production deployment included. See [launch readiness](launch-readiness.md) for the commercial site's separate activation requirements.
