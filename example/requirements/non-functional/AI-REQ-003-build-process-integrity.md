# AI-REQ-003: Build Process Integrity

## Requirement ID
AI-REQ-003

## Category
Development Standards / Build System

## Priority
Critical

## Description
The project build process must be reliable, reproducible, and complete. All build commands must execute successfully, compile all modules, and produce deployable artifacts.

## Rationale
- Ensures consistent builds across environments
- Prevents "works on my machine" problems
- Supports CI/CD automation
- Maintains code quality through compilation checks
- Required for regulatory compliance (traceability, reproducibility)
- Enables reliable releases

## Acceptance Criteria

### 1. Maven Build Success
All Maven build commands must complete successfully:

```bash
# Full project build
mvn clean install
# Expected: BUILD SUCCESS for all modules

# Individual module builds
mvn clean install -pl audit-trail-api
mvn clean install -pl audit-trail-repository
# Expected: BUILD SUCCESS for each
```

### 2. Multi-Module Coordination
- Parent POM correctly defines modules
- Dependencies between modules resolve correctly
- Build order respects inter-module dependencies
- No circular dependencies

### 3. Docker Build Success
```bash
# Build Docker images
docker compose build
# Expected: Images built successfully

# Alternative: Direct Docker build
docker build -t audit-trail-repository:latest .
# Expected: BUILD SUCCESS with no errors
```

### 4. Angular Build Success
```bash
cd audit-trail-browser
npm install
npm run build
# Expected: Production bundle created in dist/
```

### 5. Test Execution During Build
- Tests run by default (unless explicitly skipped with `-DskipTests`)
- Test failures cause build failure
- Test results clearly reported

### 6. Artifact Generation
Build must produce:
- JAR files: `audit-trail-api-0.0.1.jar`, `audit-trail-repository-0.0.1.jar`
- Docker images: `audit-trail-repository:latest`
- Angular bundle: `audit-trail-browser/dist/`

## Verification Method

### 1. Clean Build from Scratch
```bash
cd /Users/janne/work/audit-trail

# Clean all build artifacts
mvn clean
rm -rf audit-trail-browser/dist
docker compose down --rmi all

# Full rebuild
mvn clean install
cd audit-trail-browser && npm run build && cd ..
docker compose build

# Verify success
echo "Maven build: $?"
ls -lh audit-trail-api/target/*.jar
ls -lh audit-trail-repository/target/*.jar
ls -lh audit-trail-browser/dist/
docker images | grep audit-trail
```

**Expected Results:**
- Exit code: 0 for all commands
- JAR files exist in target directories
- Angular dist/ folder contains built files
- Docker image appears in `docker images` list

### 2. Dependency Resolution Check
```bash
# Verify all dependencies resolve
mvn dependency:tree -pl audit-trail-repository > dependency-tree.txt

# Check for conflicts or missing dependencies
grep -i "error\|conflict\|missing" dependency-tree.txt
# Expected: No errors or unresolved conflicts
```

### 3. Compilation Check
```bash
# Compile without tests (faster check)
mvn compile -pl audit-trail-api
mvn compile -pl audit-trail-repository

# Verify class files generated
find audit-trail-api/target/classes -name "*.class" | wc -l
find audit-trail-repository/target/classes -name "*.class" | wc -l
# Expected: >0 class files in each
```

### 4. Incremental Build Test
```bash
# First build
mvn clean install -DskipTests
# Expected: BUILD SUCCESS

# No-op build (should be fast, no changes)
mvn install -DskipTests
# Expected: BUILD SUCCESS, up-to-date message

# Change a file and rebuild
touch audit-trail-api/src/main/java/com/topconhealth/service/framework/audittrail/api/Test.java
mvn install -DskipTests -pl audit-trail-api
# Expected: Recompiles only affected module
```

