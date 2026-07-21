# Math2 repository guidance

Read `CONTINUE.md` before substantive work and update it before handing off. Requirements under `requirements/` are normative; change the relevant requirement before intentionally changing behavior.

Work in dependency order: specifications, `java-api`, `java-rest-service`, documentation/tooling, then REST client libraries. Do not begin or extend non-Java clients unless `mvn verify` passes for the complete Java reactor.

Use Java 21, Maven, package root `com.nologo2.math2`, immutable public math values, `BigDecimal`-based calculation, and `HALF_EVEN` rounding. Never replace arbitrary-precision calculations with primitive floating-point. Keep REST exposure on `127.0.0.1` by default and validate all externally controlled limits.

Every production class needs its own specification file and focused tests. Record exact verification commands, results, and blockers in `CONTINUE.md`; do not describe unexecuted validation as passing.
