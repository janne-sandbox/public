# REQ-001: No Critical CVEs in Project

## Requirement ID
REQ-001

## Category
Security

## Priority
Critical

## Description
The DIMSE Server application and all its dependencies (SOUP - Software of Unknown Provenance) shall not contain any known critical security vulnerabilities (CVEs) with CVSS scores of 9.0 or higher.

## Rationale
- Medical device software must maintain high security standards to protect patient data (HIPAA compliance)
- Critical vulnerabilities can lead to unauthorized access, data breaches, or system compromise
- Regulatory requirements (FDA, IEC 62304) mandate risk management and vulnerability tracking
- Healthcare environments are frequent targets for cyberattacks

## Acceptance Criteria
1. OWASP Dependency Check scan shows zero critical CVEs (CVSS ≥ 9.0)
2. All identified high CVEs (CVSS 7.0-8.9) have documented risk assessment and mitigation plan
3. Dependency scan performed before each release
4. SOUP list maintained with known vulnerabilities documented

## Verification Method
Automated testing using OWASP Dependency Check Maven plugin:

```bash
# Add plugin to pom.xml
mvn dependency-check:check

# Review generated report
open target/dependency-check-report.html
```

Pass criteria: Zero findings with severity "CRITICAL"

## Traceability
- Related Document: [SOUP List](../documentation/soup.html)
- Related Document: [Project Specifications](../specifications/project-specifications.md)
- Compliance: IEC 62304 (Medical Device Software), FDA Cybersecurity Guidance

## Current Status
✅ **COMPLIANT** (as of January 12, 2026)

Current assessment:
- Spring Boot 3.2.1: No critical CVEs
- DCM4CHE 5.31.2: No critical CVEs
- All database drivers: No critical CVEs
- Jackson 2.15.3: No critical CVEs
- H2 Database CVE-2022-45868: MITIGATED (fixed in 2.2.220+, test scope only)
- Commons BeanUtils CVE-2019-10086: MITIGATED (fixed in 1.9.4)

See [SOUP List](../documentation/soup.html) for complete vulnerability assessment.

## Remediation Process
When critical CVE is identified:

1. **Immediate Assessment** (within 24 hours)
   - Confirm vulnerability applies to application usage
   - Evaluate attack surface and exploitability
   - Document initial risk assessment

2. **Emergency Patch** (within 24-48 hours)
   - Update affected dependency to patched version
   - If patch unavailable, implement compensating controls
   - Test application functionality after update

3. **Validation** (within 72 hours)
   - Re-run dependency scan to confirm remediation
   - Execute regression test suite
   - Update SOUP list with new version

4. **Documentation**
   - Document CVE details, impact, and resolution
   - Update risk assessment
   - Notify stakeholders

## Maintenance
- **Weekly**: Automated dependency scan in CI/CD pipeline
- **Monthly**: Manual review of SOUP list and NVD database
- **Quarterly**: Comprehensive security assessment
- **Ad-hoc**: Upon disclosure of new critical vulnerabilities affecting healthcare systems

## Notes
- Non-critical CVEs (CVSS < 9.0) are evaluated on case-by-case basis
- Test-scope dependencies have lower priority than runtime dependencies
- Vulnerabilities in unused code paths may be accepted with documented justification

## Last Reviewed
January 12, 2026

## Approved By
[Pending - Add approval signature]
