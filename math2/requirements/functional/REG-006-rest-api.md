# REG-006 — REST calculation API

## Endpoints

1. `GET /api/v1/calculate?equation={expression}` evaluates one URL-encoded equation.
2. `GET /api/v1/calculate/batch?equations={csv}` evaluates comma-separated URL-encoded equations and returns results in input order.
3. Function argument commas make plain CSV ambiguous; callers must percent-encode commas inside an equation. An empty batch element is invalid.
4. An optional positive `precision` query parameter applies to every equation in a request and defaults to the configured library maximum.
5. Single success returns `{ "result": "..." }`; batch success returns `{ "results": ["...", "..."] }`.
6. Invalid input returns HTTP 400 with `{ "code", "message", "position"? }`. Unexpected errors return HTTP 500 with a generic message.
7. The service publishes an OpenAPI document and Swagger UI.
8. The server binds to `127.0.0.1` by default; normal Spring configuration can override the address and port.
9. Expressions accept the `pi` constant; its digit count follows the request precision or configured default.
