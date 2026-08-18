#!/usr/bin/env bash
set -Eeuo pipefail

if [[ $# -ne 4 ]]; then
  echo "Usage: configure-backend-runtime.sh <aws-region> <db-secret-arn> <application-secret-id> <runtime-parameter-name>" >&2
  exit 2
fi

if [[ $EUID -ne 0 ]]; then
  echo "Runtime configuration must run as root." >&2
  exit 1
fi

readonly aws_region="$1"
readonly db_secret_arn="$2"
readonly application_secret_id="$3"
readonly runtime_parameter_name="$4"
readonly script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
readonly repository_root="$(cd -- "$script_dir/../.." && pwd)"
readonly launcher_source="$script_dir/run-backend.sh"
readonly service_source="$repository_root/.github/systemd/subscription-tracker.service"
readonly config_dir="/etc/subtrak"
readonly identifiers_file="$config_dir/runtime-identifiers"
readonly launcher_target="/usr/local/bin/run-subtrak"
readonly service_target="/etc/systemd/system/subscription-tracker.service"
readonly application_jar="/home/ec2-user/subscription-service.jar"

if [[ ! "$aws_region" =~ ^[a-z]{2}(-gov)?-[a-z]+-[0-9]+$ ]]; then
  echo "AWS region is invalid." >&2
  exit 1
fi
if [[ ! "$db_secret_arn" =~ ^arn:aws[a-zA-Z-]*:secretsmanager:[a-z0-9-]+:[0-9]{12}:secret:[A-Za-z0-9/_+=.@!-]+$ ]]; then
  echo "Database secret ARN is invalid." >&2
  exit 1
fi
if [[ ! "$application_secret_id" =~ ^[A-Za-z0-9/_+=.@-]+$ ]]; then
  echo "Application secret identifier is invalid." >&2
  exit 1
fi
if [[ ! "$runtime_parameter_name" =~ ^/[A-Za-z0-9_./-]+$ ]]; then
  echo "Runtime parameter name is invalid." >&2
  exit 1
fi

for command_name in aws install jq systemctl; do
  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "Required command is unavailable: $command_name" >&2
    exit 1
  fi
done

test -r "$launcher_source"
test -r "$service_source"

# Validate access and JSON shape without writing values to stdout or disk.
aws secretsmanager get-secret-value \
  --secret-id "$db_secret_arn" \
  --region "$aws_region" \
  --query SecretString \
  --output text \
  | jq -e \
    '.username and .password' \
    >/dev/null

aws secretsmanager get-secret-value \
  --secret-id "$application_secret_id" \
  --region "$aws_region" \
  --query SecretString \
  --output text \
  | jq -e \
    '.JWT_SECRET and .INBOUND_EMAIL_TOKEN_ENCRYPTION_KEY' \
    >/dev/null

aws ssm get-parameter \
  --name "$runtime_parameter_name" \
  --region "$aws_region" \
  --query Parameter.Value \
  --output text \
  | jq -e \
    '.DB_HOST and .DB_PORT and .DB_NAME and .GOOGLE_CLIENT_ID and
      .FRONTEND_BASE_URL and .FRONTEND_ORIGINS' \
    >/dev/null

install -d -o root -g root -m 0755 "$config_dir"

temporary_identifiers="$(mktemp "$config_dir/runtime-identifiers.XXXXXX")"
cleanup() {
  rm -f -- "$temporary_identifiers"
}
trap cleanup EXIT

{
  printf 'AWS_REGION=%s\n' "$aws_region"
  printf 'DB_SECRET_ARN=%s\n' "$db_secret_arn"
  printf 'APPLICATION_SECRET_ID=%s\n' "$application_secret_id"
  printf 'RUNTIME_PARAMETER_NAME=%s\n' "$runtime_parameter_name"
} >"$temporary_identifiers"

chmod 0644 "$temporary_identifiers"
chown root:root "$temporary_identifiers"
mv -f -- "$temporary_identifiers" "$identifiers_file"
trap - EXIT

install -o root -g root -m 0755 "$launcher_source" "$launcher_target"
install -o root -g root -m 0644 "$service_source" "$service_target"

systemctl daemon-reload
systemctl enable subscription-tracker.service

if [[ -r "$application_jar" ]]; then
  chmod 0644 "$application_jar"
  systemctl restart subscription-tracker.service
else
  systemctl stop subscription-tracker.service >/dev/null 2>&1 || true
  echo "Runtime installed; service remains stopped until the first JAR deployment."
fi
