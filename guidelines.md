# Subtrak Development Guidelines

This document is the working reference for anyone changing this repository. Read it before implementing, reviewing, or debugging a change.

## 1. Project Overview

Subtrak is a subscription-tracking SaaS with:

- `subscription-service/`: Java 17, Spring Boot, Spring Security, JPA, PostgreSQL, Flyway, JWT, and SendGrid.
- `webapp/`: React 19, TypeScript, Vite, Redux Toolkit, Axios, Material UI, Formik, and Yup.
- `.github/workflows/`: backend build and deployment automation.

Keep changes scoped to the requested behavior. Preserve unrelated user changes and avoid broad refactors unless they are necessary for correctness.

## 2. Before Making Changes

1. Read the relevant controller, service, repository, entity, DTO, migration, and tests before editing.
2. Check `git status` and do not overwrite unrelated work.
3. Search for every caller before changing a public method, DTO, enum, database column, or API response.
4. Identify whether the change affects authentication, tenant isolation, money, migrations, notification delivery, or user data. Treat these as high-risk areas.
5. Prefer the smallest complete change that preserves existing API behavior.

Never commit secrets, `.env` files, access tokens, refresh tokens, provider keys, database credentials, or production user data.

## 3. Backend Structure and Conventions

Keep backend code grouped by feature:

```text
controller -> service -> repository -> entity/database
                 |
                 -> DTO/model
```

- Controllers handle HTTP concerns and delegate business rules.
- Services enforce authorization, ownership, validation that depends on state, and business behavior.
- Repositories contain persistence queries, not business decisions.
- Entities represent persisted state and must not be accepted directly as request bodies.
- Request and response DTOs define the API boundary.
- Prefer constructor injection.
- Use enums for closed sets of values.
- Return stable API error objects through the global exception handler.
- Do not expose stack traces, provider responses, secrets, or internal exception messages to clients.

When updating or deleting a user-owned resource, query or validate using both its ID and the authenticated user. Never trust a user ID, owner ID, or resource ID supplied only in a request body.

## 4. Authentication and Security Invariants

These rules must remain true:

- Only JWTs with `type=access` may authenticate normal API requests.
- Refresh endpoints accept only JWTs with `type=refresh`.
- Validate JWT signature, expiry, issuer, audience, subject, and required `jti`.
- Access tokens remain in frontend memory. Do not move them to `localStorage` or `sessionStorage`.
- Refresh tokens remain in `HttpOnly`, `Secure` cookies.
- Store only hashes of refresh tokens in the database.
- Rotate a refresh token after every successful refresh.
- Treat reuse of a rotated token as possible session-family compromise.
- Revoke refresh sessions on logout and security-sensitive account events.
- Do not log tokens or cookie values.

Cross-origin authentication must use the centralized CORS policy. Do not add controller-level `@CrossOrigin` annotations. When `SameSite=None` is used, the refresh cookie must also be `Secure`.

The frontend Axios interceptor must:

- Retry a failed request at most once.
- Never attempt to refresh the refresh request itself.
- Share one in-flight refresh operation between concurrent requests.
- Clear only authentication state when refresh fails.

## 5. API and Input Validation

All write endpoints must accept validated request DTOs.

- Apply required-field, length, range, format, and collection-size constraints.
- Normalize input at the boundary: trim text, convert blank optional values to `null`, and normalize case where appropriate.
- Validate URLs as `http` or `https`.
- Validate currencies using ISO 4217.
- Use enums for subscription type, billing unit, and category.
- Recurring subscriptions require a billing unit and a strictly positive interval.
- One-time subscriptions must not be passed into recurring billing calculations.
- Reject unsupported combinations explicitly instead of relying on null-pointer or database errors.

Path parameters are authoritative. For example, `PUT /subscriptions/{id}` must use `{id}` and must not trust an ID from the request body.

## 6. Money and Currency

Financial values require exact decimal handling:

- PostgreSQL: use `NUMERIC`, with explicit precision and scale.
- Java: use `BigDecimal`; never use `double` or `float` for persisted money or totals.
- TypeScript API models: keep decimal values as strings.
- Frontend calculations: use the scaled `bigint` helpers in `webapp/src/utils/money.ts`.
- Every subscription must store its own ISO 4217 currency.
- Never add values in different currencies.
- If currency conversion is introduced, persist or identify the exchange rate, source currency, target currency, and rate timestamp.
- Define rounding mode and scale explicitly whenever division is introduced.

Converting a value to JavaScript `number` is acceptable only at a display-library boundary, such as chart rendering, after the exact calculation is complete.

## 7. Database and Flyway

Flyway owns the production schema. Hibernate must remain configured with:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

Migration rules:

