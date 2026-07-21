#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASE_URL="http://127.0.0.1:${MATH2_SERVER_PORT:-8080}"
STARTED_HERE=false

cleanup() {
  if [[ "$STARTED_HERE" == true ]]; then
    "$SCRIPT_DIR/stop-dev-local.sh"
  fi
}
trap cleanup EXIT

if ! curl --fail --silent "$BASE_URL/actuator/health" >/dev/null; then
  "$SCRIPT_DIR/run-dev-local.sh"
  STARTED_HERE=true
fi

SINGLE_RESULT="$(curl --fail --silent --get \
  --data-urlencode 'equation=sqrt(9) + 2 ^ 3' \
  "$BASE_URL/api/v1/calculate")"
BATCH_RESULT="$(curl --fail --silent --get \
  --data-urlencode 'equations=1+1,max(2,3),sqrt(16)' \
  "$BASE_URL/api/v1/calculate/batch")"

[[ "$SINGLE_RESULT" == '{"result":"11"}' ]]
[[ "$BATCH_RESULT" == '{"results":["2","3","4"]}' ]]
echo "Math2 REST smoke test passed"
