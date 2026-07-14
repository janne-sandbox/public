# REQ-007: Semantic Versioning for All Documentation and Code

## Requirement ID
REQ-007

## Category
Documentation & Version Control

## Priority
High

## Description
All version numbers in the Horizon Platform project shall follow Semantic Versioning 2.0.0 (SemVer) specification using the x.y.z format (MAJOR.MINOR.PATCH). This applies to:
- Project version numbers
- Documentation version references
- Changelog entries
- Specification documents
- API versions
- Package versions
- Release tags

## Rationale
- Provides clear, unambiguous version information across all artifacts
- Enables automated version parsing and comparison
- Ensures consistency in version tracking and dependency management
- Facilitates proper release management and change communication
- Supports regulatory traceability requirements
- Prevents confusion between version formats (e.g., 1.0 vs 1.0.0)

## Semantic Versioning Rules
Given a version number MAJOR.MINOR.PATCH, increment the:
1. **MAJOR** version when you make incompatible API changes
2. **MINOR** version when you add functionality in a backward compatible manner
3. **PATCH** version when you make backward compatible bug fixes

### Version Format Requirements
- **Format:** `x.y.z` where x, y, z are non-negative integers
- **Examples:** `1.0.0`, `1.0.1`, `2.1.3`, `10.15.2`
- **Invalid:** `1.0`, `v1.0.0`, `1.0.0-beta`, `1.0.0.0`
- **Pre-release:** May use `-alpha`, `-beta`, `-rc.1` suffix: `1.0.0-beta.1`

## Acceptance Criteria
1. All version references in markdown files use x.y.z format
2. All version references in HTML documentation use x.y.z format
3. All version references in JSON/YAML configuration files use x.y.z format
4. CHANGELOG.md uses semantic versioning for all release entries
5. VERSION.json contains semantic version string
6. Git tags for releases follow semantic versioning: `v1.0.0`, `v1.0.1`
7. Specification documents include version in x.y.z format
8. No two-part version numbers (e.g., `1.0`) except when referring to major.minor version ranges

## Files Required to Use Semantic Versioning

### Root Directory
- `CHANGELOG.md` - Version headers: `## [1.0.0]`, `## [1.0.1]`
- `VERSION.json` - `"version": "1.0.0"`
- `README.md` - Version references in badges or headers

### Documentation
- `documentation/changelog.html` - Version sections
- `documentation/README.md` - Document version metadata
- All `documentation/*.html` - Version references

### Specifications
- All `specifications/*.md` files - Version metadata headers
- `specifications/README.md` - Specification version table
- `specifications/RELEASE-NOTES-*.md` - Version-specific release notes

### Requirements
- All `requirements/*.md` files - Document version in metadata

### Migration Guides
- `MIGRATION-v*.md` - Filenames use semantic versioning

## Verification Method

### Automated Check
```bash
# Search for invalid version patterns (two-part versions)
grep -rn --include="*.md" --include="*.html" --include="*.json" \
  -E '\bVersion:?\s+[0-9]+\.[0-9]+\b(?!\.[0-9])' \
  documentation/ specifications/ requirements/

# Expected: No matches (or only valid exceptions like "HTTP/1.1")
```

### Manual Review Checklist
- [ ] CHANGELOG.md uses `[x.y.z]` format for all versions
- [ ] VERSION.json has three-part version string
- [ ] All specification documents show version as x.y.z
- [ ] All requirements documents show version as x.y.z
- [ ] HTML changelog displays versions in x.y.z format
- [ ] Git tags follow `vx.y.z` pattern

## Exceptions
The following are **not** subject to this requirement:
- Protocol version numbers (e.g., `HTTP/1.1`, `TLS 1.2`, `OAuth 2.0`)
- External package versions specified by upstream maintainers
- Third-party SOUP component versions (document as provided)
- DICOMweb protocol headers (e.g., `X-DICOMweb-Version: 1.0`)
- References to semantic versioning specification itself (e.g., "SemVer 2.0.0")

## Traceability
- Related Document: [CHANGELOG.md](../CHANGELOG.md)
- Related Document: [VERSION.json](../VERSION.json)
- Related Document: [Specifications README](../specifications/README.md)
- Standard: [Semantic Versioning 2.0.0](https://semver.org/spec/v2.0.0.html)
- Compliance: ISO 13485 (Documentation Control), IEC 62304 (Software Configuration Management)

## Current Status
✅ **COMPLIANT** (as of January 21, 2026)

### Implementation Summary
- Project version: 1.0.1
- All documentation updated to use x.y.z format
- CHANGELOG.md uses semantic versioning
- VERSION.json created with semantic version string
- 19 specifications updated to x.y.z format
- HTML changelog updated with version 1.0.0 and 1.0.1 sections
- Git tagging strategy established

### Non-Compliance Identified and Fixed
- ✅ Changed 15+ instances of "1.0" to "1.0.0" across specifications
- ✅ Updated REQUIREMENTS-VERIFICATION.md from "1.0" to "1.0.0"
- ✅ Updated all specification version tables to use x.y.z format
- ✅ Created VERSION.json with proper semantic version

## Maintenance
This requirement shall be verified:
1. Before each release (automated check in CI/CD pipeline)
2. During documentation reviews
3. When creating new specification or requirement documents
4. When updating changelog entries

## Enforcement
- Pre-commit hooks may validate version format in modified files
- CI/CD pipeline shall fail if invalid version formats detected
- Code review checklist includes semantic versioning verification

## Revision History
| Version | Date       | Author | Changes                     |
|---------|------------|--------|-----------------------------|
| 1.0.0   | 2026-01-21 | System | Initial requirement created |

---

**Approved By**: [Technical Lead] [Documentation Manager]  
**Review Date**: 2026-01-21  
**Next Review**: 2026-04-21 (Quarterly)