- `V0__create_initial_schema.sql` must support a fresh empty database.
- Never edit a migration that may already have run in another environment.
- Add a new, monotonically ordered migration for every schema change.
- Keep entity definitions and database constraints aligned.
- Add database `NOT NULL`, length, unique, foreign-key, and check constraints where applicable.
- Design migrations for both fresh installations and upgrades from the current production schema.
- Test migrations against PostgreSQL, not only an in-memory substitute.
- Consider existing nulls, invalid legacy values, indexes, lock duration, and rollback/recovery before tightening constraints.

Backups, point-in-time recovery, retention, and restore drills are deployment responsibilities and must be documented separately from migrations.

## 8. Notification Delivery

Reminder delivery is durable and idempotent. Do not replace it with a full-table loop or direct fire-and-forget email sending.

- Use the persisted reminder schedule to find a bounded batch of due work.
- Calculate due instants using the user's saved timezone.
- Respect both account-level and per-subscription notification preferences.
- Create a durable delivery record with a unique idempotency key.
- Treat every non-2xx SendGrid response as a failure.
- Isolate failures so one notification cannot stop a batch.
- Retry with bounded exponential backoff.
- Move exhausted deliveries to the `DEAD` state.
- Claim retry work atomically so multiple application instances cannot process the same row concurrently.
- Preserve unsubscribe, suppression, bounce, and webhook-signature verification behavior.
- Never send to a suppressed recipient.

Email delivery is inherently at-least-once around a crash after provider acceptance but before the local `SENT` update. Changes should minimize and explicitly account for this window.

## 9. Frontend Conventions

- Keep TypeScript types aligned with backend response DTOs.
- Use the shared Axios client rather than creating isolated clients.
- Keep global server-backed state in the established Redux slices.
- Validate forms on the frontend for usability, while retaining backend validation as authoritative.
- Do not expose switches, payment details, alerts, reports, or other capabilities that are not implemented.
- If an unfinished capability must be visible, label it clearly as “Coming soon” and ensure it cannot imply that data is being processed.
- Use the subscription's currency when displaying its cost.
- Preserve accessible labels, keyboard interaction, loading states, empty states, and useful error feedback.
- Add translations when introducing user-facing text in an already translated area.

Avoid silently clearing unrelated browser storage or preferences during authentication failures.

## 10. Testing Expectations

Add or update tests whenever behavior changes. At minimum, cover the regression that motivated the change.

High-priority backend coverage:

- Authentication token type, issuer, audience, expiry, rotation, reuse, and revocation.
- Tenant ownership and cross-user access rejection.
- DTO validation, normalization, malformed input, and stable error responses.
- Billing intervals, month-end dates, leap years, zero intervals, and exact decimal arithmetic.
- Flyway migration from an empty PostgreSQL database and from the prior schema.
- Reminder timezone boundaries, idempotency, retries, dead-letter behavior, suppression, and concurrent claiming.

Frontend work should add unit/component tests when a test framework is available. Authentication flows and critical subscription workflows should eventually have browser-level end-to-end coverage.

## 11. Verification Commands

Run checks relevant to the files changed.

Backend on Windows:

```powershell
cd subscription-service
.\mvnw.cmd test
.\mvnw.cmd clean package
```

Backend on Linux or CI:

```bash
cd subscription-service
./mvnw test
./mvnw clean package
```

Frontend:

```bash
cd webapp
npm test
npm run test:e2e
npm run lint
npm run build
```

Backend tests require PostgreSQL and the environment variables documented in `README.md`. A successful compilation alone is not sufficient when behavior, persistence, authentication, or migrations changed.

Before handing off work:

1. Review the diff.
2. Confirm no secrets or generated artifacts were added.
3. Confirm unrelated files were not changed.
4. Run the relevant tests, lint, and build.
5. Report what changed, why it changed, verification performed, and any remaining limitation.

## 12. Commit and Review Guidance

Keep commits small and coherent. A commit should represent one reviewable behavior or migration step.

Good examples:

```text
fix(auth): reject refresh tokens in API authentication
fix(subscription): validate recurring billing interval
feat(notification): add durable retry delivery ledger
test(auth): cover refresh-token reuse detection
```

During review, prioritize:

1. Security and tenant isolation.
2. Data integrity and migration safety.
3. Correct money and timezone behavior.
4. Failure handling and idempotency.
5. API compatibility.
6. Tests and operational visibility.
7. Maintainability and user experience.

## 13. Definition of Done

A change is complete only when:

- The requested behavior works end to end.
- Authorization and tenant ownership remain enforced.
- Inputs and failure paths are handled deliberately.
- Entity, DTO, API, migration, and frontend types remain consistent.
- Relevant automated checks pass.
- Configuration and documentation are updated when required.
- No placeholder UI or unsupported claim was introduced.
- Remaining risks or deployment actions are clearly documented.
