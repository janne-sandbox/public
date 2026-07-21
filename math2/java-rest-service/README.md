# Math2 REST service

Spring Boot 4 service for the Java calculation API. It provides single and ordered batch GET endpoints, Swagger UI, health reporting, bounded request validation, and a file-backed H2 LRU result cache managed by Hibernate and Liquibase.

Run from the repository root with `./run-dev-local.sh`. The default cache database is under `.math2/`, cache size is 100, and the listener is restricted to `127.0.0.1` unless explicitly configured.

Set `MATH2_CACHE_ENABLED=false` to run without any datasource, Hibernate, repository, or Liquibase initialization. Persisted cache misses include their calculation duration in milliseconds. `MATH2_MAX_LENGTH` configures the maximum significant-digit precision (default 1000, maximum 1,000,000); expressions may use the precision-aware constant `pi`.

Build the image with:

```bash
docker build -f java-rest-service/Dockerfile -t math2:local .
```
