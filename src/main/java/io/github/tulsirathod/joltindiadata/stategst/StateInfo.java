package io.github.tulsirathod.joltindiadata.stategst;

/**
 * State / Union Territory record keyed by the two-digit GST state code
 * (the leading two digits of a GSTIN).
 *
 * @param code     two-digit GST state code, e.g. {@code "24"} for Gujarat
 * @param name     full state or UT name
 * @param isUT     {@code true} if the entry is a Union Territory rather than
 *                 a state; affects which GST components apply (UTGST vs SGST)
 */
public record StateInfo(
        String code,
        String name,
        boolean isUT
) {}
