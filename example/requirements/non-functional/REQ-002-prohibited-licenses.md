# REQ-002: Prohibited Open Source Licenses

## Requirement ID
REQ-002

## Category
Legal / Licensing

## Priority
Critical

## Description
The DIMSE Server application shall not include any dependencies (SOUP) with copyleft licenses that would require disclosure of proprietary source code or impose copyleft obligations on the entire application. Specifically prohibited licenses include GPL (GNU General Public License) and AGPL (GNU Affero General Public License) without explicit dual-licensing alternatives.

## Prohibited Licenses

### Strictly Prohibited
- **GPL v2** (GNU General Public License version 2)
- **GPL v3** (GNU General Public License version 3)
- **AGPL v3** (GNU Affero General Public License version 3)

### Conditionally Prohibited (Requires Legal Review)
- **LGPL v2.1** (GNU Lesser General Public License) - Allowed ONLY if used as separate library without modification
- **LGPL v3** - Allowed ONLY if used as separate library without modification
- **EPL 1.0/2.0** (Eclipse Public License) - Allowed for unmodified libraries
- **CDDL** (Common Development and Distribution License) - Requires review
- **EUPL** (European Union Public License) - Requires review

### Permitted Licenses
- **Apache 2.0** ✅
- **MIT** ✅
- **BSD (2-Clause, 3-Clause)** ✅
- **MPL 2.0** (Mozilla Public License) ✅ - File-level copyleft acceptable
- **ISC** ✅
- **CC0** ✅ (Public Domain)

## Rationale
- **Commercial Use**: Application intended for commercial distribution to healthcare providers
- **Proprietary Code Protection**: Contains proprietary algorithms and integrations that cannot be disclosed
- **Customer Requirements**: Healthcare customers require non-copyleft licensing for their internal deployments
- **Derivative Works**: GPL would require making the entire application open source if distributed
- **Legal Risk**: GPL violations can result in injunctions, damages, and forced disclosure

## Exceptions and Special Cases

### DCM4CHE Dual License
- **Status**: ✅ APPROVED
- **License**: MPL 2.0 / GPL 2.0 (dual-licensed)
- **Usage**: Used under MPL 2.0 option (NOT GPL)
- **Rationale**: MPL 2.0 is file-level copyleft, does not affect application as a whole
- **Documentation**: SOUP list must clearly state "Used under MPL 2.0"

### LGPL Libraries (Hibernate, Logback)
- **Status**: ✅ APPROVED
- **License**: LGPL 2.1
- **Usage**: Dynamic linking only, no modifications to library source code
- **Rationale**: LGPL permits use as separate library without copyleft obligations
- **Requirements**:
  - Must be dynamically linked (JAR files, not source inclusion)
  - Cannot modify LGPL library source code
  - Must include copy of LGPL license in distribution

### EPL Libraries (H2 Database, JUnit)
- **Status**: ✅ APPROVED
- **License**: EPL 1.0/2.0
- **Usage**: Unmodified libraries, no source code changes
- **Rationale**: EPL allows commercial use without source disclosure if unmodified
- **Requirements**:
  - H2: Test scope only (not in production distribution)
  - JUnit: Test scope only
  - Must include copy of EPL license

## Acceptance Criteria
1. Maven dependency tree scan shows zero GPL/AGPL dependencies in compile/runtime scope
2. All LGPL/EPL dependencies documented with usage justification in SOUP list
3. License compliance review completed before each release
4. All dual-licensed dependencies explicitly configured to use permissive license option

## Verification Method

### Automated License Scanning
```bash
# Maven License Plugin
mvn license:add-third-party
mvn license:aggregate-add-third-party

# Review generated licenses list
cat target/generated-sources/license/THIRD-PARTY.txt

# License Maven Plugin for enforcement
mvn license:check-licenses
```

### Manual Verification
1. Review pom.xml for all dependencies
2. Check each dependency's LICENSE file on GitHub/Maven Central
3. For dual-licensed libraries, verify usage of permissive option
4. Document findings in SOUP list

### CI/CD Integration
```xml
<!-- Add to pom.xml build/plugins -->
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>license-maven-plugin</artifactId>
    <version>2.4.0</version>
    <configuration>
        <failOnBlacklist>true</failOnBlacklist>
        <excludedLicenses>
            <excludedLicense>GNU General Public License v2.0</excludedLicense>
            <excludedLicense>GNU General Public License v3.0</excludedLicense>
            <excludedLicense>GNU Affero General Public License v3.0</excludedLicense>
        </excludedLicenses>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>check-licenses</goal>
            </goals>
            <phase>verify</phase>
        </execution>
    </executions>
</plugin>
```

