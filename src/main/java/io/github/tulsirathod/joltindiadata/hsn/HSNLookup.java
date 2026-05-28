package io.github.tulsirathod.joltindiadata.hsn;

import io.github.tulsirathod.joltindiadata.MultiLookup;

/**
 * HSN-to-GST-rate lookup. Returns {@link java.util.List} (never
 * {@link java.util.Optional}) because a single HSN may have multiple
 * applicable rates, discriminated by {@link HSNRate#description()} and
 * {@link HSNRate#effectiveFrom()}.
 */
public interface HSNLookup extends MultiLookup<String, HSNRate> {}
