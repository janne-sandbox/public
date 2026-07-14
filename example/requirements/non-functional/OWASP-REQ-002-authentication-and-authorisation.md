# OWASP-REQ-002: Authentication, Authorisation, and Session Management

## Requirement ID
OWASP-REQ-002

## Regulatory Reference
OWASP Top 10:2021 — A01: Broken Access Control  
OWASP Top 10:2021 — A07: Identification and Authentication Failures

## Category
Security

## Priority
Critical

## Description
The system must enforce strict authentication for all non-public endpoints and must implement role-based access control (RBAC) so that users can only perform actions and access data permitted by their assigned role. Session tokens must be short-lived, securely stored, and invalidatable server-side.

## Rationale
- OWASP A01 (Broken Access Control) is the top-ranked vulnerability class; privilege escalation or horizontal access violations in a healthcare system could expose alarm histories, resident data, or allow an attacker to suppress alarms.
- OWASP A07 (Authentication Failures) covers weak credentials, missing brute-force protection, and improper token handling — all high-likelihood risks for a web-accessible platform.
- The platform distinguishes at minimum between: unauthenticated device/bridge service accounts (MQTT-only, no REST write), care staff (alarm view and acknowledge), and operators/administrators (system configuration, audit log query). Each role must have explicitly defined permissions.

## Acceptance Criteria
1. **Authentication required**: Every REST API endpoint except designated public health-check endpoints (`/api/health`, `/api/messages/latest` if public by design) must reject unauthenticated requests with HTTP 401.
2. **Role enforcement**: Every endpoint that modifies data or queries sensitive records must verify the caller's role. Insufficient role must return HTTP 403, never 404 (which leaks resource existence).
3. **Token standards**: Authentication tokens must be JWT (RS256 or ES256 algorithm) or equivalent signed tokens. HS256 with a weak shared secret is not permitted. Tokens must have an expiry (`exp` claim) of no more than **15 minutes** for access tokens; refresh tokens must be rotated on each use.
4. **Secure storage of tokens (frontend)**: Access tokens must be stored in memory only (not `localStorage` or `sessionStorage`) to prevent XSS token theft. If persistent sessions are required, refresh tokens may be stored in an `HttpOnly`, `Secure`, `SameSite=Strict` cookie.
5. **Brute-force protection**: The authentication endpoint must implement rate limiting (maximum 10 failed attempts per IP per 15-minute window) with a lockout or CAPTCHA escalation.
6. **Token replay prevention**: The system must maintain a server-side token revocation list (deny-list) or use short-lived tokens with mandatory rotation so that a stolen token cannot be replayed after the user logs out.
7. **Service-to-service authentication**: The Spring Boot bridge must authenticate to the backend REST API using a service account token or mTLS, not an anonymous or hardcoded credential embedded in environment variables accessible to all services.

## Verification Method
- Integration test suite includes: unauthenticated request → 401, wrong-role request → 403, valid token → 200.
- Penetration test checks for horizontal privilege escalation (user A accessing user B's data).
- Token expiry test confirms access token is rejected after 15 minutes without refresh.
- `npm audit` and OWASP Dependency Check confirm no auth-library CVEs.

## Traceability
| ID | Linked to |
|----|-----------|
| OWASP-REQ-002 | OWASP Top 10:2021 A01 |
| OWASP-REQ-002 | OWASP Top 10:2021 A07 |
| OWASP-REQ-002 | GDPR-REQ-003 (encryption in transit) |
| OWASP-REQ-002 | GDPR-REQ-005 (audit trail — auth events) |
| OWASP-REQ-002 | OWASP-REQ-003 (sensitive data exposure) |
