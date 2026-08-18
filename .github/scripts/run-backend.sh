#!/usr/bin/env bash
set -Eeuo pipefail
set +x

readonly identifiers_file="/etc/subtrak/runtime-identifiers"
readonly application_jar="/home/ec2-user/subscription-service.jar"

if [[ ! -r "$identifiers_file" ]]; then
  echo "Runtime identifiers are not installed." >&2
  exit 1
fi

# This file contains resource identifiers only and is written by the runtime
# installer after validating every value as a safe single-line token.
# shellcheck disable=SC1090
source "$identifiers_file"

: "${AWS_REGION:?AWS_REGION is required}"
: "${DB_SECRET_ARN:?DB_SECRET_ARN is required}"
: "${APPLICATION_SECRET_ID:?APPLICATION_SECRET_ID is required}"
: "${RUNTIME_PARAMETER_NAME:?RUNTIME_PARAMETER_NAME is required}"

for command_name in aws base64 java jq; do
  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "Required command is unavailable: $command_name" >&2
    exit 1
  fi
done

if [[ ! -r "$application_jar" ]]; then
  echo "Application JAR is unavailable." >&2
  exit 1
fi

db_secret_json="$(
  aws secretsmanager get-secret-value \
    --secret-id "$DB_SECRET_ARN" \
    --region "$AWS_REGION" \
    --query SecretString \
    --output text
)"

application_secret_json="$(
  aws secretsmanager get-secret-value \
    --secret-id "$APPLICATION_SECRET_ID" \
    --region "$AWS_REGION" \
    --query SecretString \
    --output text
)"

runtime_json="$(
  aws ssm get-parameter \
    --name "$RUNTIME_PARAMETER_NAME" \
    --region "$AWS_REGION" \
    --query Parameter.Value \
    --output text
)"

json_required() {
  local json="$1"
  local key="$2"

  jq -er --arg key "$key" \
    '.[$key] | select(type == "string" and length > 0)' <<<"$json"
}

json_optional() {
  local json="$1"
  local key="$2"
  local default_value="$3"
  local value

  if value="$(jq -er --arg key "$key" \
    '.[$key] | select(type == "string")' <<<"$json")"; then
    printf '%s' "$value"
  else
    printf '%s' "$default_value"
  fi
}

db_host="$(json_required "$runtime_json" DB_HOST)"
db_port="$(json_required "$runtime_json" DB_PORT)"
db_username="$(json_required "$db_secret_json" username)"
db_password="$(json_required "$db_secret_json" password)"
db_name="$(json_required "$runtime_json" DB_NAME)"

jwt_secret="$(json_required "$application_secret_json" JWT_SECRET)"
inbound_encryption_key="$(
  json_required "$application_secret_json" \
    INBOUND_EMAIL_TOKEN_ENCRYPTION_KEY
)"

if [[ ${#jwt_secret} -lt 43 ]]; then
  echo "JWT secret is too short." >&2
  exit 1
fi

decoded_key_length="$(
  printf '%s' "$inbound_encryption_key" \
    | base64 --decode 2>/dev/null \
    | wc -c
)"
if [[ "$decoded_key_length" -ne 32 ]]; then
  echo "Inbound email encryption key must decode to 32 bytes." >&2
  exit 1
fi

export DB_URL="jdbc:postgresql://${db_host}:${db_port}/${db_name}"
export DB_USERNAME="$db_username"
export DB_PASSWORD="$db_password"
export JWT_SECRET="$jwt_secret"
export INBOUND_EMAIL_TOKEN_ENCRYPTION_KEY="$inbound_encryption_key"

export GOOGLE_CLIENT_ID="$(json_required "$runtime_json" GOOGLE_CLIENT_ID)"
export JWT_ISSUER="$(json_required "$runtime_json" JWT_ISSUER)"
export JWT_AUDIENCE="$(json_required "$runtime_json" JWT_AUDIENCE)"
export PUBLIC_API_URL="$(json_required "$runtime_json" PUBLIC_API_URL)"
export FRONTEND_BASE_URL="$(json_required "$runtime_json" FRONTEND_BASE_URL)"
export FRONTEND_ORIGINS="$(json_required "$runtime_json" FRONTEND_ORIGINS)"

export REFRESH_COOKIE_SECURE="$(
  json_optional "$runtime_json" REFRESH_COOKIE_SECURE true
)"
export REFRESH_COOKIE_SAME_SITE="$(
  json_optional "$runtime_json" REFRESH_COOKIE_SAME_SITE None
)"
export EMAIL_OUTBOUND_PROVIDER="$(
  json_optional "$runtime_json" EMAIL_OUTBOUND_PROVIDER ses
)"
export SENDGRID_INBOUND_ENABLED="$(
  json_optional "$runtime_json" SENDGRID_INBOUND_ENABLED false
)"
export SES_CONSUMERS_ENABLED="$(
  json_optional "$runtime_json" SES_CONSUMERS_ENABLED false
)"
export SES_REGION="$(json_optional "$runtime_json" SES_REGION "$AWS_REGION")"
export INBOUND_EMAIL_DOMAIN="$(
  json_required "$runtime_json" INBOUND_EMAIL_DOMAIN
)"
export SWAGGER_ENABLED="$(json_optional "$runtime_json" SWAGGER_ENABLED true)"

export JPA_SHOW_SQL=false
export HIBERNATE_SQL_LOG_LEVEL=WARN
export HIBERNATE_BIND_LOG_LEVEL=WARN
export SPRING_SECURITY_LOG_LEVEL=INFO
export SPRING_WEB_LOG_LEVEL=INFO

unset db_secret_json application_secret_json runtime_json
unset db_host db_port db_username db_password db_name
unset jwt_secret inbound_encryption_key decoded_key_length

exec /usr/bin/java -jar "$application_jar"
