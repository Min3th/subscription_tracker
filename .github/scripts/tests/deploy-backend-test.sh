#!/usr/bin/env bash
set -Eeuo pipefail

readonly test_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
readonly scripts_dir="$(cd -- "$test_dir/.." && pwd)"
readonly deploy_script="$scripts_dir/deploy-backend.sh"
readonly mock_dispatcher="$test_dir/mock-deploy-command.sh"
readonly test_root="$(mktemp -d)"

cleanup() {
  if [[ -n "${test_root:-}" && -d "$test_root" ]]; then
    rm -rf -- "$test_root"
  fi
}
trap cleanup EXIT

fail() {
  echo "FAIL: $*" >&2
  exit 1
}

assert_equals() {
  local expected="$1"
  local actual="$2"
  local description="$3"

  if [[ "$actual" != "$expected" ]]; then
    fail "$description (expected '$expected', got '$actual')"
  fi
}

assert_file_absent() {
  local path="$1"
  local description="$2"

  if [[ -e "$path" ]]; then
    fail "$description"
  fi
}

assert_output_contains() {
  local output="$1"
  local expected="$2"

  if [[ "$output" != *"$expected"* ]]; then
    fail "output did not contain: $expected"
  fi
}

create_mock_bin() {
  local mock_bin="$1"
  local command_name

  mkdir -p "$mock_bin"
  for command_name in aws chown chmod curl install jar sleep sudo systemctl; do
    cp "$mock_dispatcher" "$mock_bin/$command_name"
    chmod +x "$mock_bin/$command_name"
  done
}

run_deployment() {
  local case_dir="$1"
  local health_result="$2"
  local output_file="$3"

  PATH="$case_dir/mock-bin:$PATH" \
    MOCK_ARTIFACT_JAR="$case_dir/artifact.jar" \
    MOCK_ARTIFACT_CHECKSUM="$case_dir/artifact.jar.sha256" \
    MOCK_HEALTH_RESULT="$health_result" \
    MOCK_SERVICE_STATE="$case_dir/service-state" \
    MOCK_SYSTEMCTL_LOG="$case_dir/systemctl.log" \
    SUBTRAK_APP_DIR="$case_dir/application" \
    bash "$deploy_script" test-deployment-bucket ap-south-1 \
    >"$output_file" 2>&1
}

prepare_case() {
  local case_name="$1"
  local existing_jar="${2:-}"
  local case_dir="$test_root/$case_name"

  mkdir -p "$case_dir/application"
  create_mock_bin "$case_dir/mock-bin"
  printf 'new-application-jar\n' >"$case_dir/artifact.jar"
  sha256sum "$case_dir/artifact.jar" | awk '{print $1}' \
    >"$case_dir/artifact.jar.sha256"
  printf 'inactive\n' >"$case_dir/service-state"
  : >"$case_dir/systemctl.log"

  if [[ -n "$existing_jar" ]]; then
    printf '%s\n' "$existing_jar" \
      >"$case_dir/application/subscription-service.jar"
  fi

  printf '%s' "$case_dir"
}

test_first_deployment_succeeds() {
  local case_dir output status
  case_dir="$(prepare_case first-success)"

  set +e
  run_deployment "$case_dir" healthy "$case_dir/output.log"
  status=$?
  set -e
  output="$(<"$case_dir/output.log")"

  assert_equals 0 "$status" "first deployment should succeed"
  assert_equals 'new-application-jar' \
    "$(<"$case_dir/application/subscription-service.jar")" \
    "first deployment should install the new JAR"
  assert_file_absent \
    "$case_dir/application/subscription-service.jar.previous" \
    "first deployment should not retain a backup"
  assert_output_contains "$output" \
    'No existing JAR found; performing first deployment.'
  assert_output_contains "$output" 'Deployment verified:'
}

test_upgrade_succeeds() {
  local case_dir output status
  case_dir="$(prepare_case upgrade-success old-application-jar)"

  set +e
  run_deployment "$case_dir" healthy "$case_dir/output.log"
  status=$?
  set -e
  output="$(<"$case_dir/output.log")"

  assert_equals 0 "$status" "upgrade should succeed"
  assert_equals 'new-application-jar' \
    "$(<"$case_dir/application/subscription-service.jar")" \
    "upgrade should retain the new JAR"
  assert_file_absent \
    "$case_dir/application/subscription-service.jar.previous" \
    "successful upgrade should remove the backup"
  assert_output_contains "$output" 'Existing JAR backed up.'
}

test_first_deployment_failure_removes_jar() {
  local case_dir output status
  case_dir="$(prepare_case first-failure)"

  set +e
  run_deployment "$case_dir" unhealthy "$case_dir/output.log"
  status=$?
  set -e
  output="$(<"$case_dir/output.log")"

  if [[ "$status" -eq 0 ]]; then
    fail "failed first deployment should return nonzero"
  fi
  assert_file_absent \
    "$case_dir/application/subscription-service.jar" \
    "failed first deployment should remove the new JAR"
  assert_equals inactive "$(<"$case_dir/service-state")" \
    "failed first deployment should leave the service stopped"
  assert_output_contains "$output" 'Failed first-deployment JAR removed.'
}

test_upgrade_failure_restores_previous_jar() {
  local case_dir output status
  case_dir="$(prepare_case upgrade-failure old-application-jar)"

  set +e
  run_deployment "$case_dir" unhealthy "$case_dir/output.log"
  status=$?
  set -e
  output="$(<"$case_dir/output.log")"

  if [[ "$status" -eq 0 ]]; then
    fail "failed upgrade should return nonzero"
  fi
  assert_equals 'old-application-jar' \
    "$(<"$case_dir/application/subscription-service.jar")" \
    "failed upgrade should restore the previous JAR"
  assert_equals active "$(<"$case_dir/service-state")" \
    "failed upgrade should restart the restored service"
  assert_file_absent \
    "$case_dir/application/subscription-service.jar.previous" \
    "failed upgrade should consume the backup during restoration"
  assert_output_contains "$output" 'Previous JAR restored.'
}

test_first_deployment_succeeds
test_upgrade_succeeds
test_first_deployment_failure_removes_jar
test_upgrade_failure_restores_previous_jar

echo 'PASS: deploy-backend.sh first deployment and rollback scenarios'
