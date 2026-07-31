# SendGrid Removal Checklist

This is the authoritative inventory and post-rollback removal order. Preparing
this checklist does not disable or delete any SendGrid component.

Run the tracked-file inventory at any time:

```powershell
.\scripts\Get-SendGridRemovalInventory.ps1
```

After the cleanup commit, CI or an operator can use `-RequireRemoved` to fail
while SendGrid remains in runtime code, tests, workflows, or operational
scripts. Documentation can continue to mention SendGrid as migration history.

## Removal gate

Do not begin removal until all of the following are recorded:

- `Get-SesMigrationStatus.ps1 -RequireReady` reports `READY`.
- A real forwarded Gmail message reaches the review queue and can be edited,
  confirmed, or ignored.
- An SES outbound message, mailbox-simulator bounce, complaint event, and
  unsubscribe have been accepted and processed as expected.
- SES inbound and event queues and both DLQs remain empty after processing.
- No request reaches either SendGrid webhook for at least twice the previous MX
  TTL. Record the cutover time, TTL, observation start/end, and evidence.
- The rollback owner explicitly closes the rollback window.

The migration is not removal-ready until the real forwarding acceptance test is
complete, even if DNS and SES health checks are green.

## Tracked repository inventory

### Dependency and runtime components

- `subscription-service/pom.xml`: `com.sendgrid:sendgrid-java`.
- `notification/config/SendGridConfig.java`: conditional client bean.
- `notification/config/SendGridProperties.java`: API key, sender identity, and
  event-webhook key binding.
- `notification/service/SendGridOutboundEmailSender.java`: rollback outbound
  adapter.
- `notification/service/SendGridEventService.java` and
  `SendGridWebhookVerifier.java`: event parsing, verification, and suppression.
- `notification/controller/SendGridEventWebhookController.java`: public
  `/notifications/webhooks/sendgrid`.
- `inboundemail/controller/InboundEmailWebhookController.java`: public
  `/webhooks/inbound-email`.
- `inboundemail/service/InboundEmailWebhookVerifier.java` and
  `SendGridInboundMultipartParser.java`: legacy signed multipart ingestion.
- `InboundEmailIngestionService.java`: still constructor-coupled to the two
  legacy inbound components for `receive(...)`. Remove that method and those
  dependencies without changing provider-neutral `ingest(...)`.
- `SecurityConfig.java`: public access entries for both legacy endpoints.
- `OpenApiConfig.java`: legacy event-webhook API treatment.

Keep `OutboundEmailSender`, SES adapters/consumers, notification retries,
suppression behavior, unsubscribe endpoints, inbound ledgers, MIME
normalization, extraction, suggestions, Gmail verification, and retention.

### Configuration and environment variables

Application bindings in `application.properties`:

- `SENDGRID_API_KEY`
- `SENDGRID_FROM_EMAIL`
- `SENDGRID_FROM_NAME`
- `SENDGRID_EVENT_WEBHOOK_PUBLIC_KEY`
- `SENDGRID_INBOUND_WEBHOOK_PUBLIC_KEY`
- `SENDGRID_INBOUND_ENABLED`
- `EMAIL_OUTBOUND_PROVIDER=sendgrid|ses`

The cleanup must make SES the only outbound implementation and remove the
provider switch rather than leave a non-functional `sendgrid` option.
`SES_*` variables and `SES_CONSUMERS_ENABLED` remain.

Operational references:

- `.github/workflows/deploy.yaml` supplies three dummy SendGrid values to tests.
- `scripts/Set-SesRuntime.ps1` can select SendGrid and always enables legacy
  inbound webhooks.
- `scripts/Get-SesMigrationStatus.ps1` reports the rollback flag.

### Tests

Delete tests devoted solely to removed behavior:

- `SendGridInboundMultipartParserTest`
- `InboundEmailWebhookVerifierTest`
- `SendGridEventServiceTest`
- `SendGridWebhookConditionTest`
- `SendGridRollbackFlagIntegrationTest`
- `EmailServiceTest` (currently tests the SendGrid outbound adapter)

Refactor tests that mock the legacy parser or assert the two legacy public
routes:

- `InboundEmailIngestionServiceTest`
- `ApiBoundaryIntegrationTest`

Remove dummy SendGrid properties from all integration-test bootstraps. Use the
inventory command for the exact current file list; it deliberately derives the
list from tracked files so this document cannot silently omit a newly added
reference.

`EmailSuppressionServiceTest` may retain a historical `"SENDGRID"` source value
to prove old rows remain readable. Do not delete or rewrite production
suppression records, notification ledgers, or inbound-email history merely
because their source/provider is SendGrid.

### Documentation

Update `README.md`, `docs/inbound-email-forwarding.md`,
`infrastructure/README.md`, and `guidelines.md` to describe SES as the only live
provider. Preserve a short migration-history note and this checklist in version
control.

## Secrets and external resources

Secret values must never be printed by inventory or verification scripts.
After the code-only cleanup is deployed and proven healthy:

1. Remove the five `SENDGRID_*` values above from the EC2 service
   EnvironmentFile and systemd drop-ins. Remove the provider/rollback flags once
   the application no longer reads them.
2. Remove matching GitHub Actions/repository/environment secrets if present.
   GitHub does not expose secret values, so verify names in repository settings.
3. Disable the SendGrid Event Webhook and Inbound Parse configuration.
4. Revoke the SendGrid API key last. Record who revoked it and when.
5. Remove unused SendGrid sender/domain authentication only if the account no
   longer needs it for another application.

Do not infer external secret presence from repository search. Inspect EC2,
GitHub, and SendGrid independently.

## Recommended future commits

1. **Close inbound rollback:** set `SENDGRID_INBOUND_ENABLED=false`, deploy,
   verify both legacy endpoints are absent, and run the SES acceptance checks
   again. This is reversible and retains the code.
2. **Remove application integration:** remove the dependency, adapters,
   controllers, verifier/parser, shared-service coupling, security/OpenAPI
   entries, properties, and obsolete tests. Make SES the sole sender.
3. **Remove deployment configuration:** simplify runtime/status scripts and CI,
   then deploy and run unit, PostgreSQL integration, and production health
   checks.
4. **Revoke secrets and external hooks:** perform the external-resource sequence
   above only after the deployed cleanup is stable.
5. **Finalize documentation:** record acceptance evidence and removal time, and
   run:

   ```powershell
   .\scripts\Get-SendGridRemovalInventory.ps1 -RequireRemoved
   ```

Each commit should remain independently reviewable and must not combine
credential revocation with application-code removal.
