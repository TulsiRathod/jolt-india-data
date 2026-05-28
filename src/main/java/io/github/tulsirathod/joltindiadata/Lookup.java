package io.github.tulsirathod.joltindiadata;

import java.util.Optional;

/**
 * Single-result lookup over an India-specific dataset.
 *
 * <p>Implementations are expected to be effectively immutable and safe for
 * concurrent use after construction. Data loading may be lazy (deferred to
 * the first {@link #lookup} call); see the {@code jolt-india-data} loader
 * contract for the two-tier strategy.
 *
 * @param <K> lookup key type (typically {@code String} for codes like
 *            {@code "HDFC0001234"} or {@code "395001"})
 * @param <V> result record type
 */
public interface Lookup<K, V> {

    /**
     * @param key the dataset key; never {@code null}
     * @return the matching record, or {@link Optional#empty()} if the key is
     *         unknown. Implementations must not return {@code null}.
     */
    Optional<V> lookup(K key);
}
