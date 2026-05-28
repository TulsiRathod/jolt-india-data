package io.github.tulsirathod.joltindiadata.internal;

import io.github.tulsirathod.joltindiadata.spi.DataLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

/**
 * Default {@link DataLoader}. See PLAN.md §4.
 *
 * <p>On first call, tries to decompress the gzipped classpath resource into
 * {@code ${java.io.tmpdir}/jolt-india-data/<artifactId>-<dataVersion>.bin}
 * and memory-map it. If that fails (read-only tmp dir, sandbox, security
 * manager block, etc.) it falls back to a direct {@link ByteBuffer}.
 *
 * <p>Logs at {@link Level#INFO} which tier was used the first time a given
 * resource is loaded.
 */
public final class TwoTierDataLoader implements DataLoader {

    private static final Logger LOG = Logger.getLogger(TwoTierDataLoader.class.getName());
    private static final String TMP_SUBDIR = "jolt-india-data";

    private final String dataVersion;

    public TwoTierDataLoader(String dataVersion) {
        this.dataVersion = dataVersion;
    }

    @Override
    public ByteBuffer open(String resourcePath) throws IOException {
        try {
            return openMapped(resourcePath);
        } catch (Throwable mmFailure) {
            LOG.log(Level.INFO,
                    "Memory-map tier unavailable for {0} ({1}); falling back to direct buffer",
                    new Object[]{resourcePath, mmFailure.getMessage()});
            return openDirect(resourcePath);
        }
    }

    private ByteBuffer openMapped(String resourcePath) throws IOException {
        Path tmpDir = Paths.get(System.getProperty("java.io.tmpdir"), TMP_SUBDIR);
        Files.createDirectories(tmpDir);
        String safeName = resourcePath.replace('/', '_').replace('\\', '_')
                .replaceFirst("^_+", "");
        Path target = tmpDir.resolve(safeName + "-" + dataVersion + ".bin");

        if (!Files.exists(target)) {
            decompressTo(resourcePath, target);
            LOG.log(Level.INFO, "Extracted {0} to {1} (mmap tier)",
                    new Object[]{resourcePath, target});
        } else {
            LOG.log(Level.FINE, "Reusing extracted {0} ({1})",
                    new Object[]{resourcePath, target});
        }

        try (FileChannel ch = FileChannel.open(target, StandardOpenOption.READ)) {
            return ch.map(MapMode.READ_ONLY, 0, ch.size()).asReadOnlyBuffer();
        }
    }

    private void decompressTo(String resourcePath, Path target) throws IOException {
        Path tmp = Files.createTempFile(target.getParent(), "ingest-", ".part");
        try (InputStream in = openResource(resourcePath);
             GZIPInputStream gz = new GZIPInputStream(in)) {
            Files.copy(gz, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        try {
            Files.move(tmp, target, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Files.move(tmp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private ByteBuffer openDirect(String resourcePath) throws IOException {
        try (InputStream in = openResource(resourcePath);
             GZIPInputStream gz = new GZIPInputStream(in)) {
            byte[] all = gz.readAllBytes();
            ByteBuffer direct = ByteBuffer.allocateDirect(all.length);
            direct.put(all);
            direct.flip();
            LOG.log(Level.INFO, "Loaded {0} via direct buffer ({1} bytes)",
                    new Object[]{resourcePath, all.length});
            return direct.asReadOnlyBuffer();
        }
    }

    private InputStream openResource(String resourcePath) throws IOException {
        InputStream in = getClass().getResourceAsStream(resourcePath);
        if (in == null) {
            throw new IOException("Resource not found on classpath: " + resourcePath);
        }
        return in;
    }

}
