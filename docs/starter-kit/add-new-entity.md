# Add a new entity

Use Project as a complete example. Work in one edition first, then port to the other only if the feature belongs there.

1. Add a forward Flyway migration for `customers`: UUID primary key, `owner_id` foreign key, bounded name and timestamps. Index owner queries. Keep previous migrations immutable after deployment.
2. Add a `customer` package with request/response DTOs, controller, transactional service and parameterized JDBC queries. Validate input at the boundary; return ProblemDetail errors.
3. Derive owner identity from authentication. Every read/update/delete query must constrain both resource ID and owner ID. Return 404 for resources the caller cannot access.
4. Add backend tests with two accounts: A creates, B cannot list/read/edit/delete A's data; A can update/delete. Cover invalid input and unauthenticated requests.
5. Document endpoints in the API contract, then add TypeScript models/service methods and an Angular route/component with real loading, empty, validation and failure states.
6. Test form submission through the running web app, refresh the page, and verify persisted data. Run Maven tests and Angular tests/build. Record any untested behavior in the handoff.

Do not accept `ownerId` from the UI or reuse an unscoped lookup for convenience. Add pagination before the collection can grow without a bounded limit.
