package io.github.tulsirathod.joltindiadata.ifsc;

/**
 * Bank-branch record keyed by IFSC (Indian Financial System Code).
 *
 * <p>Sourced from the RBI master list; see the {@code NOTICE} in
 * {@code jolt-india-data-ifsc} for attribution.
 *
 * @param ifsc    the 11-character IFSC code, e.g. {@code "HDFC0001234"}
 * @param bank    bank name, e.g. {@code "HDFC BANK"}
 * @param branch  branch name as RBI publishes it
 * @param city    branch city
 * @param state   branch state (full name, not abbreviation)
 * @param address branch street address, free-form
 * @param rtgs    {@code true} if the branch participates in RTGS
 * @param neft    {@code true} if the branch participates in NEFT
 * @param imps    {@code true} if the branch participates in IMPS
 */
public record IFSCInfo(
        String ifsc,
        String bank,
        String branch,
        String city,
        String state,
        String address,
        boolean rtgs,
        boolean neft,
        boolean imps
) {}
