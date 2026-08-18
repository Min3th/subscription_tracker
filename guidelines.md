# Subtrak Development Guidelines

This document is the working reference for anyone changing this repository. Read it before implementing, reviewing, or debugging a change.

## 1. Project Overview

Subtrak is a subscription-tracking SaaS with:

- `subscription-service/`: Java 17, Spring Boot, Spring Security, JPA, PostgreSQL, Flyway,
  JWT, provider-neutral email boundaries, AWS SES as the primary email provider, and
  SendGrid retained temporarily as a rollback adapter.
- `webapp/`: React 19, TypeScript, Vite, Redux Toolkit, Axios, Material UI, Formik, and Yup.
- `.github/workflows/`: backend build and deployment automation.
- `infrastructure/ses-email.yaml`: the production SES messaging stack managed by CloudFormation.
- `infrastructure/terraform/`: isolated environment infrastructure, including remote-state
  bootstrap, networking, database, application compute, environment email resources,
  monitoring, and GitHub OIDC deployment roles.

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
- Preserve `404` for unmapped routes and `400` for missing request parameters;
  the catch-all handler must not turn normal client errors into `500` responses.
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
- Treat provider API acceptance as `SENT`, while preserving the provider message ID for
  operational correlation. Map throttling and temporary network/service failures to the
  existing retry schedule, and map permanent request, identity, or message failures to `DEAD`.
- While the SendGrid rollback adapter remains, treat every non-2xx SendGrid response as a failure.
- Isolate failures so one notification cannot stop a batch.
- Retry with bounded exponential backoff.
- Move exhausted deliveries to the `DEAD` state.
- Claim retry work atomically so multiple application instances cannot process the same row concurrently.
- Preserve unsubscribe, suppression, bounce, and webhook-signature verification behavior.
- Consume SES lifecycle events through a durable SQS queue with a DLQ. Delete an
  SQS message only after recognized event handling succeeds; malformed and unknown
  events must remain available for retry and redrive.
- Treat only permanent SES bounces and complaints as suppression events. Transient
  bounces and delivery confirmations are operational events and must not suppress users.
- Never send to a suppressed recipient.

Email delivery is inherently at-least-once around a crash after provider acceptance but before the local `SENT` update. Changes should minimize and explicitly account for this window.

### 8.1 Inbound Email Ingestion

Inbound subscription-email processing is security-sensitive and must remain durable, tenant-scoped,
and reviewable.

- Keep outbound sending and inbound transport behind provider-neutral application boundaries.
  Provider adapters may translate SES or SendGrid payloads, but notification retries, inbound
  persistence, extraction, and review behavior must not depend on a provider SDK.
- Authenticate AWS access with the default credential chain and an EC2 instance role in production.
  Never add static AWS access keys to application properties, source control, or deployment secrets.
- Keep the application's default SES region aligned with the deployed email infrastructure
  (`ap-south-1` at present), while allowing `SES_REGION` to differ from the region where
  the application process runs.
- Provision production SES, S3, SNS, SQS, DLQs, and runtime IAM grants through the repository
  CloudFormation stack. Environment-specific equivalents may be managed by the Terraform
  email module, but a resource must have only one infrastructure owner. Keep queue publication
  source-account/source-ARN constrained,
  block public S3 access, require TLS, and grant the EC2 role only queue consumption,
  inbound object reads, and verified-identity sending.
- Keep automated SES readiness checks read-only. Discover deployed resource names
  from CloudFormation outputs, never print credentials or raw MIME, and clearly
  distinguish infrastructure checks from end-to-end delivery acceptance tests.
- Require empty SES dead-letter queues before cutover, and surface source-queue
  backlog without reading or logging message bodies.
- Use the SES mailbox simulator for automated delivery, bounce, and complaint
  acceptance checks. Do not direct automated negative-path tests at real recipients,
  and do not consume pre-existing queue payloads merely to prove event delivery.
- Keep production SES runtime changes explicit, reversible, and independent of the
  EC2 deployment region. Discover non-secret resource identifiers from CloudFormation,
  require an apply flag, and restore the previous systemd configuration if restart fails.
