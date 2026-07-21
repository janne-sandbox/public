#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PORT="${MATH2_CLIENT_TEST_PORT:-18080}"
BASE_URL="http://127.0.0.1:$PORT"
JAR_FILE="$SCRIPT_DIR/java-rest-service/target/math2-rest-service-0.1.0-SNAPSHOT.jar"
LOG_FILE="$(mktemp -t math2-client-integration.XXXXXX.log)"

cleanup() {
  if [[ -n "${SERVICE_PID:-}" ]] && kill -0 "$SERVICE_PID" 2>/dev/null; then
    kill "$SERVICE_PID" 2>/dev/null || true
    wait "$SERVICE_PID" 2>/dev/null || true
  fi
  rm -f "$LOG_FILE"
}
trap cleanup EXIT

if [[ ! -f "$JAR_FILE" ]]; then
  (cd "$SCRIPT_DIR" && mvn -q -pl java-rest-service -am package -DskipTests)
fi

MATH2_SERVER_ADDRESS=127.0.0.1 \
MATH2_SERVER_PORT="$PORT" \
MATH2_CACHE_ENABLED=false \
MATH2_MAX_LENGTH=1200 \
java -jar "$JAR_FILE" >"$LOG_FILE" 2>&1 &
SERVICE_PID=$!

for attempt in $(seq 1 60); do
  if curl --fail --silent "$BASE_URL/actuator/health" >/dev/null; then
    break
  fi
  if ! kill -0 "$SERVICE_PID" 2>/dev/null; then
    echo "Math2 client integration service exited during startup" >&2
    tail -80 "$LOG_FILE" >&2
    exit 1
  fi
  if [[ "$attempt" -eq 60 ]]; then
    echo "Math2 client integration service did not become healthy" >&2
    tail -80 "$LOG_FILE" >&2
    exit 1
  fi
  sleep 1
done

export MATH2_TEST_BASE_URL="$BASE_URL"

(cd "$SCRIPT_DIR/js-api" && npm test)
(cd "$SCRIPT_DIR/ts-api" && npm test)

cmake -S "$SCRIPT_DIR/cpp-api" -B "$SCRIPT_DIR/cpp-api/build"
cmake --build "$SCRIPT_DIR/cpp-api/build"
ctest --test-dir "$SCRIPT_DIR/cpp-api/build" --output-on-failure

DOTNET8_BIN="/opt/homebrew/opt/dotnet@8/bin/dotnet"
DOTNET8_ROOT="/opt/homebrew/opt/dotnet@8/libexec"
if [[ -x "$DOTNET8_BIN" ]]; then
  DOTNET_ROOT="$DOTNET8_ROOT" "$DOTNET8_BIN" test \
    "$SCRIPT_DIR/dotnet-api/Math2.Tests/Math2.Tests.csproj"
elif command -v dotnet >/dev/null 2>&1; then
  dotnet test "$SCRIPT_DIR/dotnet-api/Math2.Tests/Math2.Tests.csproj"
else
  echo "SKIPPED: .NET live client test (dotnet is not installed)"
fi

if command -v cargo >/dev/null 2>&1; then
  (cd "$SCRIPT_DIR/rust-api" && cargo test)
else
  echo "SKIPPED: Rust live client test (cargo is not installed)"
fi

echo "Math2 live REST client integration tests passed"
