# Dev backend runtime

The dev EC2 instance loads runtime configuration when the systemd service
starts. Secret values are never written to Terraform, GitHub, user data, the
systemd unit, or a persistent environment file.

## Runtime sources

- RDS-managed secret: database username and password.
- `subtrak-dev/application` in Secrets Manager: `JWT_SECRET` and
  `INBOUND_EMAIL_TOKEN_ENCRYPTION_KEY`.
- `/subtrak/dev/runtime` in SSM Parameter Store: the non-secret database host,
  port, database name, URLs, identifiers, feature flags, and provider selection.

The EC2 instance role must have `secretsmanager:DescribeSecret` and
`secretsmanager:GetSecretValue` for the two exact secret ARNs, plus
`ssm:GetParameter` for the exact runtime parameter ARN.

## Package the runtime

From the repository root, create the runtime archive and checksum without
including configuration values or secrets:

```bash
tar --create --gzip --file runtime-assets.tar.gz \
  .github/scripts/configure-backend-runtime.sh \
  .github/scripts/run-backend.sh \
  .github/systemd/subscription-tracker.service
sha256sum runtime-assets.tar.gz >runtime-assets.tar.gz.sha256
```

Upload both generated artifacts to the `runtime/` prefix of the environment's
deployment bucket. The artifacts are ignored by Git and must not be committed.

## Install the runtime

Deliver `install-runtime-from-s3.sh` to the instance through SSM, then run it as
root. Pass identifiers only, never secret values:

```bash
sudo .github/scripts/install-runtime-from-s3.sh \
  '<DEV_DEPLOYMENT_BUCKET>' \
  ap-south-1 \
  '<RDS_MANAGED_SECRET_ARN>' \
  subtrak-dev/application \
  /subtrak/dev/runtime
```

The S3 installer downloads the archive and checksum, verifies the checksum and
exact file list, and then invokes the runtime configurator. The configurator
validates AWS access without printing secret values, writes only resource
identifiers to `/etc/subtrak/runtime-identifiers`, installs the launcher at
`/usr/local/bin/run-subtrak`, and installs `subscription-tracker.service`. It
enables the service but leaves it stopped until
`/home/ec2-user/subscription-service.jar` exists.

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
