package io.github.tulsirathod.joltindiadata.ifsc;

import io.github.tulsirathod.joltindiadata.Lookup;

/**
 * Lookup of IFSC code to {@link IFSCInfo}. The provider implementation lives
 * in {@code jolt-india-data-ifsc} and is discovered via {@link java.util.ServiceLoader}.
 */
public interface IFSCLookup extends Lookup<String, IFSCInfo> {}
