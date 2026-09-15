#!/usr/bin/env bash
# clean-all.sh — remove every build output that mvn clean does not handle.
# Run this before git init / git add, or whenever you want a completely fresh tree.
set -euo pipefail
cd "$(dirname "$0")"

echo "=== Maven (java-api + java-rest-service) ==="
mvn clean -q

echo "=== C++ (CMake) ==="
rm -rf cpp-api/build/

echo "=== Rust (Cargo) ==="
if command -v cargo &>/dev/null; then
    cargo clean --manifest-path rust-api/Cargo.toml -q
else
    rm -rf rust-api/target/
fi

echo "=== .NET ==="
if command -v dotnet &>/dev/null; then
    dotnet clean dotnet-api/Math2/Math2.csproj         --nologo -v q 2>/dev/null || true
    dotnet clean dotnet-api/Math2.Tests/Math2.Tests.csproj --nologo -v q 2>/dev/null || true
else
    rm -rf dotnet-api/Math2/bin/ dotnet-api/Math2/obj/
    rm -rf dotnet-api/Math2.Tests/bin/ dotnet-api/Math2.Tests/obj/
fi

echo "=== JavaScript / TypeScript (npm) ==="
rm -rf js-api/node_modules ts-api/node_modules

echo "=== Python ==="
find python-api -type d -name __pycache__ -prune -exec rm -rf {} +
rm -rf python-api/build/ python-api/dist/ python-api/src/*.egg-info/

echo "=== Runtime state (.math2 H2 cache) ==="
rm -rf .math2/

echo "All build outputs removed."
