# Math2 requirements

The requirement set is normative for implementation and tests. Functional files use the `REG` prefix; cross-cutting constraints use `NFR`.

Ambiguities in the original concept note are resolved in favor of deterministic decimal behavior, immutable public values, bounded inputs, and small versioned transport contracts. Any behavior change must update the relevant requirement and `CONTINUE.md` before code changes.

`REG-009` and `REG-010` describe target GPU/backend-planning and UDP transport
capabilities. Their presence is not evidence that those capabilities are implemented;
the current state and executed verification remain recorded in `CONTINUE.md`.
