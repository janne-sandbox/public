#!/bin/bash
# review.sh — Local AI-powered code review agent orchestrator
# Usage: ./run-review.sh [LANGUAGE=java|rust|react|typescript|cpp|csharp|all] [INTERNET=true|false] [DEEPSEEK=true|false]
# Example: ./review.sh LANGUAGE=typescript INTERNET=false DEEPSEEK=true
# Default: LANGUAGE=all, INTERNET=false, DEEPSEEK=true

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REVIEW_DIR="${SCRIPT_DIR}/review"
REQUIREMENTS_DIR="${SCRIPT_DIR}/requirements"
AGENTS_DIR="${REVIEW_DIR}/agents"
RESULTS_DIR="${REVIEW_DIR}/results"

# ─── Parameters ──────────────────────────────────────────────────────────────
LANGUAGE="${LANGUAGE:-all}"
INTERNET="${INTERNET:-false}"
DEEPSEEK="${DEEPSEEK:-true}"

# Normalize convenient aliases to the report and agent names.
case "${LANGUAGE}" in
    c++|cxx)
        LANGUAGE="cpp"
        ;;
    "c#"|cs)
        LANGUAGE="csharp"
        ;;
esac

# Validate parameters
if [[ ! "${LANGUAGE}" =~ ^(java|rust|react|typescript|cpp|csharp|all)$ ]]; then
    echo "ERROR: LANGUAGE must be one of: java, rust, react, typescript, cpp, csharp, all"
    exit 1
fi
if [[ ! "${INTERNET}" =~ ^(true|false)$ ]]; then
    echo "ERROR: INTERNET must be true or false"
    exit 1
fi
if [[ ! "${DEEPSEEK}" =~ ^(true|false)$ ]]; then
    echo "ERROR: DEEPSEEK must be true or false"
    exit 1
fi

# ─── Setup ───────────────────────────────────────────────────────────────────
mkdir -p "${AGENTS_DIR}"
mkdir -p "${RESULTS_DIR}"

# Timestamp for output filenames
TIMESTAMP=$(date +%Y%m%d%H%M)
RUN_RESULTS_DIR="${RESULTS_DIR}/${TIMESTAMP}"

if [[ -e "${RUN_RESULTS_DIR}" ]]; then
    suffix=1
    while [[ -e "${RESULTS_DIR}/${TIMESTAMP}-${suffix}" ]]; do
        ((suffix++))
    done
    RUN_RESULTS_DIR="${RESULTS_DIR}/${TIMESTAMP}-${suffix}"
fi

mkdir -p "${RUN_RESULTS_DIR}"

echo "═══════════════════════════════════════════════════════════════════════════"
echo "Example — Code Review Agent Suite"
echo "═══════════════════════════════════════════════════════════════════════════"
echo ""
echo "Configuration:"
echo "  LANGUAGE:        ${LANGUAGE}"
echo "  INTERNET:        ${INTERNET}"
echo "  DEEPSEEK:        ${DEEPSEEK}"
echo "  Results Dir:     ${RUN_RESULTS_DIR}/"
echo "  Requirements:    ${REQUIREMENTS_DIR}/"
echo ""

# ─── Review Functions ────────────────────────────────────────────────────────

review_typescript() {
    local agent="${AGENTS_DIR}/typescript-agent.js"
    echo "▶ Launching TypeScript review agent..."
    
    if [[ ! -f "${agent}" ]]; then
        echo "  ERROR: TypeScript agent not found at ${agent}"
        return 1
    fi
    
    node "${agent}" \
        --language=typescript \
        --internet="${INTERNET}" \
        --deepseek="${DEEPSEEK}" \
        --requirements-dir="${REQUIREMENTS_DIR}" \
        --output-dir="${RUN_RESULTS_DIR}" \
        --timestamp="${TIMESTAMP}"
}

