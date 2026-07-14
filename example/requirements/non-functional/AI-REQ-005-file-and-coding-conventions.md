# AI-REQ-005: File and Coding Conventions

## Requirement ID
AI-REQ-005

## Category
Development Standards / Code Quality

## Priority
High

## Description
All code, configuration files, scripts, and documentation must follow established naming conventions and coding standards. Consistency in file organization, naming patterns, and code structure is mandatory across the entire project.

## Rationale
- Ensures codebase maintainability and readability
- Facilitates onboarding of new developers
- Reduces cognitive load when navigating the project
- Prevents naming conflicts and confusion
- Supports automated tooling and build processes
- Required for professional software development
- Maintains code quality for medical device software compliance

## File Naming Conventions

### Java Files
```
PascalCase for classes:
✓ AuditTrailRepositoryApplication.java
✓ MongoDBUtil.java
✓ EventDetailDialogComponent.java

Test files must end with "Test":
✓ AuditMessageTest.java
✓ MongoDBTest.java
✓ ConsumerTest.java
```

### Configuration Files
```
Kebab-case for multi-word files:
✓ audit-trail.properties
✓ service_framework.properties
✓ docker-compose.yml
✓ docker-compose.override.yml

Standard names:
✓ pom.xml
✓ Dockerfile
✓ .dockerignore
✓ .gitignore
✓ README.md
✓ CHANGELOG.md
```

### Scripts
```
Kebab-case with .sh extension:
✓ start.sh
✓ stop.sh
✓ start-browser-ui.sh
✓ stop-browser-ui.sh
✓ purge-mongodb-audit-trail-data.sh
✓ validation-suite.sh

Must be executable: chmod +x script-name.sh
```

### TypeScript/Angular Files
```
Kebab-case with type suffix:
✓ event-detail-dialog.component.ts
✓ auth.guard.ts
✓ audit.service.ts
✓ idle-timeout.service.ts
✓ app.module.ts
✓ app-routing.module.ts
```

### Documentation Files
```
UPPERCASE for root-level docs:
✓ README.md
✓ CHANGELOG.md
✓ DOCKER.md
✓ IMPLEMENTATION_STATUS.md
✓ REQUIREMENTS-VERIFICATION.md

Kebab-case for subdirectory docs:
✓ REQ-001-no-critical-cves.md
✓ AI-REQ-001-test-execution-policy.md
✓ API-REQ-005-file-and-coding-conventions.md
```

### Directory Structure
```
Lowercase with hyphens:
✓ audit-trail-api/
✓ audit-trail-repository/
✓ audit-trail-browser/
✓ audit-trail-load-testing/
✓ requirements/
✓ specifications/
✓ documentation/

Java package structure:
✓ com/topcon/audittrail/repository/
✓ com/topcon/audittrail/api/
```

## Java Coding Conventions

### Class and Interface Names
```java
// PascalCase for classes
public class AuditTrailRepositoryApplication { }
public class MongoDBUtil { }

// Interfaces with descriptive names (no "I" prefix)
public interface AuditMessageType { }
public interface SyslogConsumer { }

// Abstract classes with "Abstract" prefix
public abstract class AbstractSyslogConsumer { }
public abstract class AbstractIdCache { }

// Implementation classes with "Impl" suffix (when needed)
public class EventIdImpl implements EventId { }
public class TCPSenderImpl implements TCPSender { }
```

### Method Names
```java
// camelCase for methods
public void startConsumer() { }
public String getEventType() { }
public boolean isValidMessage() { }

// Test methods: descriptive with underscores allowed
@Test
public void testAuditMessageParsing() { }

@Test
public void shouldReturnEmptyList_whenNoEventsExist() { }
```

### Variable Names
```java
// camelCase for variables
private String eventType;
private int recordCount;
private AuditEvent currentEvent;

// UPPER_SNAKE_CASE for constants
public static final String DEFAULT_PROFILE = "default";
public static final int MAX_RETRY_ATTEMPTS = 3;
private static final Logger LOGGER = LoggerFactory.getLogger(ClassName.class);
```

### Package Names
```java
// All lowercase, dot-separated
package com.topcon.audittrail.repository;
package com.topcon.audittrail.api.model;
package com.topcon.audittrail.repository.database;
```

## TypeScript/Angular Coding Conventions

### Component Names
```typescript
// PascalCase for classes
export class EventDetailDialogComponent { }
export class DashboardComponent { }

// File names: kebab-case.component.ts
event-detail-dialog.component.ts
dashboard.component.ts
```

### Service Names
```typescript
// PascalCase for service classes
export class AuditService { }
export class IdleTimeoutService { }

// File names: kebab-case.service.ts
audit.service.ts
idle-timeout.service.ts
```

### Variable and Method Names
```typescript
// camelCase for variables and methods
private eventList: AuditEvent[] = [];
private currentPage: number = 0;

public loadEvents(): void { }
public filterByDateRange(startDate: Date, endDate: Date): void { }
```

### Constants
```typescript
// UPPER_SNAKE_CASE for constants
export const DEFAULT_PAGE_SIZE = 20;
export const API_BASE_URL = '/api/v1';
export const SESSION_TIMEOUT_MS = 1800000;
```

## Script Conventions

### Shebang Line
```bash
#!/bin/bash
# Use bash for advanced features

#!/bin/sh
# Use sh for portability
```

### Variable Names
```bash
# UPPER_CASE for global/exported variables
export JAVA_HOME="/usr/lib/jvm/java-17"
export PROFILE="default"

# lowercase_with_underscores for local variables
local container_name="audit-trail-repository"
local instance_count=1
```

