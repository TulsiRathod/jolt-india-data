package io.github.tulsirathod.joltindiadata.internal;

import io.github.tulsirathod.joltindiadata.rto.RTOInfo;
import io.github.tulsirathod.joltindiadata.rto.RTOLookup;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * Default RTO provider.
 *
 * <p>Record layout:
 * <pre>
 *   uint16 nameLen          free-form, UTF-8
 *   bytes  name
 *   uint16 cityIdx          dictionary index
 *   uint16 stateIdx         dictionary index
 * </pre>
 *
 * <p>CSV columns: {@code code, name, city, state}.
 */
public final class RTOLookupImpl implements RTOLookup {

    static final String BASE = "/jolt-india-data/rto";

    private volatile BinaryReader reader;

    @Override
    public Optional<RTOInfo> lookup(String key) {
        if (key == null) return Optional.empty();
        return reader().lookup(key, RTOLookupImpl::decode);
    }

    private BinaryReader reader() {
        BinaryReader r = reader;
        if (r != null) return r;
        synchronized (this) {
            if (reader == null) {
                reader = ResourceBootstrap.load(BASE, RTOLookupImpl::encode);
            }
            return reader;
        }
    }

    static RTOInfo decode(ByteBuffer in, String key, String[] dict) {
        int nameLen = in.getShort() & 0xFFFF;
        byte[] name = new byte[nameLen];
        in.get(name);
        int cityIdx = in.getShort() & 0xFFFF;
        int stateIdx = in.getShort() & 0xFFFF;
        return new RTOInfo(key, new String(name, StandardCharsets.UTF_8), dict[cityIdx], dict[stateIdx]);
    }

    static void encode(BinaryWriter w, CsvParser.Table table) {
        List<String> header = table.header();
        int iCode = required(header, "code");
        int iName = required(header, "name");
        int iCity = required(header, "city");
        int iState = required(header, "state");

        for (List<String> row : table.rows()) {
            byte[] name = row.get(iName).getBytes(StandardCharsets.UTF_8);
            int cityIdx = w.intern(row.get(iCity));
            int stateIdx = w.intern(row.get(iState));
            ByteBuffer rec = ByteBuffer.allocate(2 + name.length + 2 + 2);
            rec.putShort((short) name.length);
            rec.put(name);
            rec.putShort((short) cityIdx);
            rec.putShort((short) stateIdx);
            w.addRecord(row.get(iCode), rec.array());
        }
    }

    private static int required(List<String> header, String name) {
        int i = header.indexOf(name);
        if (i < 0) throw new IllegalArgumentException("Missing CSV column: " + name);
        return i;
    }
}
