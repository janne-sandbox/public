# AI-REQ-006: Safe Temporary Directory Usage

## Requirement ID
AI-REQ-006

## Category
Development Standards / System Safety

## Priority
High

## Description
When scripts, build processes, or code require temporary file storage, project-relative temporary directories (e.g., `./tmp`) must be used instead of system-protected directories (e.g., `/tmp`, `/var/tmp`). This ensures proper permissions, isolation, and cleanup while avoiding conflicts with system processes.

## Rationale
- **Security**: Prevents permission issues with system-protected directories
- **Isolation**: Avoids conflicts with other users or system processes
- **Cleanup**: Project-relative temp directories can be easily cleaned with project
- **Portability**: Works consistently across different operating systems and environments
- **Traceability**: Temporary files are clearly associated with the project
- **CI/CD**: Build systems may restrict access to system temp directories
- **Debugging**: Easier to inspect and debug temporary files in known location

## Acceptance Criteria

### 1. Use Project-Relative Temporary Directories
```bash
# ✓ CORRECT: Project-relative temp directory
TEMP_DIR="./tmp"
mkdir -p "$TEMP_DIR"
echo "data" > "$TEMP_DIR/file.txt"

# ✓ CORRECT: Module-specific temp directory
TEMP_DIR="./audit-trail-repository/tmp"
mkdir -p "$TEMP_DIR"

# ✗ WRONG: System temp directory
TEMP_DIR="/tmp"
echo "data" > "/tmp/file.txt"

# ✗ WRONG: /var/tmp usage
mv file.txt /var/tmp/
```

### 2. Create Temp Directories Before Use
```bash
# Always ensure temp directory exists
TEMP_DIR="./tmp"
mkdir -p "$TEMP_DIR"  # -p creates parent directories, no error if exists

# Then use it
cp large-file.dat "$TEMP_DIR/processing.dat"
```

### 3. Clean Up Temporary Files
```bash
# Option 1: Clean up after processing
TEMP_DIR="./tmp"
mkdir -p "$TEMP_DIR"
# ... do work ...
rm -rf "$TEMP_DIR"

# Option 2: Use trap for cleanup on exit
TEMP_DIR="./tmp/$$"  # Use PID for uniqueness
trap "rm -rf $TEMP_DIR" EXIT
mkdir -p "$TEMP_DIR"
# ... do work ...
# Cleanup happens automatically on exit
```

### 4. Add ./tmp to .gitignore
```gitignore
# Temporary directories
tmp/
*/tmp/
.tmp/
*.tmp

# Build artifacts
target/
build/
dist/
node_modules/
```

## Recommended Directory Structure

```
/Users/janne/work/audit-trail/
├── tmp/                          # Project-wide temp files
│   ├── build/                    # Temporary build artifacts
│   ├── test-data/                # Temporary test data
│   └── downloads/                # Temporary downloads
├── audit-trail-api/
│   └── tmp/                      # Module-specific temp files
├── audit-trail-repository/
│   ├── tmp/                      # Module-specific temp files
│   └── target/                   # Maven build output (also temp)
├── audit-trail-browser/
│   ├── tmp/                      # Module-specific temp files
│   ├── .angular/                 # Angular cache (temp)
│   └── dist/                     # Build output (temp)
└── audit-trail-load-testing/
    └── tmp/                      # Module-specific temp files
```

## Verification Method

### 1. Scan for System Temp Directory Usage
```bash
cd /Users/janne/work/audit-trail

# Check shell scripts for /tmp usage
grep -r "/tmp\|/var/tmp" --include="*.sh" .
# Expected: No matches (or only in comments/documentation)

# Check Java code for system temp usage
grep -r "java.io.tmpdir\|File.createTempFile\|/tmp" --include="*.java" audit-trail-*/src
# Expected: Minimal matches, review each usage

# Check for hardcoded temp paths
grep -r "System.getProperty(\"java.io.tmpdir\")" --include="*.java" audit-trail-*/src
# Expected: Should use project-relative paths instead
```

