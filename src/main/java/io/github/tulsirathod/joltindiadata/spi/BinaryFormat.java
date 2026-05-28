package io.github.tulsirathod.joltindiadata.spi;

/**
 * Wire-format constants and specification for the {@code .jid} (Jolt India
 * Data) binary record file. The format is hand-rolled rather than using
 * JSON / SQLite / Protobuf to keep the runtime zero-dependency; see
 * {@code PLAN.md} §4 for the rationale.
 *
 * <h2>File layout</h2>
 * <pre>
 *   +----------------------------------+
 *   | header                  16 bytes |
 *   +----------------------------------+
 *   | string dictionary       variable |   interned strings, length-prefixed
 *   +----------------------------------+
 *   | key index               variable |   sorted; binary-searchable
 *   +----------------------------------+
 *   | record payload          variable |   variable-length records
 *   +----------------------------------+
 * </pre>
 *
 * <h2>Header (16 bytes, big-endian)</h2>
 * <pre>
 *   offset  size  field
 *   0       4     magic = {@value #MAGIC_STRING}  (bytes 'J','I','D','1')
 *   4       2     formatVersion = {@value #FORMAT_VERSION}
 *   6       2     reserved (must be zero)
 *   8       4     recordCount   (uint32, written as signed; max ~2.1B)
 *   12      4     stringCount   (uint32, written as signed)
 * </pre>
 *
 * <h2>String dictionary</h2>
 * Repeated {@code stringCount} times:
 * <pre>
 *   uint16  byteLength
 *   bytes   UTF-8 bytes
 * </pre>
 * Records reference dictionary entries by index. The dictionary holds
 * repeated values (bank names, state names, regions) so the record payload
 * stores 2-byte references in place of full strings.
 *
 * <h2>Key index</h2>
 * Repeated {@code recordCount} times, sorted ascending by {@code key}:
 * <pre>
 *   uint16  keyLength
 *   bytes   key (UTF-8)
 *   uint32  recordOffset   (byte offset from start of record payload)
 * </pre>
 * Lookup is O(log n) binary search over this index. For multi-result
 * datasets (HSN), consecutive entries may share a key and the caller
 * walks forward until the key changes.
 *
 * <h2>Record payload</h2>
 * Each record's layout is dataset-specific. Convention:
 * <ul>
 *   <li>Fixed-width primitives are written big-endian.</li>
 *   <li>Booleans are packed as a single byte each (or a bitmask byte if
 *       there are 2+ flags in the same record; see IFSC for an example).</li>
 *   <li>Strings are written as {@code uint16} dictionary indices for
 *       interned values, or as inline length-prefixed UTF-8
 *       ({@code uint16} length + bytes) for free-form strings (addresses,
 *       branch names).</li>
 * </ul>
 *
 * <h2>Compression</h2>
 * Files are gzipped at rest inside the jar. The {@link DataLoader} contract
 * decompresses on first use into a {@link java.nio.ByteBuffer}-backed view.
 */
public final class BinaryFormat {

    /** Magic bytes prefixing every {@code .jid} file: ASCII {@code "JID1"}. */
    public static final int MAGIC = 0x4A_49_44_31;

    /** Human-readable form of {@link #MAGIC}. */
    public static final String MAGIC_STRING = "JID1";

    /** Current wire-format version. Bump on any layout change. */
    public static final short FORMAT_VERSION = 1;

    /** Header size in bytes. */
    public static final int HEADER_BYTES = 16;

    private BinaryFormat() {}
}
