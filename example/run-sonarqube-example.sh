#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${SCRIPT_DIR}"

SONAR_CONTAINER_NAME="${SONAR_CONTAINER_NAME:-example-sonarqube}"
SONAR_IMAGE="${SONAR_IMAGE:-sonarqube:lts-community}"
SONAR_SCANNER_IMAGE="${SONAR_SCANNER_IMAGE:-sonarsource/sonar-scanner-cli:latest}"
SONAR_PORT="${SONAR_PORT:-9000}"
SONAR_BIND_ADDRESS="${SONAR_BIND_ADDRESS:-127.0.0.1}"
SONAR_HOST_URL="${SONAR_HOST_URL:-http://host.docker.internal:${SONAR_PORT}}"
SONAR_BROWSER_URL="${SONAR_BROWSER_URL:-http://localhost:${SONAR_PORT}}"
SONAR_PROJECT_KEY="${SONAR_PROJECT_KEY:-example}"
SONAR_PROJECT_NAME="${SONAR_PROJECT_NAME:-Example}"
SONAR_PROJECT_VERSION="${SONAR_PROJECT_VERSION:-local-$(date +%Y%m%d%H%M%S)}"
SONAR_LOGIN="${SONAR_LOGIN:-admin}"
SONAR_PASSWORD="${SONAR_PASSWORD:-admin2}"
SONAR_OPEN_BROWSER="${SONAR_OPEN_BROWSER:-true}"
SONAR_ALLOW_ANONYMOUS_LOCAL="${SONAR_ALLOW_ANONYMOUS_LOCAL:-true}"
SONAR_RUN_COMPILE="${SONAR_RUN_COMPILE:-true}"
SONAR_MAVEN_ARGS="${SONAR_MAVEN_ARGS:--q -DskipTests compile}"
SONAR_RESULTS_ROOT="${SONAR_RESULTS_ROOT:-${SCRIPT_DIR}/review/sonarqube}"
SONAR_WAIT_TIMEOUT_SECONDS="${SONAR_WAIT_TIMEOUT_SECONDS:-900}"
SONAR_QUALITY_GATE_TIMEOUT_SECONDS="${SONAR_QUALITY_GATE_TIMEOUT_SECONDS:-900}"
SONAR_DOCKER_PLATFORM_ARGS="${SONAR_DOCKER_PLATFORM_ARGS:-}"

TIMESTAMP="$(date +%Y%m%d%H%M%S)"
RUN_DIR="${SONAR_RESULTS_ROOT}/${TIMESTAMP}"
TASK_METADATA_FILE="${RUN_DIR}/report-task.txt"
CONTAINER_TASK_METADATA_FILE="/usr/src/review/sonarqube/${TIMESTAMP}/report-task.txt"
CONTAINER_WORKING_DIRECTORY="review/sonarqube/${TIMESTAMP}/.scannerwork"
SCANNER_LOG_FILE="${RUN_DIR}/scanner.log"
SUMMARY_FILE="${RUN_DIR}/summary.md"
ISSUES_JSON_FILE="${RUN_DIR}/issues.json"
MEASURES_JSON_FILE="${RUN_DIR}/measures.json"
QUALITY_GATE_JSON_FILE="${RUN_DIR}/quality-gate.json"

mkdir -p "${RUN_DIR}"
mkdir -p "${RUN_DIR}/.scannerwork"

log() {
  printf '[sonarqube] %s\n' "$*"
}

fail() {
  printf '[sonarqube] ERROR: %s\n' "$*" >&2
  exit 1
}

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || fail "Required command not found: $1"
}

join_by_comma() {
  local IFS=,
  printf '%s' "$*"
}

wait_for_http_ok() {
  local url="$1"
  local timeout_seconds="$2"
  local start_ts
  start_ts="$(date +%s)"

  while true; do
    local http_code
    http_code="$(curl -s -o /dev/null -w '%{http_code}' "${url}" || true)"
    if [[ "${http_code}" == "200" ]]; then
      return 0
    fi

    if (( $(date +%s) - start_ts >= timeout_seconds )); then
      return 1
    fi

    sleep 5
  done
}