- Production deployments must verify the artifact checksum and required worker/migration
  entries before replacement, retain the previous JAR until stable service and local
  HTTP checks pass repeatedly, and restore that JAR automatically on any failure.
- Runtime launchers must retrieve secrets through the EC2 instance role, keep secret
  values in process memory, disable shell tracing, and never persist retrieved values
  in Terraform, GitHub, user data, systemd units, or environment files. Persist only
  validated non-secret resource identifiers needed to locate those values.
- Keep SQL, JDBC bind values, Spring Security, and Spring Web debug logging disabled
  by default. Enable targeted diagnostics only for a bounded investigation, never log
  bind values in production, and restore production-safe levels immediately afterward.
- Scope inbound DNS changes to the dedicated forwarding hostname. Verify the exact
  MX target and priority after publication, and never replace root-domain mail or
  forwarding MX records as part of the SES inbound cutover.
- When the parent provider couples MX records to root-domain mail settings, delegate
  only the inbound subdomain to a separately managed hosted zone. Make hosted-zone
  creation explicit because it incurs a recurring charge, and keep parent NS and
  root MX records unchanged.
- Migration status tooling must be read-only apart from transient SSM command records,
  report DNS, SES, queue/DLQ depth, service health, provider flags, and deployed worker
  presence, and never print credentials, environment secrets, or message content.
- Keep SendGrid webhook controllers behind the rollback-window feature flag. Disabling
  SendGrid inbound acceptance must remove both public SendGrid webhook mappings without
  disabling provider-neutral unsubscribe endpoints.

- Generate forwarding-address tokens with at least 256 bits of cryptographic randomness.
- Store a SHA-256 token hash for recipient lookup. Store a versioned AES-256-GCM ciphertext only
  when the token must be shown to the authenticated owner again; never store the raw token.
- Allow at most one active forwarding address per user, while retaining revoked-address history.
- Derive address ownership from the authenticated JWT subject. Never accept a user ID from the
  forwarding-address API.
- Verify SendGrid Inbound Parse authentication against the untouched multipart request before any
  framework or application parsing modifies its bytes.
- Persist a minimal, idempotent inbound-email record before returning a successful webhook response.
  Perform normalization and subscription extraction in a durable background worker.
- Enforce at the database layer that an inbound email's user owns its recipient address.
- Deduplicate provider retries with a stable fingerprint and a database unique constraint. Treat
  duplicate delivery as a successful no-op.
- When neither MIME `Message-ID` nor provider message ID is available, build the
  fingerprint from canonicalized sender, subject, text, and HTML content. Exclude
  volatile transport headers such as `Received`.
- Return the same successful response for unknown, revoked, and duplicate recipient tokens so the
  public webhook cannot be used to discover valid forwarding addresses.
- Do not log forwarding tokens, sender or recipient addresses, subjects, headers, message bodies,
  verification links, or attachment contents.
- Apply explicit size limits to multipart requests and every stored field. Do not store attachments
  until attachment validation, malware handling, storage, and retention are deliberately designed.
- Resolve inbound ownership exclusively from the authenticated provider's SMTP envelope
  recipients. Visible `To`, `Cc`, and forwarded-message headers are untrusted content and
  must never select a user or forwarding address.
- Parse SES objects as bounded RFC 822/MIME messages, cap MIME part counts, and reject
  attachments and failed virus verdicts before durable ingestion.
- SES inbound workers must validate SNS receipt metadata, require the configured private
  S3 bucket, bound both declared and streamed object sizes, and delete SQS messages only
  after durable insertion, deduplication, or intentional unknown/revoked-address discard.
- Render inbound content as plain text only. Never render untrusted email HTML or automatically
  visit links found in an inbound message.
- Keep extracted results in a pending suggestion. Never create or modify an active subscription
  until the authenticated user confirms the proposed fields.
- Deterministic extraction may leave uncertain fields null. Never invent a price, currency, billing
  interval, renewal date, plan, or provider to make a suggestion look complete.
- Persist exact extracted money as `NUMERIC`/`BigDecimal` and require amount and currency to appear
  together. Confidence must be bounded from zero through one and based only on explicit,
  test-covered evidence.
- Store an evidence summary as non-sensitive rule identifiers or field-presence descriptions, not
  copied email content. Keep at most one suggestion per inbound email.
