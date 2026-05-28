package io.github.tulsirathod.joltindiadata.rto;

/**
 * Regional Transport Office record, keyed by RTO code (e.g. {@code "GJ05"}).
 * The code is the leading state-code + district-code prefix of a vehicle
 * registration plate.
 *
 * @param code  RTO code, e.g. {@code "GJ05"}
 * @param name  office name as published by the state transport department
 * @param city  city the office is located in
 * @param state state (full name)
 */
public record RTOInfo(
        String code,
        String name,
        String city,
        String state
) {}
