package io.github.tulsirathod.joltindiadata.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.zip.GZIPInputStream;

/**
 * Shared first-load logic for a Lookup backed by a packaged dataset.
 *
 * <p>Resolution order:
 * <ol>
 *   <li>{@code <baseName>.jid.gz} on the classpath — preferred, produced by
 *       the {@code Builder}.</li>
 *   <li>{@code <baseName>.csv} on the classpath — fallback used for the
 *       small sample datasets shipped with v0.1. The CSV is parsed and
 *       converted to the binary format in memory so the lookup path is
 *       identical to the production case.</li>
 * </ol>
 *
 * <p>If neither resource exists, throws {@link IllegalStateException} with
 * the dataset name.
 */
public final class ResourceBootstrap {

    private ResourceBootstrap() {}

    public interface CsvToBinary {
        void encode(BinaryWriter writer, CsvParser.Table table);
    }

    /**
     * @param baseName classpath path (no extension), e.g.
     *                 {@code "/jolt-india-data/ifsc"}
     */
    public static BinaryReader load(String baseName, CsvToBinary csvEncoder) {
        ByteBuffer buf = loadBinary(baseName + ".jid.gz");
        if (buf == null) {
            buf = loadFromCsv(baseName + ".csv", csvEncoder);
        }
        if (buf == null) {
            throw new IllegalStateException(
                    "No dataset found on classpath: " + baseName + ".jid.gz or " + baseName + ".csv");
        }
        return new BinaryReader(buf);
    }

    private static ByteBuffer loadBinary(String path) {
        try (InputStream in = ResourceBootstrap.class.getResourceAsStream(path)) {
            if (in == null) return null;
            try (GZIPInputStream gz = new GZIPInputStream(in)) {
                return ByteBuffer.wrap(gz.readAllBytes());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + path, e);
        }
    }

    private static ByteBuffer loadFromCsv(String path, CsvToBinary encoder) {
        try (InputStream in = ResourceBootstrap.class.getResourceAsStream(path)) {
            if (in == null) return null;
            CsvParser.Table table = CsvParser.parse(in);
            BinaryWriter w = new BinaryWriter();
            encoder.encode(w, table);
            byte[] bin = w.build();
            return ByteBuffer.wrap(bin);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + path, e);
        }
    }

    /** Decompress a gzipped byte array into a heap ByteBuffer. */
    public static ByteBuffer ungzip(byte[] gzipped) throws IOException {
        try (GZIPInputStream gz = new GZIPInputStream(new ByteArrayInputStream(gzipped))) {
            return ByteBuffer.wrap(gz.readAllBytes());
        }
    }
}
