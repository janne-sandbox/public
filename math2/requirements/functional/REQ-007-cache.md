# REG-007 — Calculation cache

## Requirements

1. Successful calculations are cached in a file-backed H2 database managed by Hibernate and Liquibase.
2. The key contains the canonical equation, requested precision, and calculation-engine version.
3. Cached results are observationally identical to fresh results; failures are not cached.
4. Reading an entry updates its last-access time.
5. After insertion, least-recently-used entries are removed until the configured maximum is met.
6. The default maximum is 100; a positive configuration value overrides it. Zero disables storage and clears no existing data implicitly.
7. Concurrent requests for the same key do not produce inconsistent entries.
8. Timestamps are stored in UTC and cache records contain creation and last-access times.
9. Each stored entry records the cache-miss calculation duration in whole milliseconds.
10. Setting `math2.cache.enabled=false` disables cache reads/writes and removes datasource, Hibernate, repository, and Liquibase startup requirements; calculations continue without persistence.
