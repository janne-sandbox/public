#!/usr/bin/env bash
# build-all.sh — build one or more Math2 components.
#
# Usage:
#   ./build-all.sh                          build everything
#   ./build-all.sh java-api cpp-api         build specific targets
#   ./build-all.sh --help                   show this help
#
# Targets:
#   java              All Java modules (java-api + java-rest-service) via mvn package
#   java-api          Java calculation library only
#   java-rest-service Spring Boot REST service only (includes java-api via -am)
#   cpp-api           C++ client library  (requires cmake)
#   rust-api          Rust client library (requires cargo)
#   dotnet-api        .NET client library (requires dotnet)
#   js-api            Install JavaScript client dependencies (requires npm)
#   ts-api            Install TypeScript client dependencies (requires npm)
#   python-api        Build the Python client wheel (requires Python 3 and pip)
#   all               Explicit alias for building everything

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Prefer the Homebrew-managed .NET 8 SDK when present (matches test-client-integration.sh)
DOTNET8_BIN="/opt/homebrew/opt/dotnet@8/bin/dotnet"
DOTNET8_ROOT="/opt/homebrew/opt/dotnet@8/libexec"

_dotnet() {
    if [[ -x "$DOTNET8_BIN" ]]; then
        DOTNET_ROOT="$DOTNET8_ROOT" "$DOTNET8_BIN" "$@"
    elif command -v dotnet &>/dev/null; then
        dotnet "$@"
    else
        echo "  SKIPPED: dotnet not found"
    fi
}

# ---- per-target build functions ----

build_java() {
    echo "=== java (all Java modules) ==="
    mvn package -q
}

build_java_api() {
    echo "=== java-api ==="
    mvn package -q -pl java-api
}

build_java_rest_service() {
    echo "=== java-rest-service ==="
    mvn package -q -pl java-rest-service -am
}

build_cpp_api() {
    echo "=== cpp-api ==="
    if ! command -v cmake &>/dev/null; then
        echo "  SKIPPED: cmake not found"
        return
    fi
    cmake -S "$SCRIPT_DIR/cpp-api" -B "$SCRIPT_DIR/cpp-api/build" \
        -DCMAKE_BUILD_TYPE=Release -Wno-dev >/dev/null
    cmake --build "$SCRIPT_DIR/cpp-api/build" --parallel
}

build_rust_api() {
    echo "=== rust-api ==="
    if ! command -v cargo &>/dev/null; then
        echo "  SKIPPED: cargo not found"
        return
    fi
    cargo build --manifest-path "$SCRIPT_DIR/rust-api/Cargo.toml"
}

build_dotnet_api() {
    echo "=== dotnet-api ==="
    _dotnet build "$SCRIPT_DIR/dotnet-api/Math2/Math2.csproj" --nologo -v q
}

build_js_api() {
    echo "=== js-api ==="
    if ! command -v npm &>/dev/null; then
        echo "  SKIPPED: npm not found"
        return
    fi
    (cd "$SCRIPT_DIR/js-api" && npm install --silent)
}

build_ts_api() {
    echo "=== ts-api ==="
    if ! command -v npm &>/dev/null; then
        echo "  SKIPPED: npm not found"
        return
    fi
    (cd "$SCRIPT_DIR/ts-api" && npm install --silent)
}

build_python_api() {
    echo "=== python-api ==="
    local python_command
    if command -v python3 &>/dev/null; then
        python_command=python3
    elif command -v python &>/dev/null; then
        python_command=python
    else
        echo "  SKIPPED: Python 3 not found"
        return
    fi
    if ! "$python_command" -m pip --version >/dev/null 2>&1; then
        echo "  SKIPPED: pip is not available for $python_command"
        return
    fi
    "$python_command" -m pip wheel --no-deps \
        --wheel-dir "$SCRIPT_DIR/python-api/dist" "$SCRIPT_DIR/python-api"
}

# ---- argument parsing ----

usage() {
    sed -n '2,/^set -euo/p' "$0" | grep '^#' | sed 's/^# \{0,1\}//'
}

VALID_TARGETS=(java java-api java-rest-service cpp-api rust-api dotnet-api js-api ts-api python-api all)
# Default order when building everything (java builds both Java modules at once)
DEFAULT_ORDER=(java cpp-api rust-api dotnet-api js-api ts-api python-api)

if [[ $# -eq 0 ]]; then
    REQUESTED=("${DEFAULT_ORDER[@]}")
else
    REQUESTED=()
    for arg in "$@"; do
        case "$arg" in
            -h|--help|help)
                usage
                exit 0
                ;;
            all)
                REQUESTED=("${DEFAULT_ORDER[@]}")
                break
                ;;
            java|java-api|java-rest-service|cpp-api|rust-api|dotnet-api|js-api|ts-api|python-api)
                REQUESTED+=("$arg")
                ;;
            *)
                echo "Unknown target: '$arg'" >&2
                echo "Valid targets: ${VALID_TARGETS[*]}" >&2
                exit 1
                ;;
        esac
    done
fi

# ---- execute ----

for target in "${REQUESTED[@]}"; do
    case "$target" in
        java)              build_java ;;
        java-api)          build_java_api ;;
        java-rest-service) build_java_rest_service ;;
        cpp-api)           build_cpp_api ;;
        rust-api)          build_rust_api ;;
        dotnet-api)        build_dotnet_api ;;
        js-api)            build_js_api ;;
        ts-api)            build_ts_api ;;
        python-api)        build_python_api ;;
    esac
done

echo "Done."
