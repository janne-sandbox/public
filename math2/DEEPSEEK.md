# Math2 assistant instructions

Follow `AGENTS.md`, the normative files in `requirements/`, and the live sequence in `CONTINUE.md`. Keep `CONTINUE.md` updated with exact status and verification evidence.

Build in this order: specifications, Java calculation library, Spring Boot service and cache, tooling/documentation, then non-Java REST clients. Client work is blocked whenever root `mvn verify` is not green.

Use Java 21 and arbitrary-precision decimal algorithms. Public values and equations are immutable; lossy conversion is an explicit error carrying a nearest value. The service binds to loopback by default, bounds all inputs, and does not expose internal failure details.
