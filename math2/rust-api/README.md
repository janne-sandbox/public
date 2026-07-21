# Math2 Rust client

Rust 2021 crate with immutable `Number` and `Equation` values plus a blocking rustls-based REST client.

```rust
let equation = Equation::of(&Number::from_integer(9)).sqrt();
let result = Client::localhost()?.calculate(&equation, None)?;
```

Run `cargo test`. Errors distinguish validation, transport, protocol, and structured service failures.
