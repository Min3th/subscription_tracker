#!/usr/bin/env bash
set -Eeuo pipefail
set +x

if [[ $# -ne 5 ]]; then
  echo "Usage: install-runtime-from-s3.sh <deployment-bucket> <aws-region> <db-secret-arn> <application-secret-id> <runtime-parameter-name>" >&2
  exit 2
fi

if [[ $EUID -ne 0 ]]; then
  echo "Runtime installation must run as root." >&2
  exit 1
fi

readonly deployment_bucket="$1"
readonly aws_region="$2"
readonly db_secret_arn="$3"
readonly application_secret_id="$4"
readonly runtime_parameter_name="$5"
readonly archive_name="runtime-assets.tar.gz"
readonly checksum_name="runtime-assets.tar.gz.sha256"
readonly s3_prefix="runtime"

if [[ ! "$deployment_bucket" =~ ^[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]$ ]]; then
  echo "Deployment bucket name is invalid." >&2
  exit 1
fi
if [[ ! "$aws_region" =~ ^[a-z]{2}(-gov)?-[a-z]+-[0-9]+$ ]]; then
  echo "AWS region is invalid." >&2
  exit 1
fi

for command_name in aws bash sha256sum sort tar; do
  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "Required command is unavailable: $command_name" >&2
    exit 1
  fi
done

temporary_dir="$(mktemp -d)"
cleanup() {
  if [[ -n "${temporary_dir:-}" && -d "$temporary_dir" ]]; then
    rm -rf -- "$temporary_dir"
  fi
}
trap cleanup EXIT

aws s3 cp \
  "s3://$deployment_bucket/$s3_prefix/$archive_name" \
  "$temporary_dir/$archive_name" \
  --region "$aws_region" \
  --only-show-errors
aws s3 cp \
  "s3://$deployment_bucket/$s3_prefix/$checksum_name" \
  "$temporary_dir/$checksum_name" \
  --region "$aws_region" \
  --only-show-errors

checksum_line="$(tr -d '\r' <"$temporary_dir/$checksum_name")"
if [[ ! "$checksum_line" =~ ^([[:xdigit:]]{64})[[:space:]]+[*]?runtime-assets\.tar\.gz$ ]]; then
  echo "Runtime archive checksum file has an invalid format." >&2
  exit 1
fi

expected_sha="${BASH_REMATCH[1],,}"
actual_sha="$(sha256sum "$temporary_dir/$archive_name" | awk '{print $1}')"
if [[ "$actual_sha" != "$expected_sha" ]]; then
  echo "Runtime archive checksum verification failed." >&2
  exit 1
fi

expected_members="$({
  printf '%s\n' \
    '.github/scripts/configure-backend-runtime.sh' \
    '.github/scripts/run-backend.sh' \
    '.github/systemd/subscription-tracker.service'
} | LC_ALL=C sort)"
actual_members="$(tar -tzf "$temporary_dir/$archive_name" | LC_ALL=C sort)"

if [[ "$actual_members" != "$expected_members" ]]; then
  echo "Runtime archive contains unexpected or missing files." >&2
  exit 1
fi

tar \
  --extract \
  --gzip \
  --file "$temporary_dir/$archive_name" \
  --directory "$temporary_dir" \
  --no-same-owner \
  --no-same-permissions

readonly configure_script="$temporary_dir/.github/scripts/configure-backend-runtime.sh"
readonly launcher_script="$temporary_dir/.github/scripts/run-backend.sh"
readonly service_unit="$temporary_dir/.github/systemd/subscription-tracker.service"

for extracted_file in "$configure_script" "$launcher_script" "$service_unit"; do
  if [[ ! -f "$extracted_file" || -L "$extracted_file" ]]; then
    echo "Runtime archive contains an invalid file type." >&2
    exit 1
  fi
done

bash "$configure_script" \
  "$aws_region" \
  "$db_secret_arn" \
  "$application_secret_id" \
  "$runtime_parameter_name"

systemctl is-enabled --quiet subscription-tracker.service
test -x /usr/local/bin/run-subtrak
test -r /etc/subtrak/runtime-identifiers

echo "Runtime assets installed and subscription-tracker.service enabled."
