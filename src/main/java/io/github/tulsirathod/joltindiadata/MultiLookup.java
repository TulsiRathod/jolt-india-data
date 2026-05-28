package io.github.tulsirathod.joltindiadata;

import java.util.List;

/**
 * Multi-result lookup, used when a single key may legitimately resolve to
 * more than one record. HSN is the only such case in this library: a single
 * code can carry several GST rates discriminated by product variant.
 *
 * <p>Callers must disambiguate using the per-record fields (e.g.
 * {@code description}, {@code effectiveFrom}); this library surfaces the
 * data without guessing the intended variant.
 *
 * @param <K> lookup key type
 * @param <V> result record type
 */
public interface MultiLookup<K, V> {

    /**
     * @param key the dataset key; never {@code null}
     * @return all matching records, or an empty list if the key is unknown.
     *         Order reflects the order in the underlying binary file and
     *         should not be relied upon by callers. Implementations must
     *         not return {@code null}.
     */
    List<V> lookup(K key);
}
