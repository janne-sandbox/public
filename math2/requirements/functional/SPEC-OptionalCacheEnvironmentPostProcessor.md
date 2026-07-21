# Class specification — OptionalCacheEnvironmentPostProcessor

`OptionalCacheEnvironmentPostProcessor` runs after configuration data is available. When `math2.cache.enabled` is false, it excludes datasource, JPA, repository, and Liquibase auto-configuration so calculation remains available without a database.
