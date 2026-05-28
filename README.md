# jolt-india-data

India-specific lookup datasets (IFSC, pincode, HSN→GST, RTO, GST state code) for Java — a companion to [Jolt](https://github.com/TulsiRathod/Jolt).

## Status

**0.1.0 (pre-release).** The library code, public API, binary record format, two-tier loader, builder pipeline, CI workflows, and Maven Central publishing config are ready. Datasets ship as samples for now (state-GST is complete; others are 10–20 representative rows). The 0.1.0 jar is being prepared for Maven Central — see [RELEASING.md](RELEASING.md) for the publish walkthrough. Replacing the samples with full upstream data is a one-command refresh — see [Refreshing data](#refreshing-data).

The public API surface is `IndiaData` plus the per-domain `Lookup` interfaces and record types under `*.ifsc`, `*.pincode`, `*.hsn`, `*.rto`, `*.stategst`. Everything under `internal`, `spi`, and `builder` is implementation detail and not part of the SemVer contract (and is hidden from JPMS consumers via `module-info`).

## Quick start

```xml
<dependency>
  <groupId>io.github.tulsirathod</groupId>
  <artifactId>jolt-india-data</artifactId>
  <version>0.1.0</version>
</dependency>
```

```java
IndiaData data = IndiaData.create();

Optional<IFSCInfo>    bank   = data.ifsc().lookup("HDFC0000001");
Optional<PincodeInfo> pin    = data.pincode().lookup("395001");
List<HSNRate>         rates  = data.hsn().lookup("6109");      // multi-rate
Optional<RTOInfo>     rto    = data.rto().lookup("GJ05");
Optional<StateInfo>   state  = data.stateGst().lookup("24");
```

`IndiaData.create()` is cheap. Datasets are loaded lazily by ServiceLoader on first access and decoded from the bundled `.jid.gz` files via the two-tier loader (memory-mapped temp file by default, direct-buffer fallback when tmp is unavailable). See `BinaryFormat` for the wire-format spec.

## What's bundled today

| Dataset    | Rows (sample) | Source for full refresh                                 |
| ---------- | ------------: | ------------------------------------------------------- |
| IFSC       |            12 | RBI master list (rbi.org.in)                            |
| Pincode    |            15 | India Post All India Pincode Directory (indiapost.gov.in) |
| HSN→GST    |            15 | CBIC GST rate notifications (cbic.gov.in)               |
| RTO        |            16 | State transport department public listings              |
| GST state  |     40 (full) | GSTN public state-code list                             |

## Refreshing data

The `Builder` reads CSVs from `data/raw/` and writes gzipped `.jid` files into `src/main/resources/jolt-india-data/`:

```bash
# Drop the latest upstream extracts into data/raw/, then:
mvn compile exec:java -Dexec.mainClass=io.github.tulsirathod.joltindiadata.builder.Builder

# Or refresh only a subset:
mvn compile exec:java \
  -Dexec.mainClass=io.github.tulsirathod.joltindiadata.builder.Builder \
  -Dexec.args="ifsc pincode"
```

A scheduled GitHub Action ([refresh-data.yml](.github/workflows/refresh-data.yml)) runs the builder weekly and opens a PR with the diff. Until the upstream fetch step is wired (RBI XLSX → CSV is the main outstanding piece), the workflow is a stub maintainers complete per source.

## Module / dataset shape

Originally the plan called for one Maven module per dataset (see [PLAN.md](PLAN.md) §2). The current shape is a single jar bundling all five datasets. Reasons to revisit:
- IFSC at full size (~170k rows) pushes the jar past the comfortable Maven Central budget (PLAN §8 risk #2). When that happens, IFSC moves to a separate artifact loaded on demand.
- Consumers who want only pincode lookups still pay the IFSC jar weight. Per-dataset modules become valuable once any single dataset is over a few MB compressed.

## Build & test

```bash
mvn verify
```

JDK 17+, no other runtime dependencies. Tests are JUnit 5 and run as part of `verify`.

## Release

The full release walkthrough — Sonatype Central setup, GPG key, namespace verification, deploy steps — is in [RELEASING.md](RELEASING.md). Once that's set up:

```bash
mvn -Prelease deploy
```

The `release` profile attaches sources + javadoc, signs artefacts with GPG, and stages via the Sonatype Central publishing plugin. `autoPublish` is off — review the staged deployment in the Central portal before releasing.

## License

MIT for code (see [LICENSE](LICENSE)). Bundled datasets carry their upstream attribution; see [NOTICE](NOTICE).
