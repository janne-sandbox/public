# AI-REQ-001: Test Execution Policy

## Requirement ID
AI-REQ-001

## Category
Development Standards / Testing

## Priority
Critical

## Description
All tests must be executed as part of the build process unless explicitly configured to be skipped. Tests shall NOT be removed or disabled without proper configuration and documentation.

## Rationale
- Ensures code quality and regression detection throughout development
- Prevents silent test removal that could mask defects
- Maintains CI/CD pipeline integrity
- Supports regulatory compliance for medical device software (IEC 62304)
- Enforces test-driven development practices

## Acceptance Criteria
1. **No Test Removal**: Tests shall not be deleted from the codebase without:
   - Documented justification in code review/commit message
   - Replacement with equivalent or better test coverage
   - Approval from technical lead

2. **Conditional Test Skipping Only**: Tests may be skipped using:
   - Maven profiles: `-DskipTests` or `-Dmaven.test.skip=true` (explicit flag)
   - JUnit/TestNG annotations: `@Disabled`, `@Ignore` with reason comment
   - Environment-specific configuration (e.g., integration tests requiring external services)

3. **Configuration Documentation**: Any test skip configuration must:
   - Be documented in project README or test documentation
   - Include reason for skipping
   - Specify conditions for re-enabling
   - Have expiry date or review date

4. **Build Validation**: Default `mvn clean install` must:
   - Execute all unit tests
   - Report test results clearly
   - Fail build on test failures (not skip)

## Prohibited Practices
- Commenting out test methods without annotation
- Deleting test classes without replacement
- Using `@Disabled` or `@Ignore` without explanatory comment
- Adding `-DskipTests` to default build configuration
- Removing test dependencies to "fix" build

## Verification Method

### 1. Maven Build Test Execution
```bash
# Default build must run tests
cd /Users/janne/work/audit-trail
mvn clean install

# Expected: Tests run and results displayed
# Expected output: "Tests run: X, Failures: 0, Errors: 0, Skipped: 0"
```

### 2. Verify Test Count Stability
```bash
# Count test files (should not decrease without documentation)
find audit-trail-api/src/test -name "*Test.java" | wc -l
find audit-trail-repository/src/test -name "*Test.java" | wc -l

# Current baseline:
# audit-trail-api: 5 test files
# audit-trail-repository: 6 test files
```

### 3. Check for Disabled Tests
```bash
# Search for disabled tests (should have explanatory comments)
grep -r "@Disabled\|@Ignore" --include="*Test.java" audit-trail-*/src/test

# Each match must have adjacent comment explaining why
```

### 4. Module-Specific Test Execution
```bash
# API module tests
mvn test -pl audit-trail-api
# Expected: All tests execute successfully

# Repository module tests
mvn test -pl audit-trail-repository
# Expected: All tests execute successfully
```

## Implementation Guidelines

### For Developers
1. **Adding New Tests**: Always add tests for new functionality
2. **Modifying Existing Tests**: Update tests when requirements change
3. **Test Failures**: Fix the code or test, don't disable the test
4. **Integration Tests**: Use profiles for tests requiring external services:
   ```xml
   <profile>
       <id>integration-tests</id>
       <activation>
           <property>
               <name>integration</name>
           </property>
       </activation>
   </profile>
   ```

### For AI Assistants
When modifying test code:
- **NEVER** remove test methods without explicit user approval
- **NEVER** add `-DskipTests` to build commands unless user requests it
- **ALWAYS** ensure tests compile and pass after code changes
- **ALWAYS** maintain or improve test coverage
- If test modification needed, propose changes to user first

## Related Requirements
- AI-REQ-002: Script Reliability
- AI-REQ-003: Build Process Integrity
- AI-REQ-004: Post-Command Validation
- REQ-001: No Critical CVEs

## Status
Active

## Last Updated
2026-01-29

## Author
Generated for AI-assisted development workflow
