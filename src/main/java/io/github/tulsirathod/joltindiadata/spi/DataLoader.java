package io.github.tulsirathod.joltindiadata.spi;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Loader contract for opening a packaged {@code .jid} resource as a
 * read-only {@link ByteBuffer}. Per {@code PLAN.md} §4, the default
 * implementation is two-tier:
 *
 * <ol>
 *   <li><b>Preferred — memory-mapped temp file.</b> Decompress the gzipped
 *       classpath resource into
 *       {@code ${java.io.tmpdir}/jolt-india-data/<artifactId>-<dataVersion>.bin}
 *       and {@link java.nio.channels.FileChannel#map} it as
 *       {@link java.nio.channels.FileChannel.MapMode#READ_ONLY}. Subsequent
 *       JVMs share the OS page cache.</li>
 *   <li><b>Fallback — direct buffer.</b> If the temp-dir write fails
 *       (read-only filesystem, sandbox, security manager), decompress
 *       fully into a {@link ByteBuffer#allocateDirect direct ByteBuffer}.
 *       Always works, slower startup, no cross-process sharing.</li>
 * </ol>
 *
 * Implementations must log (INFO) which tier was used on first load so
 * operators can diagnose surprising performance. The returned buffer is
 * read-only; callers must {@link ByteBuffer#duplicate duplicate} it before
 * mutating position/limit.
 */
public interface DataLoader {

    /**
     * Opens a packaged dataset resource and returns a read-only buffer over
     * its decompressed contents.
     *
     * @param resourcePath classpath-relative path to the gzipped resource,
     *                     e.g. {@code "/jolt-india-data/ifsc.jid.gz"}
     * @return a read-only {@link ByteBuffer} positioned at 0
     * @throws IOException if the resource cannot be read or decompressed
     */
    ByteBuffer open(String resourcePath) throws IOException;
}
