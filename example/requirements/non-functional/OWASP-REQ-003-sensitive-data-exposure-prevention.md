# OWASP-REQ-003: Sensitive Data Exposure Prevention

## Requirement ID
OWASP-REQ-003

## Regulatory Reference
OWASP Top 10:2021 — A02: Cryptographic Failures  
OWASP Top 10:2021 — A05: Security Misconfiguration

## Category
Security / Privacy

## Priority
Critical

## Description
The system must prevent inadvertent exposure of sensitive data (PII, PHI, authentication credentials, internal system details) through API responses, error messages, HTTP headers, container configuration, or infrastructure misconfiguration. Security-related HTTP headers must be set on all responses. Default credentials and development-only configurations must be absent from production deployments.

## Rationale
- OWASP A02 (Cryptographic Failures) covers scenarios where sensitive data is transmitted or stored without adequate protection — including the use of weak algorithms, unencrypted protocols, and accidental exposure in API responses.
- OWASP A05 (Security Misconfiguration) is the second-most common finding in practice; default credentials, verbose error messages, and missing security headers are trivially exploitable.
- A healthcare alarm platform holds data that is both personally sensitive (GDPR) and operationally critical (EN 50518); any data leak also constitutes a regulatory breach.

## Acceptance Criteria
1. **Security HTTP headers**: All HTTP responses from nginx (and backend where directly exposed) must include:
   - `Strict-Transport-Security: max-age=63072000; includeSubDomains` (production only)
   - `X-Content-Type-Options: nosniff`
   - `X-Frame-Options: DENY`
   - `Content-Security-Policy` restricting script sources to known origins
   - `Referrer-Policy: no-referrer`
   - `Permissions-Policy` disabling unused browser APIs
2. **No sensitive data in error responses**: API error responses must not include stack traces, internal file paths, SQL error text, or library version information. Errors must be mapped to generic messages with an internal error reference ID; full details logged server-side only.
3. **No default or hardcoded credentials**: All passwords, API keys, and secrets must be supplied via environment variables sourced from a secrets manager. The `.env` file used for local development must be listed in `.gitignore` and must not be committed to version control. Docker images must not embed secrets in layers.
4. **API response field filtering**: REST API responses must only include fields that the requesting role is authorised to receive. Fields containing PII/PHI must be excluded from responses where the caller does not have a documented need for them, even if they exist in the underlying data model.
5. **CORS policy**: The backend must enforce a restrictive CORS policy (`Access-Control-Allow-Origin` limited to the known frontend origin(s)); wildcard `*` is not permitted on any authenticated endpoint.
6. **Container security baseline**: Docker images must not run as root. Images must be based on minimal base images (e.g. `alpine`). Image builds must not `COPY` `.env`, private key files, or credential files into the image.
7. **Dependency exposure**: The API must not expose its dependency versions in response headers (`X-Powered-By` must be removed; framework version banners disabled).

## Verification Method
- Automated header check (e.g. `securityheaders.com` scan or equivalent CI step) confirms all required headers are present on every response.
- Integration test requests an error condition and asserts response body contains no stack trace or SQL text.
- CI pipeline `git-secrets` or `truffleHog` scan confirms no credentials committed to repository.
- Docker image inspection (`docker inspect`) confirms `USER` is non-root.

## Traceability
| ID | Linked to |
|----|-----------|
| OWASP-REQ-003 | OWASP Top 10:2021 A02 |
| OWASP-REQ-003 | OWASP Top 10:2021 A05 |
| OWASP-REQ-003 | GDPR-REQ-003 (encryption at rest and in transit) |
| OWASP-REQ-003 | GDPR-REQ-004 (no PII in logs) |
| OWASP-REQ-003 | OWASP-REQ-001 (input validation) |
| OWASP-REQ-003 | OWASP-REQ-002 (authentication) |
| OWASP-REQ-003 | REQ-001 (no critical CVEs) |
