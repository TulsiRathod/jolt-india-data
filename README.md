# jolt-india-data

India-specific lookup datasets (IFSC, pincode, HSN→GST, RTO) for Java — a companion to [Jolt](https://github.com/TulsiRathod/Jolt).

## Status

**Planning phase — no code yet.** The architecture is captured in [PLAN.md](PLAN.md). Nothing in this repo is ready to use.

## Why a separate repo

The lookups bundled here ship with multi-MB data files and need a weekly refresh pipeline (RBI publishes IFSC updates monthly; CBIC issues HSN changes around the budget). Keeping them out of Jolt means:

- Jolt's jar stays small and zero-dependency.
- This repo's scheduled data-refresh PRs don't pollute Jolt's commit history.
- License/attribution risk from public-sector data sources stays scoped to this artifact.

## Planned scope

See [PLAN.md](PLAN.md) for the full design. In short:

- `jolt-india-data-core` — shared loader API and binary record format.
- `jolt-india-data-ifsc` — IFSC code → bank/branch/city/state, RTGS/NEFT/IMPS flags.
- `jolt-india-data-pincode` — pincode → city/district/state/region.
- `jolt-india-data-hsn` — HSN code → applicable GST rate(s).
- `jolt-india-data-rto` — RTO code → name/city/state.
- `jolt-india-data-state-gst` — GST state code → state name.
- `jolt-india-data-all` — convenience BOM importing all of the above.

## First milestone

End-to-end IFSC: real RBI ingestion, binary format, refresh CI, one Maven Central release. Other modules follow in successive milestones.

## License

MIT for code. Per-module `NOTICE` files will record attribution for the public-sector datasets bundled into each artifact (RBI for IFSC, India Post for pincode, CBIC for HSN).