wait_for_sonarqube_ready() {
  local start_ts
  start_ts="$(date +%s)"

  while true; do
    local status
    status="$(curl -s "${SONAR_BROWSER_URL}/api/system/status" | sed -n 's/.*"status":"\([A-Z_]*\)".*/\1/p' | head -n 1 || true)"
    if [[ "${status}" == "UP" ]]; then
      return 0
    fi

    if (( $(date +%s) - start_ts >= SONAR_WAIT_TIMEOUT_SECONDS )); then
      return 1
    fi

    sleep 5
  done
}

ensure_sonarqube_container() {
  local existing_id
  existing_id="$(docker ps -aq -f name="^${SONAR_CONTAINER_NAME}$")"

  if [[ -n "${existing_id}" ]]; then
    if [[ -z "$(docker ps -q -f name="^${SONAR_CONTAINER_NAME}$")" ]]; then
      log "Starting existing SonarQube container ${SONAR_CONTAINER_NAME}"
      docker start "${SONAR_CONTAINER_NAME}" >/dev/null
    else
      log "Using running SonarQube container ${SONAR_CONTAINER_NAME}"
    fi
    return 0
  fi

  log "Creating SonarQube container ${SONAR_CONTAINER_NAME}"
  docker run -d \
    ${SONAR_DOCKER_PLATFORM_ARGS} \
    --name "${SONAR_CONTAINER_NAME}" \
    -p "${SONAR_BIND_ADDRESS}:${SONAR_PORT}:9000" \
    -v "${SONAR_CONTAINER_NAME}-data:/opt/sonarqube/data" \
    -v "${SONAR_CONTAINER_NAME}-logs:/opt/sonarqube/logs" \
    -v "${SONAR_CONTAINER_NAME}-extensions:/opt/sonarqube/extensions" \
    "${SONAR_IMAGE}" >/dev/null
}

configure_local_anonymous_browse() {
  if [[ "${SONAR_ALLOW_ANONYMOUS_LOCAL}" != "true" ]]; then
    return 0
  fi

  log "Enabling anonymous browse for the localhost-only SonarQube instance"
  curl -fsS -u "${SONAR_LOGIN}:${SONAR_PASSWORD}" \
    -X POST "${SONAR_BROWSER_URL}/api/settings/set" \
    -d 'key=sonar.forceAuthentication' \
    -d 'value=false' >/dev/null
}

generate_token() {
  local token_name="example-${TIMESTAMP}"
  local response token
  response="$(curl -s -u "${SONAR_LOGIN}:${SONAR_PASSWORD}" -X POST "${SONAR_BROWSER_URL}/api/user_tokens/generate" --data-urlencode "name=${token_name}" || true)"
  token="$(printf '%s' "${response}" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')"
  if [[ -z "${token}" ]]; then
    fail "Unable to generate SonarQube token. If the local admin password changed, rerun with SONAR_LOGIN and SONAR_PASSWORD set. Response: ${response}"
  fi
  printf '%s' "${token}"
}

collect_java_binaries() {
  find . \
    \( -path '*/target/classes' -o -path '*/target/test-classes' \) \
    -type d \
    -not -path '*/node_modules/*' \
    -not -path '*/.git/*' \
    | sed 's#^./##' \
    | paste -sd, -
}

build_exclusions() {
  join_by_comma \
    '**/.git/**' \
    '**/.idea/**' \
    '**/.vscode/**' \
    '**/.angular/**' \
    '**/node_modules/**' \
    '**/dist/**' \
    '**/build/**' \
    '**/coverage/**' \
    '**/target/**' \
    '**/.scannerwork/**' \
    '**/tmp/**' \
    '**/logs/**' \
    'documentation/**' \
    'materials/**' \
    'helm-charts/**/templates/**' \
    'review/results/**' \
    'review/sonarqube/**' \
    '**/Dockerfile' \
    '**/Dockerfile.*' \
    '**/docker-compose*.yml' \
    'docker/**' \
    'terraform/**/.terraform/**' \
    'run-reviews.sh/**' \
    'purge-data.sh/**'
}

build_test_inclusions() {
  join_by_comma \
    '**/src/test/**' \
    '**/*.spec.ts' \
    '**/*.spec.tsx' \
    '**/*.test.ts' \
    '**/*.test.tsx' \
    '**/*.spec.js' \
    '**/*.test.js'
}

