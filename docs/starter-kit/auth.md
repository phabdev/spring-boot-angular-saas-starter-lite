# Authentication

For the reasoning, alternatives and limits behind each choice, see [Authentication design decisions](auth-decisions.md). Read [API contract](api-contract.md) for exact fields. Register/login return a JWT access token and public user fields, while a random refresh token is set as the `refresh_token` HttpOnly cookie. Passwords are BCrypt hashes. The server stores refresh token hashes and expiry; the browser keeps the access token only in memory.

The client sends `Authorization: Bearer <accessToken>` to protected endpoints. On page reload it refreshes the session. On an API 401 it shares one refresh request across concurrent failures and retries each request once. Authentication endpoints do not recursively trigger refresh. A failed refresh clears the client session.

Every cookie-authenticated auth mutation sends `X-Requested-With: XMLHttpRequest`. Browser Origin must match configured `APP_ORIGIN`; SameSite=Strict and a cookie path of `/api/auth` add protection. Production must set `COOKIE_SECURE=true` and use HTTPS. Local HTTP is the only intended use for an insecure cookie.

Refresh tokens rotate in both editions. A consumed or expired refresh token is invalid. Logout revokes the session's refresh token and expires its cookie; a previously issued JWT may remain valid until its short expiry. `JWT_ACCESS_SECONDS` and `JWT_REFRESH_DAYS` configure lifetimes. Signing keys must contain at least 32 bytes of unpredictable material and remain outside version control.

Registration always grants USER and does not accept caller-selected privileges. Auth errors should not reveal passwords, hashes or raw database errors. Account enumeration, credential stuffing and application-specific abuse should also be reviewed for the intended deployment.

## Pro reset flow

`forgot-password` responds generically with 202. For an existing account the email adapter sends a one-use expiring reset link. Reset consumes a hash-matched token, changes the password and revokes existing refresh sessions. Use the local Mailpit inbox to test delivery. Production requires a configured SMTP provider, sender domain and deliverability testing. No production email delivery is implied by a successful unit test.

Use a strong password of 12..72 characters. BCrypt has a 72-byte input limit, so the backend also rejects longer UTF-8 byte sequences; a multibyte password can reach this limit before 72 characters.
