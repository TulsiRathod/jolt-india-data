package io.github.tulsirathod.joltindiadata.hsn;

import java.time.LocalDate;

/**
 * GST rate applicable to an HSN code variant. A single HSN may carry
 * multiple {@code HSNRate}s when the rate depends on product variant
 * (e.g. branded vs unbranded) or has changed over time; see
 * {@link HSNLookup}.
 *
 * @param hsn            HSN code (echoed back from the lookup key)
 * @param rate           combined GST rate as a percentage (e.g. {@code 18.0})
 * @param description    verbatim description from the CBIC notification;
 *                       callers match against this to pick the variant
 * @param effectiveFrom  the date this rate became applicable
 */
public record HSNRate(
        String hsn,
        double rate,
        String description,
        LocalDate effectiveFrom
) {}
