# AI-REQ-002: Script Reliability

## Requirement ID
AI-REQ-002

## Category
Development Standards / Operations

## Priority
High

## Description
All scripts in the project (build, deployment, utility scripts) must be functional, tested, and executable. Scripts shall not be committed in a non-working state.

## Rationale
- Ensures reproducible builds and deployments
- Prevents operational failures in CI/CD pipelines
- Supports developer onboarding (scripts should "just work")
- Maintains automation reliability
- Reduces debugging time for build/deployment issues

## Acceptance Criteria

1. **Executable Permissions**: All scripts must have proper execute permissions
   ```bash
   chmod +x *.sh
   ```

2. **Error Handling**: Scripts must:
   - Check for prerequisites (tools, files, environment variables)
   - Provide meaningful error messages
   - Exit with non-zero status on failure
   - Use `set -e` to fail fast on errors (where appropriate)

3. **Documentation**: Each script must have:
   - Header comment explaining purpose
   - Usage instructions (parameters, examples)
   - Required dependencies listed
   - Exit codes documented

4. **Idempotency**: Scripts should be safe to run multiple times:
   - Check if action already completed
   - Clean up before re-running where appropriate
   - Don't fail if target state already achieved

5. **Platform Compatibility**: Scripts must work on:
   - macOS (primary development platform)
   - Linux (Docker/CI environments)
   - Use portable shell constructs (avoid bash-specific features in sh scripts)

## Covered Scripts

### Primary Operations
- **start.sh**: Start all services (Docker Compose)
- **stop.sh**: Stop all services and optionally clean volumes
- **build.sh**: Build all components (Maven + Angular)

### Browser UI
- **start-browser-ui.sh**: Start Angular development server
- **stop-browser-ui.sh**: Stop Angular development server
- **audit-trail-browser/build.sh**: Build Angular production bundle

### Utilities
- **purge-mongodb-audit-trail-data.sh**: Clean MongoDB audit data
- **start-dapr.sh**: Start with DAPR runtime

## Verification Method

### 1. Execute All Scripts
```bash
cd /Users/janne/work/audit-trail

# Test start script (stops immediately after verifying it works)
./start.sh --profile=default --count=1
# Expected: Docker builds and starts successfully
# Expected exit code: 0

# Test stop script
./stop.sh
# Expected: Containers stopped cleanly
# Expected exit code: 0

# Test build script
./build.sh
# Expected: Maven and Angular builds succeed
# Expected exit code: 0

# Test browser scripts
./start-browser-ui.sh
# Expected: Angular starts on port 4200
# Wait 30 seconds, then:
./stop-browser-ui.sh
# Expected: Angular stops cleanly

# Test cleanup script
./purge-mongodb-audit-trail-data.sh
# Expected: Executes without error (even if no data exists)
# Expected exit code: 0
```

### 2. Verify Executable Permissions
```bash
# All .sh files must be executable
ls -la *.sh audit-trail-browser/*.sh
# Expected: -rwxr-xr-x (executable bits set)
```

### 3. Test Error Conditions
```bash
# Scripts should fail gracefully with helpful messages

# Example: start.sh without Docker running
docker ps > /dev/null 2>&1 || echo "Docker not running - test edge case"
./start.sh --profile=invalid 2>&1 | grep -i "error\|invalid"
# Expected: Clear error message about invalid profile

# Example: Missing dependencies
# (Manually move docker binary temporarily to test detection)
```

### 4. Check Documentation
```bash
# Each script should have usage help
grep -l "Usage:\|USAGE" *.sh
# Expected: All scripts have usage documentation

# Scripts should have header comments
for script in *.sh; do
    echo "=== $script ==="
    head -10 "$script" | grep "^#"
done
# Expected: Each script has descriptive header
```

## Implementation Guidelines

### Script Template
```bash
#!/bin/bash
# Script: script-name.sh
# Purpose: Brief description of what this script does
# Usage: ./script-name.sh [options]
# Dependencies: docker, docker-compose, maven (list all)
# Exit Codes:
#   0 - Success
#   1 - General error
#   2 - Missing dependency

set -e  # Exit on error (use with caution)

# Check prerequisites
command -v docker >/dev/null 2>&1 || {
    echo "Error: docker is required but not installed." >&2
    exit 2
}

# Main logic here
echo "Starting operation..."

# Error handling
if [ $? -ne 0 ]; then
    echo "Error: Operation failed" >&2
    exit 1
fi

echo "Operation completed successfully"
exit 0
```

### For Developers
1. **Test Before Commit**: Always run scripts before committing
2. **Handle Edge Cases**: Test with missing dependencies, wrong paths, etc.
3. **Use Shellcheck**: Validate scripts with `shellcheck script.sh`
4. **Version Control**: Keep scripts in sync with project changes

### For AI Assistants
When modifying or creating scripts:
- **ALWAYS** include error handling and validation
- **ALWAYS** test the script after creation/modification
- **NEVER** assume paths or tools exist - check first
- **ALWAYS** make scripts executable: `chmod +x script.sh`
- **ALWAYS** provide usage documentation in comments
- **VERIFY** script works before marking task complete
- Use `#!/bin/bash` or `#!/bin/sh` shebang appropriately

## Anti-Patterns to Avoid
- ❌ Scripts without error handling
- ❌ Hard-coded absolute paths (use relative or configurable)
- ❌ Assuming tools are installed without checking
- ❌ Silent failures (always output errors)
- ❌ No usage documentation
- ❌ Platform-specific commands without alternatives
- ❌ Non-executable scripts in repository

## Related Requirements
- AI-REQ-001: Test Execution Policy
- AI-REQ-003: Build Process Integrity
- AI-REQ-004: Post-Command Validation

## Status
Active

## Last Updated
2026-01-29

## Author
Generated for AI-assisted development workflow
