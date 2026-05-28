package io.github.tulsirathod.joltindiadata.internal;

import io.github.tulsirathod.joltindiadata.pincode.PincodeInfo;
import io.github.tulsirathod.joltindiadata.pincode.PincodeLookup;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;

/**
 * Default Pincode provider.
 *
 * <p>Record layout: 4 dictionary indices (city, district, state, region) =
 * 8 bytes per record.
 *
 * <p>CSV columns: {@code pincode, city, district, state, region}.
 */
public final class PincodeLookupImpl implements PincodeLookup {

    static final String BASE = "/jolt-india-data/pincode";

    private volatile BinaryReader reader;

    @Override
    public Optional<PincodeInfo> lookup(String key) {
        if (key == null) return Optional.empty();
        return reader().lookup(key, PincodeLookupImpl::decode);
    }

    private BinaryReader reader() {
        BinaryReader r = reader;
        if (r != null) return r;
        synchronized (this) {
            if (reader == null) {
                reader = ResourceBootstrap.load(BASE, PincodeLookupImpl::encode);
            }
            return reader;
        }
    }

    static PincodeInfo decode(ByteBuffer in, String key, String[] dict) {
        int city = in.getShort() & 0xFFFF;
        int district = in.getShort() & 0xFFFF;
        int state = in.getShort() & 0xFFFF;
        int region = in.getShort() & 0xFFFF;
        return new PincodeInfo(key, dict[city], dict[district], dict[state], dict[region]);
    }

    static void encode(BinaryWriter w, CsvParser.Table table) {
        List<String> header = table.header();
        int iPincode = required(header, "pincode");
        int iCity = required(header, "city");
        int iDistrict = required(header, "district");
        int iState = required(header, "state");
        int iRegion = required(header, "region");

        for (List<String> row : table.rows()) {
            int city = w.intern(row.get(iCity));
            int district = w.intern(row.get(iDistrict));
            int state = w.intern(row.get(iState));
            int region = w.intern(row.get(iRegion));
            ByteBuffer rec = ByteBuffer.allocate(8);
            rec.putShort((short) city);
            rec.putShort((short) district);
            rec.putShort((short) state);
            rec.putShort((short) region);
            w.addRecord(row.get(iPincode), rec.array());
        }
    }

    private static int required(List<String> header, String name) {
        int i = header.indexOf(name);
        if (i < 0) throw new IllegalArgumentException("Missing CSV column: " + name);
        return i;
    }
}
