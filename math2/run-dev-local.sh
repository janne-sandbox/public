#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
STATE_DIR="$SCRIPT_DIR/.math2"
PID_FILE="$STATE_DIR/math2.pid"
LOG_FILE="$STATE_DIR/math2.log"
JAR_FILE="$SCRIPT_DIR/java-rest-service/target/math2-rest-service-0.1.0-SNAPSHOT.jar"

mkdir -p "$STATE_DIR"

if [[ -f "$PID_FILE" ]]; then
  EXISTING_PID="$(tr -d '[:space:]' < "$PID_FILE")"
  if [[ "$EXISTING_PID" =~ ^[0-9]+$ ]] && kill -0 "$EXISTING_PID" 2>/dev/null; then
    echo "Math2 is already running with PID $EXISTING_PID"
    exit 0
  fi
  rm -f "$PID_FILE"
fi

cd "$SCRIPT_DIR"
mvn -q -pl java-rest-service -am package -DskipTests

java -jar "$JAR_FILE" >"$LOG_FILE" 2>&1 &
SERVICE_PID=$!
echo "$SERVICE_PID" > "$PID_FILE"

for ATTEMPT in $(seq 1 60); do
  if curl --fail --silent "http://127.0.0.1:${MATH2_SERVER_PORT:-8080}/actuator/health" >/dev/null; then
    echo "Math2 started with PID $SERVICE_PID"
    echo "Swagger UI: http://127.0.0.1:${MATH2_SERVER_PORT:-8080}/swagger-ui.html"
    exit 0
  fi
  if ! kill -0 "$SERVICE_PID" 2>/dev/null; then
    echo "Math2 exited during startup. See $LOG_FILE" >&2
    rm -f "$PID_FILE"
    exit 1
  fi
  sleep 1
done

echo "Math2 did not become healthy within 60 seconds. See $LOG_FILE" >&2
kill "$SERVICE_PID" 2>/dev/null || true
rm -f "$PID_FILE"
exit 1
