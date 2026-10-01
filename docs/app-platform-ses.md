# SES from DigitalOcean App Platform

The Java SES, SQS, and S3 clients already use the AWS SDK v2 default credential
chain. No application code change is required to run outside EC2. Keep the
existing production email CloudFormation stack and its DNS, identities,
configuration set, SNS topics, SQS queues/DLQs, and inbound S3 bucket.

## Credential choice

Use a dedicated IAM identity for this application, never root credentials,
an administrator's key, or the GitHub deployment identity. This setup supports
a dedicated IAM user's access key in App Platform encrypted **RUN_TIME** secrets.
This is an explicit external-hosting exception to the former EC2-only guidance.
Long-lived credentials require rotation; temporary credentials with an automated
refresh mechanism are preferable when such a mechanism is available. Merely
putting expiring STS credentials into environment variables does not renew them.
GitHub OIDC authenticates CI jobs, not a continuously running App Platform service.

The separate `infrastructure/app-platform-ses-access.json` template creates an
IAM user and an inline policy only. It creates no console password or access key,
and does not take ownership of existing email resources. Permissions are:

- `ses:SendEmail` on the exact identity and configuration set, constrained to the
  configured sender address. The application uses SES v2 `SendEmail` with raw MIME,
  not SMTP credentials or a separate `SendRawEmail` API operation.
- Receive, delete, change visibility, and inspect attributes on the two exact
  source queues. No queue publishing, purging, or DLQ consumption.
- `s3:GetObject` under the existing bucket's `incoming/*` prefix. No bucket
  listing, object writes, or object deletion.

The existing stack uses SSE-S3/SQS-managed encryption. If that changes to a
customer-managed KMS key, review its key policy and the narrowly scoped decrypt
permissions separately. The runtime identity intentionally cannot run the broad
infrastructure readiness script; use an operator profile for those checks.

## Discover and prepare (read-only AWS access)

Use an AWS CLI operator profile able to read the production CloudFormation stack.
Do not use guessed queue/bucket names or reuse a development environment's data.
From the repository root, substituting the actual verified sender and profile:

```powershell
.\scripts\Export-AppPlatformSesConfig.ps1 -Profile production-operator -SenderEmail noreply@subtrak.xyz
$env:AWS_PROFILE = 'production-operator'
.\scripts\Test-SesReadiness.ps1 -RequireCutoverReady
```

The exporter validates account, region, sender domain, and queue URLs before
writing `.local/app-platform-ses/parameters.json` and `runtime.env.example`.
The files contain non-secret values only and the directory is ignored by Git.
It does not switch the local backend to production queues or change any AWS resources.
Readiness must confirm identity/DKIM, sending access, routing, and DLQ status.

## Review and provision the dedicated IAM identity

Review the generated parameter file and the template first. Create a reviewable
CloudFormation change set without executing it:

```powershell
aws cloudformation deploy --profile production-operator --region ap-south-1 `
  --stack-name subtrak-production-app-platform-access `
  --template-file infrastructure/app-platform-ses-access.json `
  --parameter-overrides file://.local/app-platform-ses/parameters.json `
  --capabilities CAPABILITY_NAMED_IAM --no-execute-changeset
```

Inspect the change set: it should add only the dedicated IAM user with its policy.
After review, execute that exact change set. The target AWS account must be the
same as the email stack checked by the exporter.

In the IAM console, create an access key for that user for an application running
outside AWS. Transfer it directly to App Platform encrypted runtime variables:

| Variable | Value |
| --- | --- |
| `AWS_ACCESS_KEY_ID` | Dedicated runtime key ID; encrypted, RUN_TIME |
| `AWS_SECRET_ACCESS_KEY` | Dedicated runtime secret; encrypted, RUN_TIME |
| `AWS_EC2_METADATA_DISABLED` | `true` |
| `AWS_REGION`, `SES_REGION` | Existing SES region, from the exporter |

Do not set `AWS_SESSION_TOKEN` for an IAM user's long-lived key. Do not set
`AWS_PROFILE` in App Platform or copy your local AWS credentials directory into
the image. Never put keys in Git, Docker build arguments, Terraform state,
CloudFormation parameters/outputs, generated exports, or terminal logs.

Add the remaining non-secret values from `runtime.env.example` to the backend
component as RUN_TIME variables. Preserve the original
`INBOUND_EMAIL_TOKEN_ENCRYPTION_KEY` separately for the restored Neon records.
Set `PUBLIC_API_URL`, `FRONTEND_BASE_URL`, and `FRONTEND_ORIGINS` to the intended
deployment URLs. SES settings do not replace the Neon/authentication settings.

## Verification and handover

1. Keep `SES_CONSUMERS_ENABLED=false` until the existing workers are stopped and
   the database cutover is complete. Merely connecting a local test backend to
   production queues would let it delete messages needed by the existing backend.
2. Confirm the dedicated runtime caller with `sts get-caller-identity` and inspect
   source queue attributes with that identity. Do not grant additional permissions
   merely to make an operator readiness script work with the runtime identity.
3. Use an operator's IAM policy simulation to check the user can send from the
   approved address using the exact configuration set, consume only the named
   source queues, and read only the intended S3 prefix. Include `ses:FromAddress`
   as simulation context. Simulation is not proof of end-to-end delivery.
4. After explicitly scheduling an acceptance test, send only to the SES mailbox
   simulator using the new runtime credentials and configuration set. Check event
   handling without consuming unrelated queue payloads. The existing
   `Test-SesOutboundAcceptance.ps1` needs operator permissions and therefore does
   not by itself verify the restricted runtime identity.
5. Stop old reminder/retry workers as well as old queue consumers before enabling
   the new production process against Neon. The two databases do not share claims
   or delivery ledgers; running both may send duplicate reminders. Setting
   `SES_CONSUMERS_ENABLED=false` alone does not disable outbound scheduled jobs.
6. Enable consumers only on the intended production backend, verify controlled
   inbound processing and delivery-event handling, and monitor both DLQs.

Do not delete the existing SES stack or inbound DNS while retiring EC2/RDS.
No MX or DKIM changes are needed merely because the backend moves hosts.

## Rotate and recover

Create a second key, update both encrypted App Platform variables, redeploy, and
verify sending/consumption. Deactivate the old key, observe normal operation,
then delete it. Keep the operator credential separate from the application key.
If reverting hosting, stop the new workers before restarting the old ones and
reconcile database changes made since cutover; reverting credentials alone is
not a database rollback.

## References

- [AWS SDK v2 credential chain](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials-chain.html)
- [SES v2 IAM actions and resources](https://docs.aws.amazon.com/service-authorization/latest/reference/list_sesv2.html)
- [App Platform encrypted environment variables](https://docs.digitalocean.com/products/app-platform/how-to/use-environment-variables/)
