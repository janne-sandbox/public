#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PID_FILE="$SCRIPT_DIR/.math2/math2.pid"

if [[ ! -f "$PID_FILE" ]]; then
  echo "Math2 is not running (no PID file)"
  exit 0
fi

SERVICE_PID="$(tr -d '[:space:]' < "$PID_FILE")"
if [[ ! "$SERVICE_PID" =~ ^[0-9]+$ ]]; then
  echo "Refusing to use invalid PID file: $PID_FILE" >&2
  exit 1
fi

if ! kill -0 "$SERVICE_PID" 2>/dev/null; then
  rm -f "$PID_FILE"
  echo "Removed stale Math2 PID file"
  exit 0
fi

COMMAND="$(ps -p "$SERVICE_PID" -o command=)"
if [[ "$COMMAND" != *"math2-rest-service"* ]]; then
  echo "Refusing to stop PID $SERVICE_PID because it is not the Math2 service" >&2
  exit 1
fi

kill "$SERVICE_PID"
for ATTEMPT in $(seq 1 20); do
  if ! kill -0 "$SERVICE_PID" 2>/dev/null; then
    rm -f "$PID_FILE"
    echo "Math2 stopped"
    exit 0
  fi
  sleep 1
done

echo "Math2 did not stop within 20 seconds; PID $SERVICE_PID is still running" >&2
exit 1
