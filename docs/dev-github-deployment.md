# Dev GitHub deployment

The dev backend deploys from the `dev` branch through the GitHub `development`
environment. GitHub obtains short-lived AWS credentials through OIDC; do not
create or store AWS access keys in GitHub.

## Provision the AWS role

Apply the dev Terraform root after reviewing its plan. It creates a GitHub OIDC
provider and a deployment role whose trust policy requires all of the following:

- Repository `Min3th/subscription_tracker`.
- GitHub environment `development`.
- Git ref `refs/heads/dev`.
- Audience `sts.amazonaws.com`.

The role may upload only the five expected deployment objects and may send only
the AWS-managed `AWS-RunShellScript` document to the dev application instance.
It can read the resulting SSM command status but cannot read application secrets
or access other instances.

The application module ignores changes to the moving `latest` Amazon Linux AMI
parameter for an existing instance. Upgrade the instance AMI through a separate,
reviewed replacement plan; an IAM-only deployment plan must never replace EC2.

If the dev account already contains the
`token.actions.githubusercontent.com` IAM OIDC provider, import it into
`module.github_oidc.aws_iam_openid_connect_provider.github` before applying;
do not create a duplicate provider.

## Configure the GitHub environment

Create or open the repository environment named `development`. Restrict deployment
branches to `dev`, then add these environment variables using Terraform outputs:

| GitHub variable | Terraform output |
| --- | --- |
| `AWS_ROLE_ARN` | `github_deployment_role_arn` |
| `AWS_REGION` | configured dev region (`ap-south-1`) |
| `S3_DEPLOY_BUCKET` | `deployment_bucket_name` |
| `EC2_INSTANCE_ID` | `application_instance_id` |
| `DATABASE_SECRET_ARN` | `database_secret_arn` |
| `API_DOMAIN` | `api.dev.subtrak.xyz` |
| `ENABLE_TLS` | initially `false` |
| `LETSENCRYPT_EMAIL` | certificate expiry-notification email |

These are resource identifiers, not secret values. Do not copy the RDS secret
contents or application secret contents into GitHub.

## Deploy

`.github/workflows/deploy-dev.yaml` runs on relevant pushes to `dev` and may be
started manually from that branch. It:

1. Tests deployment rollback behavior and builds the backend.
2. Packages and uploads the JAR and runtime assets.
3. Installs the current runtime assets through SSM.
4. Deploys the JAR using the checksum-verified rollback script.
5. Waits for the service and local HTTP verification to pass.
6. Configures and verifies the Nginx reverse proxy. TLS issuance remains gated
   by the `ENABLE_TLS` environment variable.

Keep the GitHub environment branch restriction aligned with the IAM trust
policy. Changing the environment, repository, or branch requires a reviewed
Terraform change before the workflow can assume the AWS role.

After the first workflow succeeds with `ENABLE_TLS=false`, create the
`api.dev.subtrak.xyz` A record and run `scripts/Test-DevApiDns.ps1` as described
in `docs/dev-api-proxy.md`. Enable TLS only after that check passes.
