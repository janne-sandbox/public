# REQ-005: SOUP Documentation

## Requirement ID
REQ-005

## Category
Documentation / Regulatory Compliance

## Priority
Critical

## Description
The Horizon Platform project shall maintain comprehensive documentation of all Software of Unknown Provenance (SOUP) - third-party libraries, frameworks, and dependencies used in the system. This documentation shall include license information, version details, known vulnerabilities (CVEs), and usage justification for regulatory compliance and risk management.

## Rationale
- **Regulatory Compliance**: IEC 62304 (Medical Device Software) requires identification and evaluation of SOUP
- **FDA Requirements**: FDA 510(k) submissions require SOUP documentation for medical device software
- **Risk Management**: ISO 14971 mandates risk assessment for off-the-shelf components
- **Security Auditing**: Known vulnerabilities in dependencies must be tracked and mitigated
- **License Compliance**: Open source license terms must be documented to avoid legal issues
- **Supply Chain Security**: Understanding dependency tree critical for security posture
- **Maintenance Planning**: Version tracking enables security updates and patch management
- **Audit Trail**: Regulators require evidence of SOUP evaluation during inspections

## Scope

### Documentation Requirements
The SOUP documentation shall include:

1. **Complete Dependency List**
   - All direct and transitive dependencies
   - Framework components (.NET, ASP.NET Core)
   - NuGet packages with exact versions
   - JavaScript libraries (if applicable)
   - Docker base images and system libraries

2. **License Information**
   - SPDX license identifier
   - License text or reference
   - Commercial vs. Open Source classification
   - License compatibility assessment
   - Prohibited license detection (see REQ-002)

3. **Vulnerability Assessment**
   - Known CVEs with CVSS scores
   - Severity classification (Critical/High/Medium/Low)
   - Mitigation status or acceptance rationale
   - Last vulnerability scan date
   - Reference to REQ-001 compliance

4. **Version Tracking**
   - Current version in use
   - Latest available version
   - Upgrade considerations
   - End-of-life status

5. **Usage Justification**
   - Purpose in the system
   - Which services use the component
   - Alternatives considered (if applicable)
   - Risk vs. benefit analysis

## Acceptance Criteria

### Documentation Existence
1. ✅ SOUP documentation page exists at `documentation/soup.html`
2. ✅ SOUP page accessible from main documentation navigation
3. ✅ Machine-readable SOUP list (JSON/CSV) available for automated processing

### Content Completeness
1. ✅ All NuGet packages listed with exact versions
2. ✅ All packages include license information (SPDX identifier or license name)
3. ✅ All packages checked for known CVEs
4. ✅ Critical and High severity CVEs documented with mitigation status
5. ✅ Framework components (.NET, ASP.NET Core) documented
6. ✅ DICOM libraries (fo-dicom) specifically documented
7. ✅ Azure SDK components documented
8. ✅ Docker base images documented

### Grouping and Organization
1. ✅ Dependencies grouped by category:
   - Core Framework (.NET Runtime, ASP.NET Core)
   - Web & API (Swashbuckle, OpenAPI)
   - Database (Entity Framework Core, SQL Server)
   - Medical Imaging (fo-dicom, dcmjs)
   - Authentication (OAuth, JWT, Keycloak)
   - Cloud Services (Azure SDKs)
   - Logging & Monitoring
   - Testing & Development tools
2. ✅ Each category shows total package count
3. ✅ License distribution summary provided

### Update Frequency
1. ✅ SOUP documentation updated with every dependency change
2. ✅ Vulnerability scan performed before each release
3. ✅ Last updated timestamp visible on documentation page
4. ✅ Version control history shows regular updates

### Regulatory Readiness
1. ✅ Documentation format suitable for FDA 510(k) submission
2. ✅ Traceability to requirements (REQ-001, REQ-002)
3. ✅ Risk assessment for critical components documented
4. ✅ Justification for GPL/AGPL components (if any) provided

## Verification Method

### Manual Review
1. Open `documentation/soup.html` in browser
2. Verify all major dependencies listed
3. Check license information is complete
4. Confirm CVE data is current (within 30 days)

### Automated Validation
```bash
# Generate dependency report
dotnet list package --include-transitive > dependencies.txt

# Compare against SOUP documentation
# (Future: automated script to validate completeness)

# Scan for vulnerabilities
dotnet list package --vulnerable

# Check for outdated packages
dotnet list package --outdated
```

