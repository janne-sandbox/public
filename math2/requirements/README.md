# Math2 requirements

The requirement set is normative for implementation and tests. Functional files use the `REG` prefix; cross-cutting constraints use `NFR`.

Ambiguities in the original concept note are resolved in favor of deterministic decimal behavior, immutable public values, bounded inputs, and a small versioned REST contract. Any behavior change must update the relevant requirement and `CONTINUE.md` before code changes.
