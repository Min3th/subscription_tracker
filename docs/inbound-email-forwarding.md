# SES Email Migration and Inbound Forwarding Runbook

This runbook covers the staged move from SendGrid to Amazon SES. Infrastructure
is defined in `infrastructure/ses-email.yaml`. DNS changes and the SES production
access request remain controlled deployment actions.

## 1. Deploy infrastructure without changing DNS

Follow `infrastructure/README.md` to validate and deploy the stack in the chosen
SES receiving region. For production, the current default is `ap-south-1`.

The stack needs the existing EC2 instance-role name and
`CAPABILITY_NAMED_IAM`. It creates the SES identity, DKIM tokens, receipt rule,
private raw-MIME bucket, SNS topics, SQS queues, DLQs, configuration set, and
runtime role policy.

Do not publish the inbound MX record at this stage.

## 2. Verify the SES identity and sending access

Publish all three DKIM CNAME records from the CloudFormation outputs. Wait for
the SES identity and DKIM status to become verified.

Request production sending access in the same SES region. While the account is
in the SES sandbox, both sender and recipient identities must be verified and
production reminder delivery cannot be enabled for general users.

Configure these values from stack outputs and deployment settings:

```text
SES_REGION=ap-south-1
SES_CONFIGURATION_SET=subtrak-production
SES_INBOUND_QUEUE_URL=<SesInboundQueueUrl output>
SES_EVENT_QUEUE_URL=<SesEventQueueUrl output>
SES_INBOUND_BUCKET=<SesInboundBucket output>
SES_FROM_EMAIL=noreply@subtrak.xyz
SES_FROM_NAME=SubTrak
```

The backend uses the EC2 instance role and AWS default credential chain. Never
configure static AWS access keys for the service.

## 3. Test SES outbound while SendGrid remains available

Start with internal or verified test recipients:

```text
EMAIL_OUTBOUND_PROVIDER=ses
SES_CONSUMERS_ENABLED=false
SENDGRID_INBOUND_ENABLED=true
```

Restart the service and verify:

1. A reminder is accepted by SES and delivered.
2. `List-Unsubscribe` and `List-Unsubscribe-Post` headers are present.
3. A temporary SES failure enters the existing retry schedule.
4. A permanent request error marks the delivery dead.

Return `EMAIL_OUTBOUND_PROVIDER` to `sendgrid` for immediate outbound rollback.

## 4. Validate lifecycle event processing

Set:

```text
SES_CONSUMERS_ENABLED=true
```

Both SQS consumers require all three queue/bucket values, even if inbound DNS
still points to SendGrid. Restart the service and verify long polling in the
selected region.

Test a permanent bounce and complaint with SES mailbox simulator addresses.
Confirm the recipient is suppressed and notifications are disabled. Confirm
delivery and transient bounce events do not suppress recipients.

Inspect both DLQs. They should remain empty during valid tests. Malformed or
unknown events are intentionally retried and eventually redriven.

## 5. Test inbound on a temporary subdomain

Before changing the production MX record:

1. Confirm in the SES console that the CloudFormation receipt rule set is active.
   Only one receipt rule set can be active per region.
2. Use a temporary SES-controlled inbound subdomain or non-production stack.
3. Publish its MX value as `10 inbound-smtp.<ses-region>.amazonaws.com`.
4. Generate a forwarding address in Subtrak Settings.
5. Forward a plain-text and an HTML subscription email.
6. Confirm one `inbound_email` row and one editable suggestion.
7. Redeliver the same SNS/SQS notification and confirm no duplicate is created.
8. Confirm a revoked or unknown forwarding address is acknowledged without a row.
9. Confirm attachments, oversized MIME, failed virus verdicts, and unexpected
   bucket metadata are retried and eventually reach the inbound DLQ.

Visible MIME `To` headers must not affect ownership. The SES receipt envelope
recipient selects the generated forwarding address.

## 6. Production inbound MX cutover

Record the existing MX TTL and SendGrid traffic baseline. Confirm:

- the SES receipt rule set is active;
- `SES_CONSUMERS_ENABLED=true`;
- both SES queues are being polled;
- the inbound bucket is private and lifecycle configuration is enabled;
- DLQ alarms or an operational inspection process exists.

Change only the inbound hostname MX record:

| Host | Type | Priority | Value |
| --- | --- | ---: | --- |
| `inbound.subtrak.xyz` | MX | 10 | `inbound-smtp.ap-south-1.amazonaws.com` |

Do not change the root-domain MX record.

Monitor SES, SendGrid, both queues, both DLQs, application ingestion, and
suggestion creation throughout the previous DNS TTL and queue-drain window.

## 7. Disable SendGrid after the rollback window

After no SendGrid traffic is observed for at least twice the previous MX TTL:

```text
SENDGRID_INBOUND_ENABLED=false
```

Restart the service. Both legacy paths must return `404`:

- `/webhooks/inbound-email`
- `/notifications/webhooks/sendgrid`

The application unsubscribe endpoints remain enabled.

Keep SendGrid outbound credentials only for the agreed short rollback window.
A later cleanup commit removes SendGrid dependencies, verification code,
endpoints, configuration, and secrets.

## 8. Retention and recovery

Database bodies, HTML, headers, and SES security verdicts follow
`INBOUND_EMAIL_CONTENT_RETENTION_DAYS`, normally 30 days. S3 raw MIME follows
`RawMimeRetentionDays`, normally 31 days, providing one safety day.

Successfully ingested S3 objects remain available until lifecycle expiry for
recovery. The bucket is retained if the CloudFormation stack is deleted.
Database cleanup and S3 lifecycle expiration operate independently.

Never log MIME bodies, headers, generated forwarding addresses, verification
links, tokens, or object content.
