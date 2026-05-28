# `jolt-india-data` — Architecture Plan

A separate companion library to Jolt that ships India-specific lookup data: IFSC, pincode, MICR, HSN→GST, RTO. Lives outside the Jolt jar so the core library stays small and zero-dep.

This document is the **design**, not the build. Nothing here is implemented yet.

---

## 1. Why a separate library

Pure-code utilities (validators, calculators, formatters) belong in Jolt. Anything backed by a dataset belongs here, for three reasons:

1. **Size.** Raw IFSC + pincode data is ~30–40 MB. Bundling it into Jolt would make every consumer pay for data they may not use.
2. **Release cadence.** Code is released when behavior changes. Data needs a refresh cycle (monthly for IFSC, yearly for pincode/HSN). Mixing them forces awkward versioning.
3. **License provenance.** Code is MIT. Datasets have their own attribution requirements (RBI, India Post, CBIC, derivative datasets). Keeping them in a separate artifact makes the dependency graph honest.

## 2. Modules — what's in scope

Each is a separate Maven artifact under the same multi-module project:

| Artifact                       | Purpose                                            | Approx. rows | Refresh    |
| ------------------------------ | -------------------------------------------------- | ------------ | ---------- |
| `jolt-india-data-core`         | Shared loader API + binary record format. No data. | —            | with code  |
| `jolt-india-data-ifsc`         | IFSC → bank/branch/city/state, RTGS/NEFT/IMPS      | ~170k        | monthly    |
| `jolt-india-data-pincode`      | Pincode → city/district/state/region              | ~155k        | yearly     |
| `jolt-india-data-hsn`          | HSN code → GST rate (CGST/SGST/IGST)              | ~5k          | budget     |
| `jolt-india-data-rto`          | RTO code (e.g. `GJ05`) → RTO name/city/state      | ~1.1k        | rare       |
| `jolt-india-data-state-gst`   | GST state code (01–38) → state                     | 38           | rare       |
| `jolt-india-data-all`         | BOM — depends on all of the above, for convenience | —            | —          |

Users pick what they need: `jolt-india-data-pincode` alone is ~3 MB instead of ~30.

**Explicitly out of scope:** anything tax/legal (income tax slabs, TDS sections, HRA rules). Those go in a *third* library (`jolt-india-tax`) with stronger guarantees — the rules change every budget and accuracy matters more than uptime.

## 3. Data sources & licensing

| Dataset       | Authoritative source                            | License posture                                   |
| ------------- | ----------------------------------------------- | ------------------------------------------------- |
| IFSC          | RBI master XLSX (rbi.org.in)                    | Public-sector data; attribution required          |
| Pincode       | India Post "All India Pincode Directory" CSV    | Public domain                                     |
| HSN→GST       | CBIC notifications                              | Public; rate changes require notification linkage |
| RTO           | Public lists (Wikipedia + state RTO directories)| Compile from public; verify ours                  |
| State GST     | Schedule III, GST Act                           | Public                                            |

