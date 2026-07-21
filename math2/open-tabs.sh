#!/usr/bin/env bash
set -euo pipefail

URL="http://127.0.0.1:${MATH2_SERVER_PORT:-8080}/swagger-ui.html"

if command -v open >/dev/null 2>&1; then
  open "$URL"
elif command -v xdg-open >/dev/null 2>&1; then
  xdg-open "$URL"
else
  echo "Open $URL in a browser"
  exit 1
fi