### 5. Build Tool Verification
```bash
# Verify required tools present
mvn --version
# Expected: Maven 3.6+ with Java 17

docker --version
# Expected: Docker 20.10+

node --version
npm --version
# Expected: Node 18+ and npm 9+
```

## Build Configurations

### 1. Maven Compiler Settings (Java 17)
All POM files must specify:
```xml
<properties>
    <maven.compiler.source>17</maven.compiler.source>
    <maven.compiler.target>17</maven.compiler.target>
</properties>
```

### 2. Docker Build
Multi-stage Dockerfile must:
- Build all Maven modules
- Use correct Java version (17)
- Copy dependencies correctly
- Result in runnable image

### 3. Angular Build
- Production build: `npm run build` creates optimized bundle
- Development build: `ng serve` for local testing
- Build configuration in `angular.json`

## Common Build Issues and Solutions

### Issue: Dependency Conflicts
```bash
# Symptom: "class file has wrong version" or "NoClassDefFoundError"
# Solution: Check Java version compatibility
mvn dependency:tree | grep -i conflict
# Fix conflicts in POM files
```

### Issue: Module Dependencies Not Found
```bash
# Symptom: "Cannot resolve com.topcon:audit-trail-api"
# Solution: Build parent module first
mvn install -pl audit-trail-api
# Then build dependent modules
```

### Issue: Docker Build Fails
```bash
# Symptom: Docker build exits with error
# Solution: Check Dockerfile, verify Maven build works standalone
mvn clean install
docker compose build --no-cache
```

## Implementation Guidelines

### For Developers
1. **Test Locally**: Always build locally before pushing
2. **Clean Builds**: Run `mvn clean install` periodically
3. **Fix Warnings**: Address compiler warnings, don't ignore them
4. **Update Dependencies**: Keep dependencies current and compatible
5. **Document Changes**: Update README if build process changes

### For AI Assistants
When modifying code or build configuration:
- **ALWAYS** verify build works after changes
- **NEVER** commit code that doesn't compile
- **ALWAYS** update all POM files consistently (versions, properties)
- **ALWAYS** test both Maven and Docker builds
- **VERIFY** all modules build successfully
- **CHECK** for dependency conflicts after adding new dependencies
- **TEST** the build before marking task complete:
  ```bash
  mvn clean install
  # Expected: BUILD SUCCESS
  ```

## Build Validation Checklist
Before considering any code change complete:
- [ ] `mvn clean install` succeeds
- [ ] All tests pass (or explicitly skipped with `-DskipTests`)
- [ ] No compiler warnings introduced
- [ ] JAR files generated in target/ directories
- [ ] Docker image builds successfully
- [ ] No dependency conflicts in `mvn dependency:tree`
- [ ] Application starts without errors

## Anti-Patterns to Avoid
- ❌ Committing code that doesn't compile
- ❌ Ignoring build warnings
- ❌ Skipping tests by default in POM
- ❌ Hard-coding paths in build configuration
- ❌ Incompatible Java versions across modules
- ❌ Missing or incorrect dependency versions
- ❌ Not testing Docker build after code changes

## Related Requirements
- AI-REQ-001: Test Execution Policy
- AI-REQ-002: Script Reliability
- AI-REQ-004: Post-Command Validation
- REQ-001: No Critical CVEs
- REQ-007: Semantic Versioning

## Project-Specific Build Details

### Current Configuration
- **Java Version**: 17 (upgraded from 11 for springdoc-openapi compatibility)
- **Maven Version**: 3.8.6+
- **Spring Boot Version**: 2.7.18
- **Docker Base Image**: eclipse-temurin:17-jre
- **Build Time**: ~2-3 minutes for full clean build

### Known Issues
- springdoc-openapi-starter-common requires Java 17+
- ARM64/M1 Mac: Use platform-compatible base images
- MongoDB version must match docker-compose.yml (6.0)

## Status
Active

## Last Updated
2026-01-29

## Author
Generated for AI-assisted development workflow
