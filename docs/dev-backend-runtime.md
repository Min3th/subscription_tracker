# Dev backend runtime

The dev EC2 instance loads runtime configuration when the systemd service
starts. Secret values are never written to Terraform, GitHub, user data, the
systemd unit, or a persistent environment file.

## Runtime sources

- RDS-managed secret: database host, port, username, and password.
- `subtrak-dev/application` in Secrets Manager: `JWT_SECRET` and
  `INBOUND_EMAIL_TOKEN_ENCRYPTION_KEY`.
- `/subtrak/dev/runtime` in SSM Parameter Store: non-secret URLs, identifiers,
  feature flags, and provider selection.

The EC2 instance role must have `secretsmanager:DescribeSecret` and
`secretsmanager:GetSecretValue` for the two exact secret ARNs, plus
`ssm:GetParameter` for the exact runtime parameter ARN.

## Install the runtime

Deliver the repository with both scripts and the systemd unit intact, then run
the installer as root on the EC2 instance:

```bash
sudo .github/scripts/configure-backend-runtime.sh \
  ap-south-1 \
  '<RDS_MANAGED_SECRET_ARN>' \
  subtrak-dev/application \
  /subtrak/dev/runtime
```

The installer validates access without printing secret values, writes only
resource identifiers to `/etc/subtrak/runtime-identifiers`, installs the
launcher at `/usr/local/bin/run-subtrak`, and installs
`subscription-tracker.service`. It enables the service but leaves it stopped
until `/home/ec2-user/subscription-service.jar` exists.

## Verify

After the first JAR deployment:

```bash
sudo systemctl status subscription-tracker.service
sudo journalctl -u subscription-tracker.service --since '10 minutes ago'
curl --fail --silent --show-error http://127.0.0.1:8080/v3/api-docs >/dev/null
```

Do not enable shell tracing, print environment variables, run
`systemctl show ... --property=Environment`, or copy Secrets Manager responses
into troubleshooting output.
