# NFR-002 — Quality and packaging

1. The Java modules build as a Maven reactor and the calculation library is usable as a standalone JAR.
2. Production Java code targets Java 21 and follows package root `com.nologo2.math2`.
3. Unit tests cover every public class, operator, error condition, conversion boundary, and parser precedence rule.
4. REST integration tests use the actual H2/Liquibase configuration.
5. The service is distributable as an executable Spring Boot JAR and Docker image.
6. Shell scripts use strict error handling and work when invoked outside the repository directory.
7. Runtime database, PID, log, build-output, IDE, and operating-system files are ignored by version control.
8. Public APIs and configuration properties are documented.
