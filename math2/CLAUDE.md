# Math2 assistant instructions

The project builds an arbitrary-precision Java calculation JAR, a local-first Spring Boot REST service with a bounded persistent cache, and clients in C++, C#, JavaScript, Rust, and TypeScript.

Start by reading `CONTINUE.md`, `AGENTS.md`, and the applicable requirement files. Implement the current focus from the tracker and keep the tracker synchronized with completed work, evidence, remaining gaps, and blockers.

Roles by area:

- Specification work owns testable behavior in `requirements/` and one specification per production class.
- Java library work owns immutable values, equation composition/parsing, operators, precision, and unit tests.
- Service work owns the REST/OpenAPI contract, validation, H2/Hibernate/Liquibase cache, integration tests, Docker, and loopback-safe defaults.
- Client work mirrors the Java value/equation API over REST and starts only after the Java reactor is green.
- Documentation work owns README files, the documentation site, and local scripts.

Run `mvn verify` from the repository root for Java verification. Preserve arbitrary precision, deterministic output, explicit error contracts, and backend input limits.
