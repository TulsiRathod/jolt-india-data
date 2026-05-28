package io.github.tulsirathod.joltindiadata.internal;

import io.github.tulsirathod.joltindiadata.spi.BinaryFormat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Reader for {@code .jid} binary record files. See {@link BinaryFormat}.
 *
 * <p>Construction parses the header + string dictionary + key index;
 * record payloads are accessed on demand via {@link #lookup} /
 * {@link #lookupAll}.
 *
 * <p>This class is safe for concurrent reads. Each {@code lookup} call
 * {@link ByteBuffer#duplicate duplicates} the underlying buffer before
 * positioning, so multiple threads don't trample each other's cursor.
 */
public final class BinaryReader {

    private final ByteBuffer payload;
    private final String[] dictionary;
    private final String[] keys;
    private final int[] offsets;

    public BinaryReader(ByteBuffer buf) {
        ByteBuffer b = buf.duplicate().order(ByteOrder.BIG_ENDIAN);
        b.position(0);

        int magic = b.getInt();
        if (magic != BinaryFormat.MAGIC) {
            throw new IllegalArgumentException(
                    "Not a .jid file (bad magic 0x" + Integer.toHexString(magic) + ")");
        }
        short version = b.getShort();
        if (version != BinaryFormat.FORMAT_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported .jid format version: " + version);
        }
        b.getShort(); // reserved
        int recordCount = b.getInt();
        int stringCount = b.getInt();

        this.dictionary = new String[stringCount];
        for (int i = 0; i < stringCount; i++) {
            int len = b.getShort() & 0xFFFF;
            byte[] s = new byte[len];
            b.get(s);
            dictionary[i] = new String(s, StandardCharsets.UTF_8);
        }

        this.keys = new String[recordCount];
        this.offsets = new int[recordCount];
        for (int i = 0; i < recordCount; i++) {
            int len = b.getShort() & 0xFFFF;
            byte[] k = new byte[len];
            b.get(k);
            keys[i] = new String(k, StandardCharsets.UTF_8);
            offsets[i] = b.getInt();
        }

        this.payload = b.slice().order(ByteOrder.BIG_ENDIAN);
    }

    public int recordCount() {
        return keys.length;
    }

    public String stringAt(int idx) {
        return dictionary[idx];
    }

    /** Single-record lookup. Returns the first match if duplicates exist. */
    public <T> Optional<T> lookup(String key, Decoder<T> decoder) {
        int i = binarySearch(key);
        if (i < 0) return Optional.empty();
        return Optional.of(decode(i, decoder));
    }

    /** Multi-record lookup (HSN). Returns all records sharing {@code key}. */
    public <T> List<T> lookupAll(String key, Decoder<T> decoder) {
        int i = binarySearch(key);
        if (i < 0) return Collections.emptyList();
        // walk backwards to first match
        while (i > 0 && keys[i - 1].equals(key)) i--;
        List<T> out = new ArrayList<>();
        while (i < keys.length && keys[i].equals(key)) {
            out.add(decode(i, decoder));
            i++;
        }
        return out;
    }

    private int binarySearch(String key) {
        int lo = 0, hi = keys.length - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            int cmp = keys[mid].compareTo(key);
            if (cmp < 0) lo = mid + 1;
            else if (cmp > 0) hi = mid - 1;
            else return mid;
        }
        return -1;
    }

    private <T> T decode(int i, Decoder<T> decoder) {
        ByteBuffer slice = payload.duplicate().order(ByteOrder.BIG_ENDIAN);
        slice.position(offsets[i]);
        return decoder.decode(slice, keys[i], dictionary);
    }

    /** Per-dataset record decoder. */
    @FunctionalInterface
    public interface Decoder<T> {
        T decode(ByteBuffer in, String key, String[] dictionary);
    }
}
