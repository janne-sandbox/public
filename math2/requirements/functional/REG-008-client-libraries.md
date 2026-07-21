# REG-008 — REST client libraries

## Requirements

1. C++, C#, JavaScript, Rust, and TypeScript libraries expose their own immutable `Number` and composable `Equation` APIs modeled on the Java API.
2. Client `calculate` and batch calculation methods invoke REG-006 and preserve result ordering.
3. Clients default to `http://127.0.0.1:8080` and accept an alternate base URL and request timeout.
4. Clients distinguish validation responses, service failures, transport failures, and malformed responses.
5. Each client documents installation and includes unit tests plus a runnable integration example.
6. Client implementation begins only after the complete Java reactor test suite passes.