**Decision:** ingest directly from the authoritative source (RBI master XLSX for IFSC, India Post CSV for pincode, CBIC notifications for HSN). No derivative datasets — we don't inherit anyone else's license, attribution stays clean, and we control the parsing surface end-to-end. Cost: more parser code (RBI's XLSX schema has changed twice in three years; the builder needs to be tolerant of column reorderings). Document provenance + attribution in each module's `NOTICE` file.

## 4. Storage format

JSON in the jar is wasteful (~3× bigger than necessary) and slow to load. Use a hand-rolled binary format per dataset:

```
[ header (16 bytes) ]
[ sorted key index — offset table ]
[ packed records — variable length strings ]
```

- **Sorted on the lookup key** (IFSC code, pincode digits, etc.) → binary search in O(log n).
- **String interning** for repeated bank names, state names, etc. — a small `String[]` dictionary at the start of the file, records hold indices.
- **Gzipped** at rest in the jar; decompressed on first lookup.

**Loader strategy (decided):** two-tier with automatic fallback.

1. **Preferred — memory-mapped temp file.** On first lookup, extract the gzipped resource into `${java.io.tmpdir}/jolt-india-data/<artifactId>-<dataVersion>.bin` (decompress while writing). Open with `FileChannel.map(READ_ONLY)`. The version suffix in the filename means upgrades extract fresh; we don't need to checksum-validate every JVM start. Cheap re-opens across processes share the OS page cache.
2. **Fallback — direct-buffered.** If the temp-dir write fails (read-only filesystem, sandboxed container, security manager block), fall back to reading the resource fully into a `ByteBuffer.allocateDirect` of the decompressed size. Slower startup, no shared cache across processes, but always works.

Log which tier is in use at INFO level on first load so operators can see what happened. Both tiers expose the same `ByteBuffer`-backed read path, so the lookup code doesn't branch.

Expected sizes after this: IFSC ~3 MB on disk, pincode ~2 MB, HSN ~50 KB.

**Why not SQLite?** It works, and `sqlite-jdbc` is a popular fit. But it adds a 7 MB runtime dependency and a JNI surface — both contrary to the Jolt-family ethos. Hand-rolled binary is ~200 lines of code and zero deps.

## 5. API sketch

```java
// Top-level entry point; cheap to construct, data loaded lazily on first lookup.
IndiaData data = IndiaData.create();

Optional<IFSCInfo>    bank   = data.ifsc().lookup("HDFC0001234");
Optional<PincodeInfo> p      = data.pincode().lookup("395001");
List<HSNRate>         rates  = data.hsn().lookup("1001");      // multi-rate: see below
Optional<RTOInfo>     rto    = data.rto().lookup("GJ05");

// Records, not classes — immutable, equals/hashCode auto-generated.
public record IFSCInfo(
    String ifsc, String bank, String branch,
    String city, String state, String address,
    boolean rtgs, boolean neft, boolean imps
) {}

public record HSNRate(
    String hsn,              // the lookup key, echoed back for convenience
    double rate,             // GST percentage (e.g. 5.0, 12.0, 18.0, 28.0)
    String description,      // product variant this rate applies to
    LocalDate effectiveFrom  // when this rate became applicable
) {}
```

**HSN multi-rate (decided):** `hsn().lookup(code)` returns `List<HSNRate>`, never `Optional`. An unknown code returns an empty list; a code with one applicable rate returns a one-element list; a code with variant-specific rates returns multiple. The caller is responsible for picking the right variant (their domain knows the product better than this library does) — we surface the data, not a guess. The `description` field is a verbatim string from the CBIC notification so callers can match against it.

Each `*Lookup` interface lives in `-core`. A consumer who only depends on `-core` gets the interfaces but no data — useful for testing with mocks.

**No global static state.** `IndiaData.create()` returns an instance you wire into your DI container or hold as a singleton yourself. (Static singletons make memory cleanup impossible and tests painful.)

## 6. Build pipeline & data refresh

A non-published Maven module, `jolt-india-data-builder`, owns the ingestion. It:

1. Downloads raw data from each authoritative source (HTTPS only, checksum-verified where the source publishes one).
2. Validates structure and row counts (catch a malformed RBI export early).
3. Converts to the binary format described in §4.
4. Writes outputs into each module's `src/main/resources/`.

This runs in CI via a scheduled GitHub Actions workflow:

```
.github/workflows/refresh-data.yml
  schedule: cron weekly
  steps:
    - run builder
    - diff vs committed binary
    - if changed: open PR with "data: refresh from <source> on <date>"
```

Maintainers review the PR (sanity-check row count delta, spot-check a few lookups) and merge. A subsequent release-on-merge workflow bumps the affected module's version and publishes to Maven Central.

**Versioning convention:** SemVer for breaking API changes. For data-only refreshes, bump the patch and append a date suffix in the release notes — `2.1.4 (ifsc data: 2026-04-15)`. Users who want pinned data can stay on a known patch; users who want fresh data move forward.

## 7. Repo & distribution

- **Separate GitHub repo:** `TulsiRathod/jolt-india-data`. Different release cadence, different CODEOWNERS, different threat surface from Jolt itself.
- **Multi-module Maven project** with the layout in §2.
- **Coordinates:** `io.github.tulsirathod:jolt-india-data-<module>:<version>`. The `jolt-india-data-all` BOM uses dependency management so consumers can `<scope>import</scope>` it.
- **GPL/license:** MIT for code; per-module `NOTICE` for data attribution.

## 8. Risks & open questions

**Decided (recorded here so the rationale survives):**
- Direct RBI / India Post / CBIC ingestion — no derivative datasets. (§3)
- HSN lookup returns `List<HSNRate>` with `description` + `effectiveFrom` discriminators; caller picks the variant. (§5)
- Loader is two-tier: memory-mapped temp file by default, `ByteBuffer.allocateDirect` fallback if the temp dir is unavailable. (§4)

**Still open:**
1. **Pincode authoritativeness.** India Post's CSV has known errors and is updated irregularly. We may need a "corrections" overlay shipped in the jar, maintained by community PRs.
2. **Jar size budget.** If `jolt-india-data-ifsc` ends up >5 MB compressed, reconsider extracting the IFSC database to a separate downloadable artifact loaded on first run. Maven Central is fine with 10–20 MB jars but consumers feel it in build times.
3. **Privacy posture.** None of this data is personal — bank IDs and pincodes are public. Worth saying so explicitly in the README so consumers don't mistake "India data" for "Indian user PII."

## 9. Suggested first milestone — "spike on IFSC"

Don't build all six modules up front. Build IFSC end-to-end first, including:

- `-core` interfaces (`Lookup<K, V>`, binary format spec, loader)
- `-ifsc` module with real RBI data, builder pipeline, tests
- `-all` BOM with just IFSC in it
- One CI workflow refreshing IFSC monthly
- One Maven Central release

That proves the architecture, the licensing approach, and the refresh pipeline. Add pincode/HSN/RTO afterwards in successive milestones, each adding ~1 week of work.

Stop the spike if any of these turn red:
- Legal review blocks bundling RBI data.
- Compressed jar exceeds 8 MB.
- Lookup latency on a cold JVM exceeds 50 ms for a single IFSC query.

## 10. Out-of-scope (for future thought)

- Tax/TDS/HRA — separate library `jolt-india-tax`, see §2.
- Real-time data (live exchange rates, live UPI status) — needs a service, not a jar.
- KYC / e-Sign integrations — heavily regulated; outside utility-library scope.
- Hindi / regional-language formatting — possible future extension to Jolt's `INRFormat` once that exists.
