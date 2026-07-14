# OWASP-REQ-001: Input Validation and Injection Prevention

## Requirement ID
OWASP-REQ-001

## Regulatory Reference
OWASP Top 10:2021 — A03: Injection  
OWASP Top 10:2021 — A08: Software and Data Integrity Failures

## Category
Security

## Priority
Critical

## Description
All data entering the system through API endpoints, WebSocket messages, or environment configuration must be validated and sanitised before use. The application must be provably free from SQL injection, command injection, and NoSQL injection vulnerabilities. Schema validation must be enforced at every system boundary, not as a secondary check.

## Rationale
- OWASP A03 (Injection) is consistently among the most exploited vulnerability classes. In a healthcare platform, a successful injection attack could expose or corrupt patient alarm records, impersonate care staff, or disrupt alarm delivery.
- Parameterised queries and ORM-generated SQL (Prisma) eliminate most SQL injection by default, but hand-crafted queries, dynamic filter construction, and raw OpenSearch/LocalStack calls remain risk surfaces.
- Input from MQTT-connected IoT devices must be treated as untrusted even though it originates from an internal network.

## Acceptance Criteria
1. **Schema validation at every boundary**: Every Express API endpoint must validate the request body, query parameters, and URL parameters against a defined schema (e.g. using `zod`) before any business logic executes. Requests failing validation must be rejected with HTTP 400 before reaching the service layer.
2. **No raw string interpolation in queries**: No SQL, OpenSearch DSL, or AWS SDK call may construct query payloads by string concatenation with unsanitised user or device input. Prisma parameterised queries or equivalent must be used exclusively.
3. **MQTT payload validation**: The Spring Boot bridge must validate and reject MQTT payloads that do not conform to the expected JSON schema before forwarding to the REST API.
4. **Dependency integrity**: Third-party npm and Maven packages must be verified via lockfiles (`package-lock.json`, `pom.xml` with checksum verification). `npm audit` and OWASP Dependency Check must be run in CI and must not produce critical findings (see REQ-001).
5. **Content-Type enforcement**: The backend must reject requests with unexpected `Content-Type` headers (e.g. reject `text/plain` bodies on JSON endpoints) to prevent type-confusion injection vectors.
6. **Output encoding**: Any user-supplied string rendered in the React frontend must be rendered via React's built-in JSX escaping; `dangerouslySetInnerHTML` must not be used without a documented, reviewed justification and explicit HTML sanitisation.

## Verification Method
- Automated unit tests for each validation schema confirming rejection of malformed, oversized, and type-confused inputs.
- OWASP ZAP or equivalent DAST scan run against the API before each release; zero critical or high injection findings permitted.
- `npm audit --audit-level=critical` passes in CI pipeline with exit code 0.

## Traceability
| ID | Linked to |
|----|-----------|
| OWASP-REQ-001 | OWASP Top 10:2021 A03 |
| OWASP-REQ-001 | OWASP Top 10:2021 A08 |
| OWASP-REQ-001 | REQ-001 (no critical CVEs) |
| OWASP-REQ-001 | OWASP-REQ-002 (authentication and authorisation) |