- Prefer the plain-text part and use a maintained HTML parser only when text is absent. Normalize
  Unicode and whitespace, discard quoted reply lines and forwarding metadata, and never use regex
  as an HTML parser.
- Keep deterministic provider, event, money, cadence, plan, and date rules separately testable.
  Accept dates only when the year is explicit. Treat ambiguous currency symbols and unlabeled
  numbers as unknown rather than applying locale or user-preference guesses.
- Claim inbound work in bounded `FOR UPDATE SKIP LOCKED` batches with an opaque claim token.
  Increment attempts when claiming, recover stale `PROCESSING` claims, retry transient failures
  with bounded exponential backoff, and move exhausted events to `DEAD`. Complete each claimed
  email in its own transaction so one malformed message cannot roll back the rest of the batch.
- Treat possible subscription matches as review hints. A deduplication match must never update,
  merge, or suppress an active subscription without an authenticated user decision.
- Scope suggestion reads and decisions by the authenticated user's stable identity, returning the
  same not-found response for missing and foreign IDs. Lock a pending suggestion while deciding it;
  confirmation must atomically create exactly one subscription and record that link, while ignore
  must create nothing. A second decision attempt must return a conflict.
- Purge stored text, HTML, and headers in bounded, concurrency-safe batches after the configured
  retention window. Expired unprocessed events must move to `DEAD`, because missing content cannot
  be processed safely. Account deletion must cascade through forwarding addresses, inbound events,
  and suggestions.

## 9. Frontend Conventions

- Keep TypeScript types aligned with backend response DTOs.
- For local frontend testing against a deployed API, use the configurable Vite
  proxy target. Keep provider URLs and OAuth client IDs in ignored local
  environment files, and never connect a local worker to production queues or
  databases merely to exercise frontend behavior.
- Use the shared Axios client rather than creating isolated clients.
- Keep global server-backed state in the established Redux slices.
- Model review decisions as server-confirmed state transitions: keep a pending suggestion visible
  while confirm or ignore is in flight, and remove it only after the API succeeds. Prevent duplicate
  list requests while a fetch is active and surface decision failures without discarding the item.
- Use the suggestion confirmation endpoint for review edits; do not route a reviewed suggestion
  through the ordinary create-subscription form. Pre-fill only extracted facts, keep all fields
  editable, warn on possible duplicates, and require an explicit confirmation before ignore.
- Recognize Gmail forwarding verification only from an allowlisted Google sender domain. Persist
  only an HTTPS `mail-settings.google.com/mail/vf-…` action URL after parsing and validating its
  scheme, exact host, port, user info, and path. Never return arbitrary links, follow a link on the
  server, auto-open it in the browser, or allow a verification item to create a subscription.
- Render Gmail verification separately from subscription suggestions. Require the user to explicitly
  open the allowlisted URL in a new, isolated tab and separately mark the step complete; disable the
  completion action when no trusted URL is available.
- Treat Gmail verification URLs as short-lived secrets: clear them immediately on any user decision
  and include undecided URLs in the bounded inbound-content retention job. Retain only the
  non-sensitive suggestion metadata needed for review history and deduplication.
- Build inbound-email extraction fixtures from sanitized examples only. Replace names,
  addresses, account/order/transaction identifiers, card details, forwarding addresses,
  and live URLs while preserving only the wording and layout needed by deterministic rules.
- Keep extracted start dates distinct from receipt timestamps and renewal dates. Populate
  review forms only from explicit evidence; require user input when no start date was found
  instead of silently substituting the processing date.
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
- Tenant ownership and cross-user access rejection through the authenticated HTTP boundary.
- DTO validation, normalization, malformed input, and stable error responses.
- Billing intervals, month-end dates, leap years, zero intervals, and exact decimal arithmetic.
- Flyway migration from an empty PostgreSQL database and from the prior schema.
- Reminder timezone boundaries, idempotency, retries, dead-letter behavior, suppression, and concurrent claiming.

Tenant-isolation regressions require integration tests, not only mocked controller or repository
tests. For every user-owned resource or lifecycle operation:

