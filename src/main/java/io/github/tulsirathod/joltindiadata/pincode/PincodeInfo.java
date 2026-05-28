package io.github.tulsirathod.joltindiadata.pincode;

/**
 * Pincode-to-location record, sourced from the India Post All India Pincode
 * Directory. A pincode covers one or more delivery offices; this record
 * collapses to the predominant city/district/state/region pairing.
 *
 * @param pincode  6-digit pincode, e.g. {@code "395001"}
 * @param city     primary city/town
 * @param district administrative district
 * @param state    state (full name)
 * @param region   postal region (e.g. {@code "Western"}); the eight regions
 *                 defined by India Post
 */
public record PincodeInfo(
        String pincode,
        String city,
        String district,
        String state,
        String region
) {}