review_java() {
    local agent="${AGENTS_DIR}/java-agent.js"
    echo "▶ Launching Java review agent..."
    
    if [[ ! -f "${agent}" ]]; then
        echo "  ERROR: Java agent not found at ${agent}"
        return 1
    fi
    
    node "${agent}" \
        --language=java \
        --internet="${INTERNET}" \
        --deepseek="${DEEPSEEK}" \
        --requirements-dir="${REQUIREMENTS_DIR}" \
        --output-dir="${RUN_RESULTS_DIR}" \
        --timestamp="${TIMESTAMP}"
}

review_rust() {
    local agent="${AGENTS_DIR}/rust-agent.js"
    echo "▶ Launching Rust review agent..."
    
    if [[ ! -f "${agent}" ]]; then
        echo "  ERROR: Rust agent not found at ${agent}"
        return 1
    fi
    
    node "${agent}" \
        --language=rust \
        --internet="${INTERNET}" \
        --deepseek="${DEEPSEEK}" \
        --requirements-dir="${REQUIREMENTS_DIR}" \
        --output-dir="${RUN_RESULTS_DIR}" \
        --timestamp="${TIMESTAMP}"
}

review_react() {
    local agent="${AGENTS_DIR}/react-agent.js"
    echo "▶ Launching React review agent..."
    
    if [[ ! -f "${agent}" ]]; then
        echo "  ERROR: React agent not found at ${agent}"
        return 1
    fi
    
    node "${agent}" \
        --language=react \
        --internet="${INTERNET}" \
        --deepseek="${DEEPSEEK}" \
        --requirements-dir="${REQUIREMENTS_DIR}" \
        --output-dir="${RUN_RESULTS_DIR}" \
        --timestamp="${TIMESTAMP}"
}

review_cpp() {
    local agent="${AGENTS_DIR}/cpp-agent.js"
    echo "▶ Launching C++ review agent..."

    if [[ ! -f "${agent}" ]]; then
        echo "  ERROR: C++ agent not found at ${agent}"
        return 1
    fi

    node "${agent}" \
        --language=cpp \
        --internet="${INTERNET}" \
        --deepseek="${DEEPSEEK}" \
        --requirements-dir="${REQUIREMENTS_DIR}" \
        --output-dir="${RUN_RESULTS_DIR}" \
        --timestamp="${TIMESTAMP}"
}

review_csharp() {
    local agent="${AGENTS_DIR}/csharp-agent.js"
    echo "▶ Launching C# review agent..."

    if [[ ! -f "${agent}" ]]; then
        echo "  ERROR: C# agent not found at ${agent}"
        return 1
    fi

    node "${agent}" \
        --language=csharp \
        --internet="${INTERNET}" \
        --deepseek="${DEEPSEEK}" \
        --requirements-dir="${REQUIREMENTS_DIR}" \
        --output-dir="${RUN_RESULTS_DIR}" \
        --timestamp="${TIMESTAMP}"
}

# ─── Main orchestration ──────────────────────────────────────────────────────

run_reviews() {
    case "${LANGUAGE}" in
        typescript)
            review_typescript
            ;;
        java)
            review_java
            ;;
        rust)
            review_rust
            ;;
        react)
            review_react
            ;;
        cpp)
            review_cpp
            ;;
        csharp)
            review_csharp
            ;;
        all)
            review_typescript && echo ""
            review_java && echo ""
            review_rust && echo ""
            review_react && echo ""
            review_cpp && echo ""
            review_csharp && echo ""
            ;;
    esac
}

# ─── Execute ─────────────────────────────────────────────────────────────────

if run_reviews; then
    echo ""
    echo "═══════════════════════════════════════════════════════════════════════════"
    echo "✓ Review complete. Results written to ${RUN_RESULTS_DIR}/"
    echo ""
    echo "  Files:"
    ls -1 "${RUN_RESULTS_DIR}"/review-*.md 2>/dev/null | awk '{print "    " $0}'
    echo ""
else
    echo ""
    echo "═══════════════════════════════════════════════════════════════════════════"
    echo "✗ Review failed. See above for details."
    exit 1
fi