- Create at least two tenants with distinguishable data.
- Authenticate as one tenant through the real JWT filter.
- Include a positive control proving the owner can perform the operation.
- Attempt cross-tenant reads and every supported mutation.
- Verify both the HTTP response and the persisted database state.
- Prefer `404` for foreign resource identifiers when revealing existence would leak information.
- Test body, query, and path identifiers that could conflict with the authenticated identity.
- Verify invalid, expired, or replayed capability tokens cannot affect either tenant.
- Keep tenant tests under `src/test/java/.../integration` so they run against Testcontainers.

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

Backend integration tests require a running Docker-compatible runtime. Testcontainers provisions
an isolated PostgreSQL database and supplies its connection details, so tests must not depend on a
developer database or `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`. Runtime configuration for the
deployed backend still uses the environment variables documented in `README.md`. A successful
compilation alone is not sufficient when behavior, persistence, authentication, or migrations
changed.

Before handing off work:

1. Review the diff.
2. Confirm no secrets or generated artifacts were added.
3. Confirm unrelated files were not changed.
4. Run the relevant tests, lint, and build.
5. Report what changed, why it changed, verification performed, and any remaining limitation.

Deployment-script changes must retain hermetic coverage for first deployment,
successful upgrade, failed first-deployment cleanup, and failed-upgrade rollback.
Run those tests before packaging or sending a deployment command to an instance.
GitHub deployments must use environment-scoped OIDC roles with repository,
environment, and branch trust conditions. Grant artifact upload and SSM command
permissions only for the matching environment bucket and application instance;
never store long-lived AWS access keys in GitHub.
Treat a change to a moving `latest` AMI parameter as a separate instance-upgrade
operation. IAM, workflow, or application deployments must not silently replace a
configured EC2 instance merely because the upstream AMI pointer changed.
Runtime provisioning must install and verify every command used by deployment
validation. A fresh instance must not depend on an unrecorded manual package
installation before it can validate and deploy its first JAR.
Public backend traffic must terminate at Nginx and proxy only to the application
loopback port. Keep the ACME HTTP-01 path reachable on port 80, enable TLS only
after the exact environment hostname resolves to its Elastic IP, and keep
certificate issuance and renewal non-interactive and idempotent.

Infrastructure changes must also run `terraform fmt -recursive`, `terraform validate`,
and a saved-plan review from the intended environment root. Inspect every replacement
and destroy action before applying; an unexplained destructive action is a blocker, not
an acceptable side effect of an unrelated change.

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

## 14. Retiring an Email Provider

- Maintain an exact, repository-derived provider inventory before removal.
- Do not remove a rollback provider until the replacement passes real inbound
  and outbound acceptance tests, queues and DLQs are healthy, and no legacy
  webhook traffic is observed for at least twice the previous MX TTL.
- Disable legacy endpoints through their feature flag and observe the deployed
  result before deleting their implementation.
- Remove provider code and deploy it successfully before revoking provider
  credentials or external webhook configuration. Revoke API keys last.
- Never delete historical notification, suppression, or inbound-email records
  solely because they name the retired provider.
- Inventory and readiness tools may report secret names and whether settings are
  present, but must never print secret values.

## 15. Infrastructure and Environment Management

Infrastructure ownership must remain explicit:

- CloudFormation owns the production SES identity, receipt rules, S3 inbound storage,
  SNS topics, SQS queues and DLQs, configuration set, and their resource policies.
- Terraform owns the environment foundations represented under
  `infrastructure/terraform`: network, database, application compute, deployment bucket,
  runtime configuration containers, monitoring, environment email resources, and GitHub
  OIDC roles. Do not import or recreate a CloudFormation-owned production resource in
  Terraform without a separately reviewed migration of ownership.
- Use a separate Terraform environment root, remote state, AWS account, runtime resources,
  DNS names, and deployment identity for development, staging, and production. Never share
  a database, queue, bucket, inbound domain, encryption key, or application secret between
  environments.
- The current development deployment mapping is branch `dev` -> GitHub environment
  `development` -> the development AWS account. Branch `main` remains the production
  deployment path, but its workflow must not be described as environment-scoped until it
  explicitly declares and uses the GitHub `production` environment. Treat staging as
  unavailable until its infrastructure and deployment mapping have been explicitly created
  and verified; do not silently point it at development or production.