### 2. Verify .gitignore Includes tmp/
```bash
cd /Users/janne/work/audit-trail

# Check .gitignore for tmp patterns
grep -E "^tmp/|^\*/tmp/" .gitignore
# Expected: tmp/ and */tmp/ patterns present

# Verify tmp directories are not in git
git ls-files | grep "/tmp/"
# Expected: No output (tmp directories ignored)
```

### 3. Test Temp Directory Creation
```bash
cd /Users/janne/work/audit-trail

# Test creating project temp directory
mkdir -p ./tmp/test-$(date +%s)
ls -la ./tmp/
# Expected: Directory created successfully with proper permissions

# Clean up test
rm -rf ./tmp/test-*
```

### 4. Check Permissions
```bash
cd /Users/janne/work/audit-trail

# Create temp directory and check permissions
mkdir -p ./tmp
ls -ld ./tmp
# Expected: drwxr-xr-x (owner has full access)

# Verify we can write to it
touch ./tmp/test-write-permission
[ -w ./tmp/test-write-permission ] && echo "✓ Writable" || echo "✗ Not writable"
rm ./tmp/test-write-permission
```

## Implementation Guidelines

### Shell Scripts
```bash
#!/bin/bash
# Example: Script using project-relative temp directory

set -e

# Define project-relative temp directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR"
TEMP_DIR="$PROJECT_ROOT/tmp"

# Create temp directory
mkdir -p "$TEMP_DIR"

# Set up cleanup on exit
cleanup() {
    echo "Cleaning up temporary files..."
    rm -rf "$TEMP_DIR"
}
trap cleanup EXIT

# Use temp directory
echo "Processing data..." > "$TEMP_DIR/processing.log"
cp input.dat "$TEMP_DIR/working.dat"

# Process files in temp directory
process_file "$TEMP_DIR/working.dat"

# Cleanup happens automatically via trap
```

### Java Code
```java
// ✓ CORRECT: Use project-relative temp directory
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;

public class TempFileHandler {
    private static final Path PROJECT_ROOT = Paths.get("").toAbsolutePath();
    private static final Path TEMP_DIR = PROJECT_ROOT.resolve("tmp");
    
    static {
        try {
            Files.createDirectories(TEMP_DIR);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create temp directory", e);
        }
    }
    
    public Path createTempFile(String prefix, String suffix) throws IOException {
        return Files.createTempFile(TEMP_DIR, prefix, suffix);
    }
    
    public void cleanup() {
        // Clean up temp files when done
        try {
            Files.walk(TEMP_DIR)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        // Log error
                    }
                });
        } catch (IOException e) {
            // Log error
        }
    }
}

// ✗ WRONG: Using system temp directory
public class BadTempFileHandler {
    public File createTempFile() {
        return new File("/tmp/myfile.txt");  // DON'T DO THIS
    }
}
```

### TypeScript/Node.js
```typescript
// ✓ CORRECT: Use project-relative temp directory
import * as path from 'path';
import * as fs from 'fs';

export class TempFileService {
    private readonly projectRoot = process.cwd();
    private readonly tempDir = path.join(this.projectRoot, 'tmp');
    
    constructor() {
        // Ensure temp directory exists
        if (!fs.existsSync(this.tempDir)) {
            fs.mkdirSync(this.tempDir, { recursive: true });
        }
    }
    
    createTempFile(filename: string): string {
        return path.join(this.tempDir, filename);
    }
    
    cleanup(): void {
        // Clean up temp directory
        fs.rmSync(this.tempDir, { recursive: true, force: true });
        fs.mkdirSync(this.tempDir, { recursive: true });
    }
}

// ✗ WRONG: Using system temp directory
import * as os from 'os';
export class BadTempService {
    getTempDir(): string {
        return os.tmpdir();  // DON'T DO THIS
    }
}
```

### Maven/Build Configuration
```xml
<!-- pom.xml: Configure temp directory for tests -->
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <configuration>
                <systemPropertyVariables>
                    <java.io.tmpdir>${project.basedir}/tmp</java.io.tmpdir>
                </systemPropertyVariables>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### Docker Configuration
```dockerfile
# Dockerfile: Use project-relative temp in container
FROM maven:3.8.6-eclipse-temurin-17 AS builder

