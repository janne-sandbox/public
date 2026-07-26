#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SOURCE="$SCRIPT_DIR/java-metal-backend/src/main/native/NativeMetalRuntime.m"
OUTPUT_DIR="$SCRIPT_DIR/java-metal-backend/target/native"
OUTPUT="$OUTPUT_DIR/libmath2_metal.dylib"

if [[ "$(uname -s)" != "Darwin" ]]; then
  echo "The Metal native bridge can only be built on macOS." >&2
  exit 1
fi
if ! command -v xcrun >/dev/null 2>&1; then
  echo "Xcode command-line tools are required." >&2
  exit 1
fi

JAVA_21_HOME="${MATH2_JAVA_HOME:-$(/usr/libexec/java_home -v 21)}"
if [[ ! -f "$JAVA_21_HOME/include/jni.h" ]]; then
  echo "Java 21 JNI headers were not found under $JAVA_21_HOME." >&2
  exit 1
fi

SDK_PATH="$(xcrun --sdk macosx --show-sdk-path)"
mkdir -p "$OUTPUT_DIR"

xcrun --sdk macosx clang \
  -dynamiclib \
  -fobjc-arc \
  -O2 \
  -Wall \
  -Wextra \
  -Werror \
  -isysroot "$SDK_PATH" \
  -I"$JAVA_21_HOME/include" \
  -I"$JAVA_21_HOME/include/darwin" \
  -framework Foundation \
  -framework Metal \
  "$SOURCE" \
  -o "$OUTPUT"

echo "Built Metal JNI bridge: $OUTPUT"
