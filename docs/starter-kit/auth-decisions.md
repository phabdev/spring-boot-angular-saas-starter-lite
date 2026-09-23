# Authentication design decisions

This page explains *why* the authentication and ownership code looks the way it does. For each decision it lists the alternatives that were considered, the tests that pin the behaviour down and the limits you should know about. Read it with the code open. [Authentication](auth.md) describes the flow, and this page explains the choices behind it.

Paths are relative to `backend/src/main/java/com/phabdev/starter/` and `frontend/src/app/core/`. Test names refer to `AuthenticationIntegrationTest`, `AuthRequestFilterTest` and `ProjectIntegrationTest` (backend, real PostgreSQL through Testcontainers) and to `auth.spec.ts` (frontend).

## 1. Access token in memory, refresh token in an HttpOnly cookie

**What.** Login returns a short-lived JWT in the response body. Angular keeps it in a field of `AuthService`, never in `localStorage` or `sessionStorage`. The refresh token travels only as the `refresh_token` cookie: `HttpOnly`, `SameSite=Strict`, `Path=/api/auth`, and `Secure` outside local development.

**Why.** Script injected through XSS can read anything in web storage, so a token kept there is easy to steal. An `HttpOnly` cookie can't be read by JavaScript. Scoping the cookie to `/api/auth` means the browser sends it only to the endpoints that need it, not with every API call. Keeping the access token in memory means a reload loses it, and `restore()` gets a new one from the cookie.

**Alternatives.** Both tokens in cookies: simpler client, but every API call becomes cookie-authenticated and needs full CSRF protection. Both tokens in `localStorage`: simplest, but fully exposed to XSS. A server-side session (`JSESSIONID`): a valid choice, but it adds state to every API call.

**Covered by.** `registrationUsesSafeCookieAndServerRole`; frontend: *keeps access credentials in memory without browser storage writes*, *restores a reload via the refresh cookie and verifies /me*.

**Limits.** `HttpOnly` does not stop injected script from *using* the session while the page is open. XSS still has to be prevented with output encoding and a Content Security Policy.

## 2. Refresh tokens are random, hashed and rotated

**What.** A refresh token is 32 bytes from `SecureRandom` (`TokenSecrets.random`). The database stores only its SHA-256 hash (`refresh_token.token_hash`). Every successful refresh revokes the token it received and issues a new one (`AuthService.refresh`).

**Why.** Hashing means a leaked table, backup or log can't be turned into sessions. Rotation shortens the window in which a stolen token is useful.

**Why SHA-256 and not BCrypt.** BCrypt is slow on purpose, to protect low-entropy secrets such as passwords against guessing. A 256-bit random value can't be guessed, so a fast hash is enough, and it allows an indexed lookup by hash (the primary key).

**Alternatives.** A refresh JWT: self-contained, but it can't be revoked without a server-side list anyway. Non-rotating tokens: simpler, but a stolen token stays valid until it expires.

**Covered by.** `refreshRotationRejectsReplayAndRevokesFamily` (the rotated value differs from the original), `expiredRefreshCannotBeUsed`.

## 3. Token families and reuse detection

**What.** Each login or registration starts a new `family_id`, and every rotation inherits it. If a refresh token arrives that is already revoked or expired, the server revokes **the whole family** and returns 401.

**Why.** Rotation alone doesn't reveal theft. If an attacker uses a stolen token first, the real user's copy just stops working. A revoked token showing up again means two parties hold the same token. The server can't tell which one is legitimate, so it ends the session for both. This is the reuse detection described in the OAuth 2.0 Security BCP ([RFC 9700](https://www.rfc-editor.org/rfc/rfc9700)).

**Covered by.** `refreshRotationRejectsReplayAndRevokesFamily`: replaying the original token fails *and* the rotated token stops working too.

**Limits.** The real user is logged out along with the attacker. That is the intended price.

## 4. `noRollbackFor` on `refresh`

**What.** `AuthService.refresh` is annotated with `@Transactional(noRollbackFor = ApiException.class)`.

**Why.** On replay the method revokes the family and then throws `ApiException` to return 401. `ApiException` is a `RuntimeException`, and by default Spring rolls the transaction back on unchecked exceptions. That would undo the revocation while the client still sees a correct 401. `noRollbackFor` makes the revocation commit.

**Covered by.** The last assertion of `refreshRotationRejectsReplayAndRevokesFamily`. A test that checks only the 401 would not catch this.

## 5. Row lock on the user for session changes

