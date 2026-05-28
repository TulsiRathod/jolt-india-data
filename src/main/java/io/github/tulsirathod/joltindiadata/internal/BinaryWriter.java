package io.github.tulsirathod.joltindiadata.internal;

import io.github.tulsirathod.joltindiadata.spi.BinaryFormat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/**
 * Builder for {@code .jid} binary record files. See
 * {@link BinaryFormat} for the wire-format spec.
 *
 * <p>Usage:
 * <pre>{@code
 *   BinaryWriter w = new BinaryWriter();
 *   for (...) {
 *       ByteArrayOutputStream rec = new ByteArrayOutputStream();
 *       // write fields; use w.intern(s) for strings worth interning
 *       w.addRecord(key, rec.toByteArray());
 *   }
 *   byte[] jid = w.build();         // raw .jid bytes
 *   w.writeGzip(out, jid);          // optional: compress and stream out
 * }</pre>
 *
 * Records may be added with duplicate keys (HSN multi-rate); order within a
 * key group reflects insertion order. The output is sorted by key.
 */
public final class BinaryWriter {

    private final Map<String, Integer> dictIndex = new HashMap<>();
    private final List<String> dictList = new ArrayList<>();
    private final List<Entry> entries = new ArrayList<>();

    private record Entry(String key, byte[] payload) {}

    /** Returns the dictionary index for {@code s}, adding it if absent. */
    public int intern(String s) {
        Integer idx = dictIndex.get(s);
        if (idx != null) return idx;
        int next = dictList.size();
        if (next > 0xFFFF) {
            throw new IllegalStateException(
                    "String dictionary overflow: more than 65535 distinct values");
        }
        dictIndex.put(s, next);
        dictList.add(s);
        return next;
    }

    /** Adds a record. {@code payload} is the dataset-specific bytes. */
    public void addRecord(String key, byte[] payload) {
        entries.add(new Entry(key, payload));
    }

    /** Builds the complete file as a single byte array. */
    public byte[] build() {
        entries.sort(Comparator.comparing(Entry::key));

        ByteBuffer payloadBuf = ByteBuffer.allocate(totalPayloadBytes()).order(ByteOrder.BIG_ENDIAN);
        int[] offsets = new int[entries.size()];
        for (int i = 0; i < entries.size(); i++) {
            offsets[i] = payloadBuf.position();
            payloadBuf.put(entries.get(i).payload);
        }

        ByteBuffer dictBuf = ByteBuffer.allocate(totalDictBytes()).order(ByteOrder.BIG_ENDIAN);
        for (String s : dictList) {
            byte[] b = s.getBytes(StandardCharsets.UTF_8);
            if (b.length > 0xFFFF) {
                throw new IllegalStateException("Interned string longer than 65535 bytes: " + b.length);
            }
            dictBuf.putShort((short) b.length);
            dictBuf.put(b);
        }

        ByteBuffer indexBuf = ByteBuffer.allocate(totalIndexBytes()).order(ByteOrder.BIG_ENDIAN);
        for (int i = 0; i < entries.size(); i++) {
            byte[] k = entries.get(i).key.getBytes(StandardCharsets.UTF_8);
            if (k.length > 0xFFFF) {
                throw new IllegalStateException("Key longer than 65535 bytes: " + k.length);
            }
            indexBuf.putShort((short) k.length);
            indexBuf.put(k);
            indexBuf.putInt(offsets[i]);
        }

        ByteBuffer out = ByteBuffer.allocate(
                BinaryFormat.HEADER_BYTES + dictBuf.capacity() + indexBuf.capacity() + payloadBuf.capacity())
                .order(ByteOrder.BIG_ENDIAN);
        out.putInt(BinaryFormat.MAGIC);
        out.putShort(BinaryFormat.FORMAT_VERSION);
        out.putShort((short) 0); // reserved
        out.putInt(entries.size());
        out.putInt(dictList.size());
        out.put(dictBuf.array());
        out.put(indexBuf.array());
        out.put(payloadBuf.array());
        return out.array();
    }

    /** Builds and gzips into {@code out}. Closes the gzip stream but not {@code out}. */
    public void writeGzip(OutputStream out) throws IOException {
        byte[] raw = build();
        try (GZIPOutputStream gz = new GZIPOutputStream(out)) {
            gz.write(raw);
        }
    }

    /** Convenience: build, gzip, return bytes. */
    public byte[] buildGzipped() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writeGzip(baos);
        return baos.toByteArray();
    }

    private int totalPayloadBytes() {
        int n = 0;
        for (Entry e : entries) n += e.payload.length;
        return n;
    }

    private int totalDictBytes() {
        int n = 0;
        for (String s : dictList) n += 2 + s.getBytes(StandardCharsets.UTF_8).length;
        return n;
    }

    private int totalIndexBytes() {
        int n = 0;
        for (Entry e : entries) n += 2 + e.key.getBytes(StandardCharsets.UTF_8).length + 4;
        return n;
    }
}
