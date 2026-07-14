# AI-REQ-004: Post-Command Validation

## Requirement ID
AI-REQ-004

## Category
Development Standards / Quality Assurance

## Priority
High

## Description
After executing any command that modifies code, configuration, or deployment state, proper validation must be performed to verify the command completed successfully and achieved the intended result. No operation should be considered complete without verification.

## Rationale
- Ensures changes actually work as intended
- Detects failures early before they compound
- Prevents "assumed success" leading to cascading failures
- Supports reliable automation and CI/CD
- Maintains system integrity and user confidence
- Required for medical device software validation

## Acceptance Criteria

### 1. Build Command Validation
After any build command:
```bash
# Execute build
mvn clean install

# Validate
if [ $? -eq 0 ]; then
    echo "✓ Build succeeded"
    ls -lh */target/*.jar
else
    echo "✗ Build failed"
    exit 1
fi
```

### 2. Test Execution Validation
After running tests:
```bash
# Execute tests
mvn test

# Validate results
mvn surefire-report:report
grep -i "Tests run:" target/surefire-reports/*.txt
# Expected: "Tests run: X, Failures: 0, Errors: 0, Skipped: 0"
```

### 3. Deployment Validation
After starting services:
```bash
# Start services
./start.sh --profile=default --count=1

# Wait for startup
sleep 10

# Validate services running
docker ps | grep audit-trail-repository
# Expected: Container running

# Validate API responding
curl -s http://localhost:8080/api/v1/audit/events?page=0&size=1
# Expected: HTTP 200 with JSON response

# Validate UI accessible
curl -s http://localhost:4200 | grep "<title>"
# Expected: HTML with title tag
```

### 4. Code Modification Validation
After changing source code:
```bash
# Make code changes (example: modify Java file)
# ...

# Validate compilation
mvn compile -pl <affected-module>

# Validate tests still pass
mvn test -pl <affected-module>

# Validate full integration
mvn clean install
```

### 5. Configuration Change Validation
After modifying configuration files:
```bash
# Change configuration (example: application.properties)
# ...

# Validate syntax (if applicable)
# For properties files: check key=value format
grep -v "^#" config.properties | grep -v "^$" | grep -v "=" && echo "Invalid format"

# Test with new configuration
# Start application and verify behavior
```

### 6. Docker Build Validation
After building Docker images:
```bash
# Build image
docker compose build

# Validate image created
docker images | grep audit-trail-repository
# Expected: Image listed with "latest" tag and recent timestamp

# Validate image runs
docker run --rm audit-trail-repository:latest java --version
# Expected: Java version output

# Validate full stack
docker compose up -d
sleep 10
docker compose ps | grep "Up"
# Expected: All services in "Up" state
```

## Verification Method

### Validation Test Suite
Run this after any significant change:

```bash
#!/bin/bash
# validation-suite.sh - Comprehensive validation script

set -e
cd /Users/janne/work/audit-trail

echo "=== Phase 1: Build Validation ==="
mvn clean install
if [ $? -ne 0 ]; then
    echo "FAIL: Maven build failed"
    exit 1
fi
echo "PASS: Maven build succeeded"

echo ""
echo "=== Phase 2: Artifact Validation ==="
for jar in audit-trail-api/target/*.jar audit-trail-repository/target/*.jar; do
    if [ -f "$jar" ]; then
        echo "PASS: Found $jar ($(ls -lh $jar | awk '{print $5}'))"
    else
        echo "FAIL: Missing $jar"
        exit 1
    fi
done

echo ""
echo "=== Phase 3: Docker Build Validation ==="
docker compose build
if [ $? -ne 0 ]; then
    echo "FAIL: Docker build failed"
    exit 1
fi
echo "PASS: Docker build succeeded"

echo ""
echo "=== Phase 4: Service Startup Validation ==="
./start.sh --profile=default --count=1
sleep 15

if ! docker ps | grep -q audit-trail-repository; then
    echo "FAIL: Repository container not running"
    docker compose logs audit-trail-repository | tail -50
    exit 1
fi
echo "PASS: Repository container running"

echo ""
echo "=== Phase 5: API Validation ==="
response=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/v1/audit/events?page=0&size=1)
if [ "$response" != "200" ]; then
    echo "FAIL: API returned HTTP $response"
    exit 1
fi
echo "PASS: API responding (HTTP 200)"

echo ""
echo "=== Phase 6: Frontend Validation ==="
if curl -s http://localhost:4200 | grep -q "<title>"; then
    echo "PASS: Frontend serving HTML"
else
    echo "FAIL: Frontend not responding correctly"
    exit 1
fi

echo ""
echo "=== Phase 7: MongoDB Validation ==="
if docker ps | grep -q audit-trail-mongodb; then
    echo "PASS: MongoDB container running"
else
    echo "FAIL: MongoDB container not running"
    exit 1
fi

echo ""
echo "================================="
echo "ALL VALIDATION CHECKS PASSED ✓"
echo "================================="
```

