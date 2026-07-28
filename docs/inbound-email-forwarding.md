# Inbound Email Forwarding Operations

This runbook configures SendGrid Inbound Parse for Subtrak's generated forwarding addresses.
Application changes alone do not make `inbound.subtrak.me` capable of receiving email.

## 1. Production prerequisites

- A publicly reachable HTTPS backend.
- Control of the `subtrak.me` DNS zone.
- A SendGrid account with `subtrak.me` authenticated.
- Database migrations through `V12` applied.
- Proxy and application request limits configured consistently.

Do not enable the user-facing feature in production until signature verification has been tested
against a real SendGrid delivery.

## 2. DNS

Create an MX record for the inbound-only hostname. Do not replace the MX record for the root domain.

| Host | Type | Priority | Value |
| --- | --- | ---: | --- |
| `inbound.subtrak.me` | MX | 10 | `mx.sendgrid.net` |

DNS providers may expect only `inbound` in the host field and may append the zone automatically.
Verify the final record with:

```powershell
Resolve-DnsName -Type MX inbound.subtrak.me
```

SendGrid's current setup instructions specify priority `10` and `mx.sendgrid.net`.

## 3. Inbound Parse setting

In SendGrid, open **Settings → Inbound Parse → Add Host & URL** and configure:

- Receiving domain: `inbound.subtrak.me`
- Destination URL: `https://<api-host>/webhooks/inbound-email`
- Spam checking: enabled
- Post raw full MIME message: disabled

Subtrak expects SendGrid's default parsed multipart fields (`envelope`, `headers`, `text`, `html`,
and related metadata). Raw MIME mode changes that contract and must remain disabled.

Official references:

- [Configure Inbound Parse](https://www.twilio.com/docs/sendgrid/for-developers/parsing-email/setting-up-the-inbound-parse-webhook)
- [Secure Inbound Parse](https://www.twilio.com/docs/sendgrid/for-developers/parsing-email/securing-your-parse-webhooks)

## 4. Webhook signature policy

Create a SendGrid webhook security policy with ECDSA signature verification enabled and attach it
to the `inbound.subtrak.me` Parse setting. Copy the public key returned by that security policy into
`SENDGRID_INBOUND_WEBHOOK_PUBLIC_KEY`.

This is a distinct configuration value from `SENDGRID_EVENT_WEBHOOK_PUBLIC_KEY`, which protects
SendGrid delivery-event JSON.

Subtrak rejects:

- missing signatures;
- invalid signatures;
- timestamps more than five minutes from the application clock;
- payloads changed before or after signing.

Keep production hosts synchronized to a reliable time source. Never fetch the public key during an
incoming request.

## 5. Application secrets and limits

Generate the forwarding-token encryption key once and store it in the deployment secret manager.
Changing it makes existing forwarding addresses impossible to redisplay.

PowerShell:

```powershell
$bytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
[Convert]::ToBase64String($bytes)
```

Required production values:

```text
INBOUND_EMAIL_DOMAIN=inbound.subtrak.me
INBOUND_EMAIL_TOKEN_ENCRYPTION_KEY=<base64-encoded 32-byte key>
SENDGRID_INBOUND_WEBHOOK_PUBLIC_KEY=<SendGrid security-policy public key>
```

Recommended initial limits:

```text
INBOUND_EMAIL_MAX_REQUEST_BYTES=10485760
INBOUND_EMAIL_MAX_FIELD_BYTES=1048576
INBOUND_EMAIL_MAX_PARTS=30
INBOUND_EMAIL_CONTENT_RETENTION_DAYS=30
INBOUND_EMAIL_RETENTION_BATCH_SIZE=100
INBOUND_EMAIL_RETENTION_CRON=0 15 * * * *
```

The reverse proxy request-body limit must be at least the application request limit but should not
substantially exceed it. The application does not persist attachments.

## 6. Retention behavior

Every hour, the retention worker claims one bounded batch using `FOR UPDATE SKIP LOCKED` and clears
stored text, HTML, and raw headers older than the configured window. Metadata and fingerprints
remain for audit and deduplication.

If an expired event is still `RECEIVED`, `PROCESSING`, or `RETRY`, it moves to `DEAD` with
`CONTENT_RETENTION_EXPIRED`. Terminal events retain their existing status.

## 7. Deployment verification

1. Deploy the backend and apply Flyway migrations.
2. Confirm the public endpoint rejects an unsigned multipart request with `401`.
3. Generate an address while authenticated in Settings.
4. Send a plain-text test message to that address.
5. Confirm SendGrid receives `204`.
6. Confirm one `inbound_email` row exists with status `RECEIVED`.
7. Send the same provider delivery again and confirm no duplicate row is created.
8. Rotate the address and confirm mail sent to the old address receives `204` but creates no row.
9. Confirm subjects, bodies, headers, tokens, and addresses are absent from application logs.

Do not use a production user's real billing email for the initial smoke test.
