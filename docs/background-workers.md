# Background worker controls

All settings below are runtime environment variables and require an application
restart. Existing polling intervals remain the defaults. Set booleans to `true`
or `false`; delays are positive integer milliseconds. Cron expressions use
Spring's six fields (seconds first) and the server timezone. Reminder eligibility
continues to use each user's stored timezone.

`BACKGROUND_WORKERS_ENABLED=false` disables scheduling for **all** workers,
including SES queue consumers, regardless of their individual flags. It does
not disable HTTP requests, Flyway/Hibernate initialization, or the one-time
reminder-schedule backfill that runs on startup.

| Worker | Enable variable (default) | Schedule variables (defaults) |
| --- | --- | --- |
| Reminder delivery | `REMINDERS_ENABLED=true` | `REMINDERS_CRON=0 */5 * * * ?` |
| Notification retries | `NOTIFICATION_RETRY_ENABLED=true` | `NOTIFICATION_RETRY_DELAY_MS=60000`, `NOTIFICATION_RETRY_INITIAL_DELAY_MS=30000` |
| Inbound processing | `INBOUND_EMAIL_PROCESSING_ENABLED=true` | `INBOUND_EMAIL_PROCESSING_DELAY_MS=30000`, `INBOUND_EMAIL_PROCESSING_INITIAL_DELAY_MS=15000` |
| Inbound retention cleanup | `INBOUND_EMAIL_RETENTION_ENABLED=true` | `INBOUND_EMAIL_RETENTION_CRON=0 15 * * * *` |
| SES event queue | `SES_CONSUMERS_ENABLED=false` (shared with inbound queue) | `SES_EVENT_POLL_DELAY_MS=1000`, `SES_EVENT_POLL_INITIAL_DELAY_MS=5000` |
| SES inbound queue | `SES_CONSUMERS_ENABLED=false` (shared with event queue) | `SES_INBOUND_POLL_DELAY_MS=1000`, `SES_INBOUND_POLL_INITIAL_DELAY_MS=5000` |

Fixed delays are measured after a run finishes, not from its start. SES polls
already use SQS long polling (up to 20 seconds); a one-second fixed delay does not
mean an empty-queue request every second. Empty SQS polls do not query Neon, but
processing received messages does. The global switch controls scheduling even
when `SES_CONSUMERS_ENABLED=true`; the existing SES configuration must still be
valid if those beans are enabled.

## Local/preview environment without background processing

```dotenv
BACKGROUND_WORKERS_ENABLED=false
SES_CONSUMERS_ENABLED=false
```

This stops automatic reminders, retries, inbound processing, and retention.
Do not use it on the sole production worker unless pausing all that work is
intentional. Requests may still accept inbound work into the database, but it
will wait there until processing resumes. Disabling the queue consumers leaves
messages in SQS, subject to queue retention limits.

## Example: slower polling while preserving background work

```dotenv
BACKGROUND_WORKERS_ENABLED=true
REMINDERS_CRON=0 */15 * * * *
NOTIFICATION_RETRY_DELAY_MS=900000
NOTIFICATION_RETRY_INITIAL_DELAY_MS=900000
INBOUND_EMAIL_PROCESSING_DELAY_MS=900000
INBOUND_EMAIL_PROCESSING_INITIAL_DELAY_MS=900000
```

This is a latency/cost tradeoff, not a guarantee of Neon suspension. Reminders,
retries, and inbound suggestions may be delayed by a polling interval or more
when work takes time or accumulates. Keep retention enabled so content does not
outlive the intended retention policy. Actual activity, schedule alignment,
initial delays, health readiness polling, and incoming SES events determine
whether Neon becomes idle. Observe Neon compute usage with the real workload.

Disabling reminders does not disable the other workers. Row claiming,
idempotency, batch sizes, retries, suppression, and processing logic are unchanged.
Never run old and new production backends against separate database copies with
real outbound delivery enabled during migration: their delivery ledgers do not
coordinate duplicate sends.