### Function Names
```bash
# lowercase_with_underscores
function check_prerequisites() {
    # function body
}

function generate_nginx_config() {
    # function body
}
```

## Configuration File Conventions

### Properties Files
```properties
# Lowercase with dots for namespacing
server.port=8080
spring.application.name=audit-trail-repository
mongodb.host=localhost
mongodb.port=27017

# Use descriptive keys
audit.trail.retention.days=2555
syslog.listener.tcp.port=8001
syslog.listener.udp.port=8002
```

### YAML Files
```yaml
# Lowercase with hyphens
version: '3.8'
services:
  audit-trail-mongodb:
    image: mongo:6.0
    container_name: audit-trail-mongodb
    
  audit-trail-repository:
    build: .
    container_name: audit-trail-repository
```

## Verification Method

### 1. Check File Naming
```bash
cd /Users/janne/work/audit-trail

# Java files should be PascalCase
find . -name "*.java" -not -path "*/target/*" | while read file; do
    basename "$file" | grep -E '^[A-Z][a-zA-Z0-9]*\.java$' || echo "Invalid: $file"
done

# Scripts should be kebab-case
find . -name "*.sh" -maxdepth 1 | while read file; do
    basename "$file" | grep -E '^[a-z][a-z0-9-]*\.sh$' || echo "Invalid: $file"
done

# Check script permissions
find . -name "*.sh" -maxdepth 1 ! -executable -print
# Expected: No output (all scripts executable)
```

### 2. Check Java Code Style
```bash
# Check for proper class naming
grep -r "^public class [a-z]" --include="*.java" audit-trail-*/src/main/java
# Expected: No matches (all classes should start with uppercase)

# Check for proper constant naming
grep -r "private static final [a-z]" --include="*.java" audit-trail-*/src/main/java
# Expected: Few or no matches (constants should be UPPER_CASE)
```

### 3. Check Package Structure
```bash
# Verify package names are lowercase
find audit-trail-*/src/main/java -type d | grep -E '[A-Z]'
# Expected: Only in class files, not directory names
```

### 4. Check Configuration Files
```bash
# Verify properties file format
for file in $(find . -name "*.properties"); do
    echo "Checking $file"
    grep -E '^[^#][A-Z]' "$file" && echo "  Warning: Uppercase keys found"
done
```

## Implementation Guidelines

### For Developers
1. **Use IDE Formatters**: Configure IDE to follow Java/TypeScript conventions
2. **Review Before Commit**: Check file names before committing
3. **Follow Project Patterns**: Look at existing files for examples
4. **Use Linters**: Enable ESLint for TypeScript, Checkstyle for Java
5. **Consistent Naming**: Don't mix camelCase and snake_case in same context

### For AI Assistants
When creating or modifying files:
- **ALWAYS** use PascalCase for Java classes
- **ALWAYS** use kebab-case for scripts and config files
- **ALWAYS** use camelCase for Java/TypeScript variables and methods
- **ALWAYS** use UPPER_SNAKE_CASE for constants
- **ALWAYS** make scripts executable: `chmod +x script.sh`
- **ALWAYS** follow existing directory structure patterns
- **NEVER** mix naming conventions within the same language/context
- **VERIFY** file names match project conventions before creating

### Naming Checklist
Before creating any file, verify:
- [ ] File extension is correct (.java, .ts, .sh, .md, .properties, etc.)
- [ ] Name follows case convention for file type
- [ ] Name is descriptive and clear
- [ ] No spaces in file names (use hyphens or underscores)
- [ ] Name matches existing patterns in the directory
- [ ] Scripts are executable if .sh files
- [ ] Test files end with "Test" suffix

## Anti-Patterns to Avoid

### File Naming
- ❌ mixed_Case_Names.java
- ❌ ALLUPPERCASE.java (except constants)
- ❌ scriptname (no extension)
- ❌ my script.sh (spaces)
- ❌ MyScript.sh (PascalCase for scripts)
- ❌ test_something.java (should be TestSomething.java)

### Java Code
- ❌ public class myClass { }
- ❌ public String GetValue() { } (should be getValue)
- ❌ private final String MAX_SIZE = "100"; (should be UPPER_CASE)
- ❌ package Com.Topcon; (should be all lowercase)

### Variables
- ❌ String MyVariable; (should be myVariable)
- ❌ int MAX_COUNT = 10; (non-final variable)
- ❌ boolean IsValid; (should be isValid)

### Scripts
- ❌ MY_FUNCTION() { } (should be my_function)
- ❌ variableName="value" (should be variable_name for shell)

## Project-Specific Conventions

### Module Naming
```
All modules use "audit-trail" prefix with hyphens:
- audit-trail-api
- audit-trail-repository
- audit-trail-browser
- audit-trail-load-testing
```

### Service Names
```
Services use descriptive suffixes:
- *Repository (data access layer)
- *Service (business logic)
- *Controller (REST endpoints)
- *Consumer (message consumers)
- *Configuration (configuration classes)
```

### Test Organization
```
Test classes mirror main classes:
src/main/java/com/topcon/audittrail/repository/MongoDBUtil.java
src/test/java/com/topconhealth/MongoDBTest.java
```

## Related Requirements
- AI-REQ-001: Test Execution Policy
- AI-REQ-002: Script Reliability
- AI-REQ-003: Build Process Integrity
- AI-REQ-004: Post-Command Validation
- AI-REQ-006: Safe Temporary Directory Usage

## Status
Active

## Last Updated
2026-01-29

## Author
Generated for AI-assisted development workflow
