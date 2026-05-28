# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project
adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0] - 2026-05-28

Initial release.

### Added
- `IndiaData` entry point with lazy ServiceLoader-discovered providers.
- Lookups for IFSC, Pincode, HSN→GST, RTO, and GST state code.
- `GST state code` table shipped complete (40 codes including legacy 25 and 28).
- Sample datasets for IFSC, Pincode, HSN, and RTO (10–20 rows each), wired
  through the binary record format so the lookup path is exercised
  end-to-end. Replace via `Builder` once authoritative extracts are available.
- Hand-rolled `.jid` binary record format: sorted key index, string
  interning, gzip-at-rest.
- Two-tier `DataLoader`: memory-mapped temp file with direct-buffer fallback
  when tmp is unavailable.
- `Builder` CLI for converting CSV extracts to `.jid.gz` artefacts (not part
  of the published API — excluded from the jar).
- Java Platform Module System support: module
  `io.github.tulsirathod.joltindiadata` exports public packages only.
- GitHub Actions CI (JDK 17/21 matrix) and scheduled data-refresh workflow.
- Maven Central publishing config (`-Prelease`) with GPG signing and the
  Sonatype Central publishing plugin.

### Notes
- The published API surface is `IndiaData` plus the per-domain `Lookup`
  interfaces and record types. Everything under `internal`, `spi`, and
  `builder` is implementation detail.
- The refresh workflow currently has a stub for the upstream-fetch step.
  Wiring each source (RBI XLSX, India Post CSV, CBIC notifications) is
  on the v0.2 roadmap.