collect_supported_tsconfigs() {
  find . \
    -name 'tsconfig.json' \
    -type f \
    -not -path '*/node_modules/*' \
    -not -path '*/.angular/*' \
    | while IFS= read -r tsconfig; do
        if grep -Eq '"moduleResolution"[[:space:]]*:[[:space:]]*"bundler"' "${tsconfig}"; then
          continue
        fi
        printf '%s\n' "${tsconfig#./}"
      done \
    | paste -sd, -
}

write_summary() {
  local dashboard_url="$1"
  local quality_status="$2"
  cat > "${SUMMARY_FILE}" <<EOF
# SonarQube Review Summary

- Run directory: ${RUN_DIR}
- Project key: ${SONAR_PROJECT_KEY}
- Project version: ${SONAR_PROJECT_VERSION}
- Dashboard: ${dashboard_url}
- Quality gate: ${quality_status}
- Scanner log: ${SCANNER_LOG_FILE}
- Issues export: ${ISSUES_JSON_FILE}
- Measures export: ${MEASURES_JSON_FILE}
- Quality gate export: ${QUALITY_GATE_JSON_FILE}

## Next step

Open the dashboard and review issues by severity, security hotspots, duplications, and code smells.
EOF
}

open_browser() {
  local url="$1"
  if [[ "${SONAR_OPEN_BROWSER}" != "true" ]]; then
    return 0
  fi

  if command -v open >/dev/null 2>&1; then
    open "${url}" >/dev/null 2>&1 || true
  elif command -v xdg-open >/dev/null 2>&1; then
    xdg-open "${url}" >/dev/null 2>&1 || true
  fi
}

to_browser_url() {
  local url="$1"
  if [[ -z "${url}" ]]; then
    printf '%s' "${SONAR_BROWSER_URL}/dashboard?id=${SONAR_PROJECT_KEY}"
    return 0
  fi

  if [[ "${url}" == *"host.docker.internal"* ]]; then
    printf '%s' "${url//host.docker.internal/localhost}"
    return 0
  fi

  printf '%s' "${url}"
}

