# NFR-003 — Security and operations

1. The REST service listens only on loopback by default and requires an explicit configuration change for remote access.
2. Request size, batch count, expression length, and calculation precision are bounded and validated before evaluation.
3. Error responses contain no stack traces, database details, or local file paths.
4. H2's browser console is disabled by default.
5. The Docker image runs as a non-root user, exposes a health check, and persists cache data only through an explicit volume.
6. Local scripts record the service PID, refuse to stop an unrelated process, and provide actionable startup failures.