Save as `/Users/janne/work/audit-trail/requirements/validation-suite.sh` and run after changes.

## Validation Checklists

### After Build Command
- [ ] Exit code is 0
- [ ] "BUILD SUCCESS" message appears
- [ ] JAR files exist in target/ directories
- [ ] No error messages in output
- [ ] Tests passed (if not skipped)

### After Service Start
- [ ] `docker ps` shows containers running
- [ ] API endpoint responds (curl test)
- [ ] Frontend loads (curl test)
- [ ] Logs show no errors
- [ ] Services listen on expected ports (8080, 4200, 27017)

### After Code Change
- [ ] Code compiles without errors
- [ ] All tests still pass
- [ ] No new compiler warnings
- [ ] Changed functionality works as expected
- [ ] No regressions in existing features

### After Configuration Change
- [ ] Configuration file syntax valid
- [ ] Application starts with new configuration
- [ ] Changed behavior verified
- [ ] No error messages in logs
- [ ] Fallback/default values work if applicable

## Implementation Guidelines

### For Developers
1. **Don't Assume**: Always verify, never assume success
2. **Check Exit Codes**: Use `$?` to check command success
3. **Read Logs**: Check application logs after starting services
4. **Test Endpoints**: Use curl or browser to verify APIs
5. **Monitor Resources**: Check CPU, memory, disk after deployments

### For AI Assistants
Critical validation rules:
- **ALWAYS** check exit code after commands: `if [ $? -ne 0 ]; then ...`
- **ALWAYS** verify build artifacts exist after building
- **ALWAYS** test API endpoints after starting services
- **ALWAYS** confirm containers running after docker compose up
- **NEVER** assume command succeeded without checking
- **NEVER** mark task complete without validation
- **ALWAYS** wait adequate time for service startup (10-30 seconds)
- **ALWAYS** check logs if validation fails

### Validation Command Template
```bash
# Execute command
<command>

# Capture exit code
EXIT_CODE=$?

# Validate
if [ $EXIT_CODE -eq 0 ]; then
    # Additional checks
    if <verification_check>; then
        echo "✓ Success: <operation> completed"
    else
        echo "✗ Warning: <operation> completed but validation failed"
        exit 1
    fi
else
    echo "✗ Error: <operation> failed with exit code $EXIT_CODE"
    exit $EXIT_CODE
fi
```

## Common Validation Scenarios

### 1. Maven Build
```bash
mvn clean install
[ $? -eq 0 ] && echo "✓ Build OK" || echo "✗ Build failed"
```

### 2. Docker Container
```bash
docker compose up -d
sleep 10
docker compose ps | grep "Up" && echo "✓ Containers OK" || echo "✗ Containers failed"
```

### 3. API Endpoint
```bash
response=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/v1/audit/events?page=0&size=1)
[ "$response" = "200" ] && echo "✓ API OK" || echo "✗ API returned $response"
```

### 4. File Generation
```bash
expected_file="target/artifact.jar"
[ -f "$expected_file" ] && echo "✓ Artifact created" || echo "✗ Artifact missing"
```

### 5. Test Results
```bash
mvn test
grep "Failures: 0, Errors: 0" target/surefire-reports/*.txt && echo "✓ Tests OK" || echo "✗ Tests failed"
```

## Automated Validation Integration

### CI/CD Pipeline Integration
```yaml
# Example GitHub Actions / GitLab CI validation
- name: Build
  run: mvn clean install
  
- name: Validate Build
  run: |
    if [ ! -f audit-trail-repository/target/*.jar ]; then
      echo "Build validation failed: JAR not found"
      exit 1
    fi
    
- name: Start Services
  run: ./start.sh --profile=default --count=1
  
- name: Validate Services
  run: |
    sleep 15
    curl -f http://localhost:8080/api/v1/audit/events?page=0&size=1 || exit 1
```

## Anti-Patterns to Avoid
- ❌ Assuming command succeeded without checking
- ❌ Not reading command output
- ❌ Ignoring exit codes
- ❌ Not waiting for async operations to complete
- ❌ Testing in wrong order (e.g., test API before starting service)
- ❌ Marking task complete without validation
- ❌ Single point of validation (check multiple indicators)

## Related Requirements
- AI-REQ-001: Test Execution Policy
- AI-REQ-002: Script Reliability
- AI-REQ-003: Build Process Integrity
- REQ-001: No Critical CVEs

## Validation Timing Guidelines

| Operation | Wait Time | Validation Method |
|-----------|-----------|-------------------|
| Maven build | 0s | Check exit code immediately |
| Docker build | 0s | Check exit code + image list |
| Service start | 10-30s | Check docker ps + API response |
| Database ready | 5-10s | Check connection / query |
| Frontend build | 0s | Check dist/ folder exists |
| Test execution | 0s | Check surefire reports |

## Status
Active

## Last Updated
2026-01-29

## Author
Generated for AI-assisted development workflow
