#!/usr/bin/env bash
set -Eeuo pipefail

if [[ $# -ne 2 ]]; then
  echo "Usage: deploy-backend.sh <deployment-bucket> <deployment-region>" >&2
  exit 2
fi

deployment_bucket="$1"
deployment_region="$2"
service_name="subscription-tracker"
app_dir="/home/ec2-user"
current_jar="$app_dir/subscription-service.jar"
new_jar="$current_jar.new"
previous_jar="$current_jar.previous"
checksum_file="$new_jar.sha256"
replacement_started=false

rollback() {
  exit_code=$?
  trap - ERR
  echo "Deployment failed; restoring the previous JAR." >&2
  if [[ "$replacement_started" = true ]] && sudo test -f "$previous_jar"; then
    sudo mv "$previous_jar" "$current_jar"
    sudo systemctl restart "$service_name"
  fi
  sudo rm -f "$new_jar" "$checksum_file"
  exit "$exit_code"
}
trap rollback ERR

aws s3 cp \
  "s3://$deployment_bucket/subscription-service.jar" \
  "$new_jar" \
  --region "$deployment_region" \
  --only-show-errors
aws s3 cp \
  "s3://$deployment_bucket/subscription-service.jar.sha256" \
  "$checksum_file" \
  --region "$deployment_region" \
  --only-show-errors

expected_sha="$(tr -d '[:space:]' < "$checksum_file")"
actual_sha="$(sha256sum "$new_jar" | awk '{print $1}')"
test -n "$expected_sha"
test "$actual_sha" = "$expected_sha"

jar tf "$new_jar" | grep -Fqx \
  "BOOT-INF/classes/com/track/subscription_service/notification/service/SesEventQueueWorker.class"
jar tf "$new_jar" | grep -Fqx \
  "BOOT-INF/classes/com/track/subscription_service/inboundemail/service/SesInboundQueueWorker.class"
jar tf "$new_jar" | grep -Fqx \
  "BOOT-INF/classes/db/migration/V16__add_inbound_email_security_verdicts.sql"

sudo rm -f "$previous_jar"
sudo cp "$current_jar" "$previous_jar"
replacement_started=true
sudo systemctl stop "$service_name"
sudo mv "$new_jar" "$current_jar"
sudo systemctl start "$service_name"

consecutive_healthy=0
for _ in {1..12}; do
  if sudo systemctl is-active --quiet "$service_name"; then
    http_code="$(curl \
      --silent \
      --output /dev/null \
      --write-out '%{http_code}' \
      --max-time 3 \
      http://127.0.0.1:8080/v3/api-docs || true)"
    if [[ "$http_code" != "000" ]]; then
      consecutive_healthy=$((consecutive_healthy + 1))
      if [[ "$consecutive_healthy" -ge 3 ]]; then
        break
      fi
    else
      consecutive_healthy=0
    fi
  else
    consecutive_healthy=0
  fi
  sleep 5
done

test "$consecutive_healthy" -ge 3
test "$(sha256sum "$current_jar" | awk '{print $1}')" = "$expected_sha"
jar tf "$current_jar" | grep -Fqx \
  "BOOT-INF/classes/com/track/subscription_service/notification/service/SesEventQueueWorker.class"
jar tf "$current_jar" | grep -Fqx \
  "BOOT-INF/classes/com/track/subscription_service/inboundemail/service/SesInboundQueueWorker.class"

trap - ERR
sudo rm -f "$previous_jar" "$checksum_file"
echo "Deployment verified: service active, HTTP responsive, checksum matched, SES workers present."
