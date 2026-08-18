#!/usr/bin/env bash
set -Eeuo pipefail

command_name="$(basename "$0")"

case "$command_name" in
  aws)
    if [[ "$1" != "s3" || "$2" != "cp" ]]; then
      echo "Unexpected mocked AWS invocation." >&2
      exit 1
    fi

    case "$3" in
      */subscription-service.jar)
        cp "$MOCK_ARTIFACT_JAR" "$4"
        ;;
      */subscription-service.jar.sha256)
        cp "$MOCK_ARTIFACT_CHECKSUM" "$4"
        ;;
      *)
        echo "Unexpected mocked S3 key: $3" >&2
        exit 1
        ;;
    esac
    ;;
  chown | chmod | sleep)
    exit 0
    ;;
  curl)
    if [[ "$MOCK_HEALTH_RESULT" == "healthy" ]]; then
      printf '200'
    else
      printf '000'
    fi
    ;;
  install)
    source_file="${@: -2:1}"
    target_file="${@: -1}"
    cp "$source_file" "$target_file"
    ;;
  jar)
    printf '%s\n' \
      'BOOT-INF/classes/com/track/subscription_service/notification/service/SesEventQueueWorker.class' \
      'BOOT-INF/classes/com/track/subscription_service/inboundemail/service/SesInboundQueueWorker.class' \
      'BOOT-INF/classes/db/migration/V16__add_inbound_email_security_verdicts.sql'
    ;;
  sudo)
    exec "$@"
    ;;
  systemctl)
    action="$1"
    printf '%s\n' "$*" >>"$MOCK_SYSTEMCTL_LOG"

    case "$action" in
      start)
        printf 'active\n' >"$MOCK_SERVICE_STATE"
        ;;
      stop)
        printf 'inactive\n' >"$MOCK_SERVICE_STATE"
        ;;
      is-active)
        grep -Fqx 'active' "$MOCK_SERVICE_STATE"
        ;;
      is-enabled)
        exit 0
        ;;
      *)
        echo "Unexpected mocked systemctl action: $action" >&2
        exit 1
        ;;
    esac
    ;;
  *)
    echo "Unexpected mock command name: $command_name" >&2
    exit 1
    ;;
esac