WORKDIR /build

# Create temp directory in build context
RUN mkdir -p /build/tmp

# Set temp directory for build
ENV JAVA_IO_TMPDIR=/build/tmp
ENV TMPDIR=/build/tmp

# Build application
RUN mvn clean package -DskipTests
```

## For AI Assistants

### Critical Rules
When writing scripts or code that needs temporary storage:

1. **NEVER** use `/tmp` or `/var/tmp` directly
2. **ALWAYS** use `./tmp` or project-relative paths
3. **ALWAYS** create temp directory with `mkdir -p` before use
4. **ALWAYS** include cleanup logic (trap, finally, destructors)
5. **VERIFY** temp directory in project root or module root

### Code Generation Template
```bash
# Shell script template with proper temp directory usage
#!/bin/bash
set -e

# Determine project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR"

# Create project-relative temp directory
TEMP_DIR="$PROJECT_ROOT/tmp/$(basename $0 .sh)-$$"
mkdir -p "$TEMP_DIR"

# Setup cleanup
cleanup() {
    rm -rf "$TEMP_DIR"
}
trap cleanup EXIT

# Your code here using $TEMP_DIR
echo "Using temp directory: $TEMP_DIR"

# Cleanup happens automatically
```

## Common Scenarios

### Scenario 1: Build Process Temp Files
```bash
# ✓ CORRECT
cd audit-trail-repository
mkdir -p ./tmp/build
mvn clean package -Djava.io.tmpdir=./tmp/build

# ✗ WRONG
mvn clean package  # Uses /tmp by default
```

### Scenario 2: Test Data Generation
```bash
# ✓ CORRECT
TEMP_DIR="./tmp/test-data"
mkdir -p "$TEMP_DIR"
generate_test_data > "$TEMP_DIR/sample.json"
run_tests --data-dir="$TEMP_DIR"
rm -rf "$TEMP_DIR"

# ✗ WRONG
generate_test_data > /tmp/sample.json
run_tests --data-dir=/tmp
```

### Scenario 3: Docker Build
```dockerfile
# ✓ CORRECT: Use build context temp
WORKDIR /build
RUN mkdir -p /build/tmp
ENV TMPDIR=/build/tmp
RUN mvn package

# ✗ WRONG: Rely on system /tmp
ENV TMPDIR=/tmp
RUN mvn package
```

### Scenario 4: File Processing
```bash
# ✓ CORRECT
TEMP_DIR="./tmp/processing-$$"
mkdir -p "$TEMP_DIR"
trap "rm -rf $TEMP_DIR" EXIT

for file in *.dat; do
    cp "$file" "$TEMP_DIR/"
    process "$TEMP_DIR/$file"
    mv "$TEMP_DIR/$file" "processed/$file"
done

# ✗ WRONG
for file in *.dat; do
    cp "$file" /tmp/
    process "/tmp/$file"
done
```

## .gitignore Configuration

Add these entries to project .gitignore:
```gitignore
# Temporary directories
tmp/
*/tmp/
.tmp/
*.tmp
temp/
*/temp/

# Build outputs (also temporary)
target/
build/
dist/
out/
bin/

# Cache directories
.cache/
.angular/
node_modules/.cache/
```

## Anti-Patterns to Avoid

- ❌ Using `/tmp` directory directly
- ❌ Using `/var/tmp` directory
- ❌ Using `System.getProperty("java.io.tmpdir")` without override
- ❌ Using `os.tmpdir()` in Node.js without override
- ❌ Hardcoding system temp paths
- ❌ Not cleaning up temp files
- ❌ Not creating temp directory before use
- ❌ Committing temp files to git
- ❌ Assuming temp directory exists
- ❌ Not handling temp directory creation failures

## Related Requirements
- AI-REQ-002: Script Reliability
- AI-REQ-003: Build Process Integrity
- AI-REQ-005: File and Coding Conventions

## Status
Active

## Last Updated
2026-01-29

## Author
Generated for AI-assisted development workflow