Terraform state and plan rules:

- Bootstrap each account's remote state deliberately. Store state in a private, encrypted,
  versioned S3 bucket with public access blocked and least-privilege access. Migrating local
  state to S3 changes Terraform's state storage; it is not a Git commit and does not deploy
  application code.
- Commit backend and variable examples, but ignore concrete backend configuration, `.tfvars`,
  state, saved plans, generated archives, and inventory exports that contain environment-specific
  identifiers. Never commit `.terraform/` or a local state file.
- Authenticate with the intended non-root AWS IAM Identity Center profile and verify the account
  and role with STS before initialization, planning, or applying. Do not use root credentials or
  static AWS keys for routine Terraform and deployment work.
- Run Terraform from the relevant root such as `bootstrap/` or `environments/dev/`, not from the
  parent directory that contains no root configuration. Supply the matching backend configuration
  and variable file explicitly.
- Review and apply the saved plan that was inspected. Re-plan after configuration, credentials,
  state, or provider inputs change. Do not use `-target` as a routine way to hide unrelated drift.
- Treat a replacement caused only by a moving AMI lookup as a separate compute-upgrade change.
  Pin or ignore the moving value according to the module contract; never accept an incidental
  instance replacement while adding IAM or another unrelated resource.
- Production databases require deliberate deletion protection, backup retention, and recovery
  planning. Any lower-environment `force_destroy` or reduced-retention choice must be explicit,
  environment-scoped, and must never leak into production defaults.

Runtime configuration and deployment rules:

- Terraform may create secret containers and grant access, but secret values must be populated
  outside Terraform so they do not enter configuration, plans, or state.
- Keep the RDS-managed secret limited to database credentials (`username` and `password`). Store
  application secrets such as `JWT_SECRET` and `INBOUND_EMAIL_TOKEN_ENCRYPTION_KEY` in the
  environment application secret. Store non-secret runtime values such as `DB_HOST`, `DB_PORT`,
  `DB_NAME`, regions, URLs, OAuth client IDs, and feature flags in the environment SSM parameter.
- `DB_HOST` must be a hostname only, `DB_PORT` must be the numeric port value, and `DB_NAME` must
  match the provisioned database. Do not embed schemes, ports, paths, or credentials in `DB_HOST`.
- EC2 retrieves runtime values with its instance role. Persist only identifiers needed to locate
  Secrets Manager and Parameter Store values; never write resolved secrets to user data, systemd
  units, deployment artifacts, GitHub variables, Terraform, logs, or this repository.
- Keep SES consumers disabled in an environment until that environment's inbound and event queues,
  bucket, IAM permissions, and DLQs exist and pass readiness checks.
- Package the runtime launcher and systemd unit as a checksummed archive with an exact allowlist of
  members. Install and validate runtime assets before deploying the first JAR.
- Deployment automation must distinguish a first deployment from an upgrade. A failed first
  deployment stops the service and removes the failed JAR; a failed upgrade restores and restarts
  the previously healthy JAR. Verify both the systemd service and the loopback HTTP endpoint.
- GitHub deployments must use environment-scoped OIDC rather than access keys. The IAM trust policy,
  workflow branch trigger, and GitHub environment name must match exactly, and permissions must be
  limited to the matching deployment bucket and application instance.
- Public traffic terminates at Nginx, which proxies to the Spring service on loopback port 8080.
  Publish the exact environment API DNS record before enabling TLS; preserve the ACME HTTP path and
  keep certificate installation and renewal idempotent.

## 16. Environment Inventory

- Keep `docs/aws-infrastructure-inventory.md` and the environment mapping sheet
  current when AWS resources or GitHub deployment environments change.
- Classify resources from explicit names and tags; leave ambiguous resources
  shared or unassigned until ownership is confirmed.
- Never map dev or staging configuration to production instances, databases,
  queues, buckets, encryption keys, or inbound domains.
- Inventory resource identifiers and secret names only. Never copy secret,
  parameter, database credential, OAuth credential, or message values into the
  repository.
- Treat inventory findings as read-only evidence. Security remediation and
  resource deletion require separate review, exact target verification, and
  independently reversible changes where possible.