## Traceability
- Related Document: [SOUP List - License Compliance Summary](../documentation/soup.html#license-compliance-summary)
- Related Document: [Project Specifications](../specifications/project-specifications.md)
- Related Requirement: REQ-001 - No Critical CVEs (security-related dependencies)

## Current Status
✅ **COMPLIANT** (as of January 12, 2026)

Current license assessment:

### Runtime Dependencies (Production)
| Library | License | Status |
|---------|---------|--------|
| Spring Boot 3.2.1 | Apache 2.0 | ✅ Approved |
| DCM4CHE 5.31.2 | MPL 2.0 / GPL 2.0 | ✅ Used under MPL 2.0 |
| Hibernate 6.4.1 | LGPL 2.1 | ✅ Dynamic linking, unmodified |
| Jackson 2.15.3 | Apache 2.0 | ✅ Approved |
| Apache HttpClient 5 | Apache 2.0 | ✅ Approved |
| MS SQL JDBC Driver | MIT | ✅ Approved |
| PostgreSQL JDBC | BSD-2-Clause | ✅ Approved |
| Liquibase | Apache 2.0 | ✅ Approved |
| Logback | EPL 1.0 / LGPL 2.1 | ✅ Dynamic linking, unmodified |
| Apache Commons | Apache 2.0 | ✅ Approved |
| Lombok | MIT | ✅ Approved |

### Test Dependencies (Not Distributed)
| Library | License | Status |
|---------|---------|--------|
| H2 Database | EPL 1.0 / MPL 2.0 | ✅ Test scope only |
| JUnit 5 | EPL 2.0 | ✅ Test scope only |
| TestContainers | MIT | ✅ Approved |

**No GPL or AGPL dependencies detected.**

## Risk Assessment

### Low Risk - LGPL/EPL in Dynamic Libraries
- **Likelihood**: Low - Using as intended (dynamic linking)
- **Impact**: Low - No source disclosure required if unmodified
- **Mitigation**: 
  - Document usage in SOUP list
  - Never modify LGPL/EPL library source code
  - Always include license files in distribution

### Medium Risk - Dual-Licensed Dependencies
- **Likelihood**: Medium - Could accidentally use wrong license
- **Impact**: Critical - GPL option would require source disclosure
- **Mitigation**:
  - Explicitly document license choice in SOUP list
  - Code comments indicating MPL 2.0 usage for DCM4CHE
  - Regular license audits

### High Risk - Transitive Dependencies
- **Likelihood**: Medium - Dependency updates could introduce GPL libraries
- **Impact**: Critical - Undetected GPL could require source disclosure
- **Mitigation**:
  - Automated license scanning in CI/CD pipeline
  - Dependency review before each update
  - Lock dependency versions (dependencyManagement section)

## Remediation Process

If GPL/AGPL dependency is discovered:

1. **Immediate Action** (within 24 hours)
   - Freeze releases until resolved
   - Identify how GPL dependency was introduced
   - Document initial assessment

2. **Resolution** (within 1 week)
   - Option A: Find alternative library with permissive license
   - Option B: If dual-licensed, configure to use permissive option
   - Option C: Obtain commercial license from vendor
   - Option D: Request legal exception with documented justification

3. **Validation**
   - Re-run license scan to confirm removal
   - Update SOUP list
   - Document resolution in this requirement

4. **Prevention**
   - Add license check to CI/CD pipeline if not present
   - Update dependency guidelines for development team
   - Consider Maven enforcer plugin rules

## Distribution Requirements

When distributing DIMSE Server:

1. **Include License Files**
   - Apache 2.0 license (primary application license)
   - All third-party licenses (LICENSES/third-party directory)
   - SOUP list with complete license information

2. **NOTICE File**
   - Attribution for all Apache 2.0 components
   - Attribution for MIT, BSD components as required

3. **LGPL/EPL Compliance**
   - Include copy of LGPL 2.1 license
   - Include copy of EPL 1.0 license
   - Note: No source code modifications made to LGPL/EPL libraries

## Maintenance
- **Every Dependency Update**: License check required
- **Monthly**: Review transitive dependencies for license changes
- **Quarterly**: Full license audit of all dependencies
- **Annually**: Legal review of license compliance

## References
- [GNU GPL FAQ](https://www.gnu.org/licenses/gpl-faq.html)
- [Apache License 2.0 and GPL Compatibility](https://www.apache.org/licenses/GPL-compatibility.html)
- [LGPL Java Use Case](https://www.gnu.org/licenses/lgpl-java.html)
- [Choose a License](https://choosealicense.com/)
- [SPDX License List](https://spdx.org/licenses/)

## Last Reviewed
January 12, 2026

## Approved By
[Pending - Add legal department approval signature]