main() {
  require_cmd docker
  require_cmd curl
  require_cmd sed
  require_cmd find
  require_cmd paste

  if [[ "${SONAR_RUN_COMPILE}" == "true" ]]; then
    require_cmd mvn
  fi

  log "Results will be written to ${RUN_DIR}"
  ensure_sonarqube_container

  log "Waiting for SonarQube HTTP endpoint on ${SONAR_BROWSER_URL}"
  wait_for_http_ok "${SONAR_BROWSER_URL}/api/system/status" "${SONAR_WAIT_TIMEOUT_SECONDS}" || fail "SonarQube HTTP endpoint did not respond in time"

  log "Waiting for SonarQube to report status UP"
  wait_for_sonarqube_ready || fail "SonarQube did not become ready in time"

  configure_local_anonymous_browse

  local sonar_token
  sonar_token="$(generate_token)"

  if [[ "${SONAR_RUN_COMPILE}" == "true" ]]; then
    log "Compiling Maven modules so Java analysis has bytecode"
    mvn ${SONAR_MAVEN_ARGS}
  else
    log "Skipping Maven compile because SONAR_RUN_COMPILE=false"
  fi

  local java_binaries
  java_binaries="$(collect_java_binaries)"
  if [[ -z "${java_binaries}" ]]; then
    fail "No Java binaries found. Either keep SONAR_RUN_COMPILE=true or compile the project before running the script."
  fi

  local sonar_exclusions sonar_test_inclusions supported_tsconfigs
  local -a scanner_extra_args=()
  sonar_exclusions="$(build_exclusions)"
  sonar_test_inclusions="$(build_test_inclusions)"
  supported_tsconfigs="$(collect_supported_tsconfigs)"
  if [[ -n "${supported_tsconfigs}" ]]; then
    scanner_extra_args+=("-Dsonar.typescript.tsconfigPaths=${supported_tsconfigs}")
  fi

  log "Running SonarScanner inside Docker"
  docker run --rm \
    ${SONAR_DOCKER_PLATFORM_ARGS} \
    -u "$(id -u):$(id -g)" \
    -e SONAR_HOST_URL="${SONAR_HOST_URL}" \
    -e SONAR_TOKEN="${sonar_token}" \
    -e SONAR_USER_HOME=/tmp/sonar-user-home \
    -v "${SCRIPT_DIR}:/usr/src" \
    -w /usr/src \
    "${SONAR_SCANNER_IMAGE}" \
    -Dsonar.projectKey="${SONAR_PROJECT_KEY}" \
    -Dsonar.projectName="${SONAR_PROJECT_NAME}" \
    -Dsonar.projectVersion="${SONAR_PROJECT_VERSION}" \
    -Dsonar.sourceEncoding=UTF-8 \
    -Dsonar.sources=. \
    -Dsonar.tests=. \
    -Dsonar.exclusions="${sonar_exclusions}" \
    -Dsonar.test.inclusions="${sonar_test_inclusions}" \
    -Dsonar.coverage.exclusions="${sonar_exclusions}" \
    -Dsonar.cpd.exclusions="documentation/**,materials/**" \
    -Dsonar.java.binaries="${java_binaries}" \
    -Dsonar.java.test.binaries="${java_binaries}" \
    -Dsonar.qualitygate.wait=true \
    -Dsonar.qualitygate.timeout="${SONAR_QUALITY_GATE_TIMEOUT_SECONDS}" \
    "${scanner_extra_args[@]}" \
    -Dsonar.working.directory="${CONTAINER_WORKING_DIRECTORY}" \
    -Dsonar.scanner.metadataFilePath="${CONTAINER_TASK_METADATA_FILE}" | tee "${SCANNER_LOG_FILE}"

  [[ -f "${TASK_METADATA_FILE}" ]] || fail "SonarScanner completed but metadata file was not created"

  local dashboard_url ce_task_url ce_task_id quality_status browser_dashboard_url browser_ce_task_url
  dashboard_url="$(sed -n 's/^dashboardUrl=//p' "${TASK_METADATA_FILE}" | head -n 1)"
  ce_task_url="$(sed -n 's/^ceTaskUrl=//p' "${TASK_METADATA_FILE}" | head -n 1)"
  ce_task_id="$(sed -n 's/^ceTaskId=//p' "${TASK_METADATA_FILE}" | head -n 1)"
  dashboard_url="${dashboard_url:-${SONAR_BROWSER_URL}/dashboard?id=${SONAR_PROJECT_KEY}}"
  browser_dashboard_url="$(to_browser_url "${dashboard_url}")"
  browser_ce_task_url="$(to_browser_url "${ce_task_url}")"

  curl -s -u "${SONAR_LOGIN}:${SONAR_PASSWORD}" "${SONAR_BROWSER_URL}/api/issues/search?componentKeys=${SONAR_PROJECT_KEY}&ps=500" > "${ISSUES_JSON_FILE}" || true
  curl -s -u "${SONAR_LOGIN}:${SONAR_PASSWORD}" "${SONAR_BROWSER_URL}/api/measures/component?component=${SONAR_PROJECT_KEY}&metricKeys=alert_status,bugs,vulnerabilities,code_smells,security_hotspots,coverage,duplicated_lines_density,ncloc" > "${MEASURES_JSON_FILE}" || true
  curl -s -u "${SONAR_LOGIN}:${SONAR_PASSWORD}" "${SONAR_BROWSER_URL}/api/qualitygates/project_status?projectKey=${SONAR_PROJECT_KEY}" > "${QUALITY_GATE_JSON_FILE}" || true

  quality_status="$(sed -n 's/.*"status":"\([A-Z]*\)".*/\1/p' "${QUALITY_GATE_JSON_FILE}" | head -n 1)"
  quality_status="${quality_status:-UNKNOWN}"

  write_summary "${browser_dashboard_url}" "${quality_status}"

  log "SonarQube review ready"
  log "Dashboard: ${browser_dashboard_url}"
  log "Summary: ${SUMMARY_FILE}"
  log "CE task URL: ${browser_ce_task_url:-unknown}"
  log "CE task ID: ${ce_task_id:-unknown}"
  log "Quality gate: ${quality_status}"

  open_browser "${browser_dashboard_url}"
}

main "$@"