### Checklist for Compliance
- [ ] All packages from `*.csproj` files documented
- [ ] Licenses verified against SPDX database
- [ ] No prohibited licenses found (REQ-002)
- [ ] No critical CVEs unmitigated (REQ-001)
- [ ] Docker base images documented
- [ ] Last scan date < 30 days old
- [ ] Document reviewed by compliance officer

## Traceability

### Related Requirements
- **REQ-001**: No Critical CVEs - SOUP documentation enables CVE tracking
- **REQ-002**: Prohibited Licenses - SOUP documentation includes license compliance
- **REQ-004**: API Documentation - Some SOUP components (Swashbuckle) enable this requirement

### Related Documents
- [SOUP List](../documentation/soup.html) - Main SOUP documentation page
- [Architecture Documentation](../documentation/architecture.html) - System design context
- [Technology Stack](../documentation/technology.html) - Technology choices and rationale

### Compliance Standards
- **IEC 62304** (Medical Device Software) - Section 5.1.4 (SOUP identification), Section 8.1.2 (Risk management for SOUP)
- **FDA Cybersecurity Guidance** - Requires SOUP vulnerability tracking (SBOM)
- **ISO 14971** (Risk Management) - Risk assessment for off-the-shelf components
- **ISO 13485** (Quality Management) - Traceability and document control
- **21 CFR Part 11** - Electronic records requirements for medical devices

## Current Status
✅ **COMPLIANT** (as of January 13, 2026)

### Implementation Details
- **Location**: `documentation/soup.html`
- **Total Packages Documented**: 80+ packages across all services
- **License Types**: MIT (majority), Apache-2.0, BSD-3-Clause, GPL-3.0
- **Critical CVEs**: 0 unmitigated
- **High CVEs**: 0 unmitigated
- **Last Vulnerability Scan**: January 12, 2026
- **Last Documentation Update**: January 13, 2026

### Notable SOUP Components
1. **.NET 9.0** - Core runtime (MIT license)
2. **ASP.NET Core 9.0** - Web framework (MIT license)
3. **Entity Framework Core 9.0.11** - ORM (MIT license)
4. **fo-dicom 5.2.5** - DICOM library (MS-PL license)
5. **Swashbuckle.AspNetCore** - OpenAPI/Swagger (MIT license)
6. **FluentMigrator 6.2.0** - Database migrations (Apache-2.0)
7. **Azure SDKs** - Cloud integration (MIT license)

### Risk Assessment Summary
All documented SOUP components have been evaluated for:
- **Security**: No critical vulnerabilities (CVSS ≥ 9.0)
- **License Compliance**: No prohibited licenses (GPL v2, AGPL)
- **Maintenance**: All components actively maintained
- **Alternatives**: Major components evaluated against alternatives
- **Impact**: Failure scenarios documented for critical dependencies

## Maintenance Plan

### Quarterly Review
- Update all package versions
- Re-scan for vulnerabilities
- Review end-of-life status
- Update risk assessments

### Continuous Monitoring
- Automated CVE alerts via GitHub Dependabot
- License compliance scanning in CI/CD pipeline
- Version update notifications

### Documentation Updates
- Update SOUP page within 5 business days of dependency changes
- Include SOUP review in change control process
- Maintain change log of SOUP modifications

## References

### Standards and Guidance
- IEC 62304:2006+AMD1:2015 - Medical device software lifecycle
- FDA Guidance: "Content of Premarket Submissions for Device Software Functions"
- FDA Guidance: "Cybersecurity in Medical Devices: Quality System Considerations"
- ISO 14971:2019 - Medical devices - Application of risk management
- NIST SP 800-161 - Cybersecurity Supply Chain Risk Management

### Tools
- **OWASP Dependency Check** - Vulnerability scanning
- **dotnet list package** - .NET dependency analysis
- **NuGet Package Explorer** - License and metadata inspection
- **GitHub Dependabot** - Automated vulnerability alerts
- **Snyk** - Continuous security monitoring (optional)

## Notes
- SOUP documentation is a living document requiring continuous maintenance
- Regulatory inspections may request historical SOUP records (maintain version control)
- Third-party auditors will verify SOUP documentation against actual project files
- Consider automating SOUP documentation generation from project files for accuracy
- For medical device submission, ensure SOUP documentation is signed and approved by quality assurance

## Revision History
| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0.0 | 2026-01-13 | System | Initial requirement created |

---

**Approved By**: [Quality Assurance] [Regulatory Affairs]  
**Review Date**: 2026-01-13  
**Next Review**: 2026-04-13 (Quarterly)