**What.** `login`, `refresh` and `logout` run `SELECT ... FROM app_user ... FOR UPDATE` on the account before touching its refresh tokens. `refresh` also locks the token row.

**Why.** Two refreshes with the same token can arrive at the same moment: a double submit, a retry, an attacker racing the user. Without a lock both could read `revoked = false` and each could get a new token, creating two live branches of one family. Locking one row per account makes PostgreSQL run these operations one at a time. The first rotates, the second sees a revoked token and triggers reuse detection. In the Pro edition the same lock also orders password reset against login and refresh.

**Alternatives.** Serializable isolation with retries, or an optimistic `UPDATE ... WHERE revoked = FALSE` that checks the affected row count. Both work. The explicit lock is easier to read and to test.

**Covered by.** `concurrentRefreshCannotCreateTwoUsableSessions` (exactly one 200 and one 401, and afterwards even the winner can't refresh), `concurrentLogoutAndRefreshLeaveNoUsableRefreshToken`.

## 6. The client never races itself

**What.** `AuthService.refresh()` shares one in-flight request through `shareReplay`. The interceptor compares the token a failed request was *sent with* to the current one, and retries without refreshing again if another request already rotated it. `authPost` wraps every cookie-changing request in `navigator.locks.request('phabdev-auth-session', ...)` when the Web Locks API is available.

**Why.** Because of decision 3, a client that presents an already-rotated token logs the user out. Five parallel 401s must not trigger five refreshes. Two browser tabs share the cookie but not JavaScript memory, so the Web Lock serializes their refreshes. The second tab's request is sent only after the first has finished and the browser has stored the new cookie.

**Covered by.** Frontend: *shares one refresh across concurrent subscribers*, *coordinates parallel 401 responses and retries each with the rotated token*, *does not rotate again for a late 401 sent with the previous token*, *retries a 401 once, then signs out without an infinite refresh loop*, *uses a shared Web Lock for cookie-changing requests where available*.

**Limits.** Web Locks work only in secure contexts (HTTPS and `localhost`). In a browser without them, two tabs that refresh at the same instant can still trigger reuse detection and log the user out.

## 7. CSRF: a custom header instead of CSRF tokens

**What.** Spring's CSRF protection is disabled. Instead, `AuthRequestFilter` rejects every non-GET request to `/api/auth/**` unless it carries `X-Requested-With: XMLHttpRequest`, and rejects a present `Origin` header that doesn't match `APP_ORIGIN`. CORS is disabled, so the backend never sends CORS headers.

**Why.** Only the auth endpoints read the cookie, and all other endpoints require a `Bearer` header, which a browser never attaches on its own. For the cookie endpoints, a cross-site page can't add a custom header without a CORS preflight, and the preflight fails because the server allows no cross-origin requests. `SameSite=Strict` is a second, independent layer.

**Alternatives.** Spring Security's synchronizer or double-submit CSRF tokens: standard and robust, but they add a token round-trip to a flow that is already protected by two layers.

**Covered by.** `requestsRequireAuthenticationAndSameOriginAuthHeader`; frontend: auth requests send the header and `withCredentials`.

**Limits.** The design relies on the frontend and API sharing one origin (reverse proxy or the Angular dev proxy). If you split them across origins, you must design CORS and CSRF again.

## 8. The role comes from the database on every request

**What.** Besides the issuer and timestamps, the JWT carries only the user id (`sub`) and a `ver` claim. The request filter in `SecurityConfig` loads the account on every request and builds the authorities from the **current** role in the database. It accepts the token only if `ver` equals the account's `auth_version`.

**Why.** A role or permission change takes effect on the next request, not when the token expires. `auth_version` gives you a way to invalidate every access token of one user: increment it. In Lite nothing increments it yet. In Pro, password reset does.

**Alternatives.** Roles as JWT claims: saves one primary-key query per request, but a demoted admin stays admin until the token expires.

**Covered by.** `signedButExpiredJwtAndTamperedJwtAreRejected`, `crudPersistsAndEnforcesOwnershipForUsersAndAdmins`.

**Limits.** One indexed database read per authenticated request. Logout revokes the refresh family but doesn't bump `auth_version`, so an access token that has already been issued stays valid until it expires (15 minutes by default, `JWT_ACCESS_SECONDS`).

## 9. JWT: HS256, strict validation, short lifetime

**What.** `JwtService` signs tokens with HMAC-SHA256 using `JWT_SECRET`. It refuses to start with a key shorter than 32 bytes or a lifetime outside 30–3600 seconds. The decoder pins the algorithm to HS256, validates the issuer and allows zero clock skew.

**Why.** A single backend signs and verifies its own tokens, so a shared secret is the simplest correct choice. Pinning the algorithm and the issuer closes the classic "algorithm confusion" and "token from another service" problems. The startup checks turn a weak configuration into a startup failure instead of a silent risk.

**Alternatives.** RS256/ES256 with a key pair: needed only when other services must verify tokens without being able to sign them.

**Covered by.** `productionOriginRequiresHttpsAndSecureCookiesAndSigningKeyMustBeStrong`, `signedButExpiredJwtAndTamperedJwtAreRejected`.

**Limits.** Zero clock skew assumes that the servers signing and verifying tokens have synchronized clocks. That is true here because the same process does both.

## 10. Ownership is enforced in SQL, and a foreign resource returns 404

**What.** Every project query in `ProjectRepository` includes `owner_id = :owner`, with the owner taken from the authenticated principal, never from the request. Update and delete report "not found" when no row matched. A project that exists but belongs to someone else is therefore indistinguishable from one that doesn't exist, and the API returns 404, not 403. This applies to admins too: `ADMIN` gives no access to other users' projects.

**Why.** If the check lives in the query, a forgotten `if` in a service can't leak data. Returning 404 means you can't probe which IDs exist.

**Alternatives.** Load, then check ownership in the service and return 403: common, but one missed check is a data leak, and 403 confirms that the ID exists. Row-level security in PostgreSQL: strong, but it needs per-request database session settings.

**Covered by.** `crudPersistsAndEnforcesOwnershipForUsersAndAdmins` (a second account gets 404 on read, update and delete, for both USER and ADMIN), `invalidProjectPayloadAndAnonymousAccessAreRejected`.

**Limits.** `list` returns at most 200 projects, with no pagination yet. It's listed as a candidate improvement in the [roadmap](roadmap.md).

## 11. Spring JDBC instead of JPA

**What.** Data access uses `JdbcClient` with named parameters and explicit SQL.

**Why.** The security-relevant parts of this codebase are *queries*: owner filters, row locks, family revocation. With explicit SQL you can review them by reading them. There are no lazy-loading or dirty-checking side effects, and `FOR UPDATE` is written where it applies.

**Alternatives.** JPA/Hibernate: less boilerplate for rich domain models, and a perfectly good choice if your team prefers it. The owner-filter and locking rules above still apply, as JPQL/Criteria conditions and `@Lock`.

**Limits.** More mapping code by hand as the model grows.

## 12. Login and registration details

- **Constant-work login.** If the email doesn't exist, `login` still runs a BCrypt comparison against a dummy hash. Unknown and known emails then take about the same time, which makes timing-based account enumeration harder.
- **BCrypt cost 12, 12 characters to 72 bytes.** BCrypt ignores input beyond 72 bytes, so longer passwords are rejected rather than silently truncated. With multibyte characters the byte limit can be reached before 72 characters.
- **Email normalization.** Emails are trimmed and lowercased before lookup and storage.
- **Registration never accepts a role.** It always creates `USER`. `ADMIN` exists only through the explicit, opt-in development seed.
- **Rate limiting.** `AuthRequestFilter` allows `AUTH_RATE_LIMIT` auth requests per client address per minute, in memory. It deliberately ignores `X-Forwarded-For`, because that header can be forged.

**Covered by.** `loginNormalizesEmailAndRejectsWrongPassword`, `passwordAndDtoValidationRejectsUnsafeInput`, `limitsAuthRequestsAndIgnoresUntrustedForwardedHeaders`.

**Limits.**
- Registration returns 409 for an email that already exists, so account enumeration through the registration endpoint is still possible. Close it if it matters for your product, for example with a generic response and a confirmation email.
- The rate limiter is per instance and keyed on the connection address. Behind a reverse proxy every user shares the proxy's address, so the limit applies to everybody together. Configure trusted proxy handling (`server.forward-headers-strategy`) or move rate limiting to the proxy before production.

## What is not covered

These are known gaps, not oversights:

- no email verification, password reset (Pro has a backend flow) or multi-factor authentication;
- no session or device list, although refresh families make one straightforward;
- no scheduled cleanup of revoked and expired `refresh_token` rows;
- no immediate revocation of issued access tokens on logout (see decision 8).

Review them against your own threat model before going to production. See also [deployment](deployment.md) and the repository's `SECURITY.md`.
