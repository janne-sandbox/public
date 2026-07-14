# REQ-008: OpenAPI 3.0 Specification for All REST Endpoints

## Requirement ID
REQ-008

## Category
Documentation / API Design

## Priority
High

## Description
Every REST endpoint exposed by the Hello World backend API **shall** be documented using the
[OpenAPI 3.0.3](https://spec.openapis.org/oas/v3.0.3) specification format.  Documentation is
produced automatically from `@openapi` JSDoc annotations in route source files via
`swagger-jsdoc`, and served interactively through Swagger UI (`/docs`) in development mode.  The
machine-readable OpenAPI JSON document shall be available at `/docs-json` at runtime.

## Rationale
- **Discoverability**: developers integrating with the API can explore endpoints, schemas and
  examples without reading source code.
- **Contract-first development**: the spec acts as a stable contract between the backend and any
  future consumers (frontend, bridge, external clients).
- **Automated testing**: the spec can be imported into Postman, Insomnia, or used to generate
  typed client SDKs.
- **Compliance with REQ-004**: this requirement provides the Hello World–specific acceptance
  criteria that supplement the general API documentation policy in REQ-004.

## Scope
All Express route handlers registered in:
- `packages/api/src/features/messages/messages.routes.ts`
- `packages/api/src/features/heartbeat/heartbeat.routes.ts`
- any future feature route files added under `packages/api/src/features/`

## Acceptance Criteria

### Specification completeness
1. Every HTTP endpoint (method + path) has a corresponding `@openapi` annotation.
2. Every `@openapi` annotation includes:
   - `summary` — one-line human-readable description
   - `description` (optional but recommended for non-trivial endpoints)
   - `tags` — at least one tag that groups the endpoint logically
   - `requestBody` (where applicable) with a `$ref` to a named component schema
   - `responses` — at least one success status and every documented error status, each with a
     `$ref` or inline schema
3. All schemas referenced via `$ref: '#/components/schemas/…'` are defined in the
   `components.schemas` section of the OpenAPI definition.

### Schema definitions (components.schemas)
The following named schemas **shall** be defined:

| Schema | Description |
|--------|-------------|
| `Message` | Persisted greeting message row (`id`, `text`, `version`, `createdAt`) |
| `ApiError` | Uniform error envelope (`error: string`) |
| `UpdateMessageBody` | POST /api/messages request body (`text: string`) |
| `HeartbeatBody` | POST /api/heartbeat request body (`deviceId: string`) |

### Swagger UI
1. Swagger UI is mounted at `/docs` when `NODE_ENV=development`.
2. The raw OpenAPI JSON is available at `/docs-json` when `NODE_ENV=development`.
3. Swagger UI is **disabled** (route not registered) in production builds.

### Tags
The top-level `tags` array in the spec definition shall include at least:
- `Messages` — greeting message history and updates
- `Heartbeat` — device heartbeat recording

### Info block
The `info` object shall include:
- `title`: `Hello World API`
- `version`: current semver matching the API package version
- `description`: brief plain-text description of the API purpose

## Non-Goals
- YAML format export is not required (JSON-only is sufficient).
- Authentication / security schemes (Bearer, OAuth2) are out of scope until Keycloak integration
  is complete.
- Client SDK generation is not automated as part of the build; the `/docs-json` endpoint provides
  the source material for manual generation.

## Implementation Notes
- Annotations live directly in route `.ts` files so they stay co-located with the handler code.
- `swagger-jsdoc` reads both the compiled `.js` output (Docker) and the TypeScript source
  (local dev) via the `apis` glob array in `packages/api/src/config/swagger.ts`.
- Adding a new route **requires** a corresponding `@openapi` annotation; this is enforced by
  code review, not tooling.

## References
- REQ-004: API Documentation with OpenAPI v3
- [OpenAPI 3.0.3 Specification](https://spec.openapis.org/oas/v3.0.3)
- [swagger-jsdoc](https://github.com/Surnet/swagger-jsdoc)
- `packages/api/src/config/swagger.ts`
