package io.github.tulsirathod.joltindiadata.internal;

import io.github.tulsirathod.joltindiadata.hsn.HSNLookup;
import io.github.tulsirathod.joltindiadata.hsn.HSNRate;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/**
 * Default HSN-rate provider.
 *
 * <p>Record layout (per rate; multiple records may share an HSN key):
 * <pre>
 *   float64 rate                      8 bytes
 *   int32   effectiveFromEpochDay     4 bytes
 *   uint16  descriptionLen            free-form, UTF-8
 *   bytes   description
 * </pre>
 *
 * <p>CSV columns: {@code hsn, rate, description, effective_from}.
 * Dates are ISO-8601 (yyyy-MM-dd).
 */
public final class HSNLookupImpl implements HSNLookup {

    static final String BASE = "/jolt-india-data/hsn";

    private volatile BinaryReader reader;

    @Override
    public List<HSNRate> lookup(String key) {
        if (key == null) return List.of();
        return reader().lookupAll(key, HSNLookupImpl::decode);
    }

    private BinaryReader reader() {
        BinaryReader r = reader;
        if (r != null) return r;
        synchronized (this) {
            if (reader == null) {
                reader = ResourceBootstrap.load(BASE, HSNLookupImpl::encode);
            }
            return reader;
        }
    }

    static HSNRate decode(ByteBuffer in, String key, String[] dict) {
        double rate = in.getDouble();
        int epochDay = in.getInt();
        int len = in.getShort() & 0xFFFF;
        byte[] desc = new byte[len];
        in.get(desc);
        return new HSNRate(
                key,
                rate,
                new String(desc, StandardCharsets.UTF_8),
                LocalDate.ofEpochDay(epochDay));
    }

    static void encode(BinaryWriter w, CsvParser.Table table) {
        List<String> header = table.header();
        int iHsn = required(header, "hsn");
        int iRate = required(header, "rate");
        int iDesc = required(header, "description");
        int iFrom = required(header, "effective_from");

        for (List<String> row : table.rows()) {
            double rate = Double.parseDouble(row.get(iRate));
            int epochDay = (int) LocalDate.parse(row.get(iFrom)).toEpochDay();
            byte[] desc = row.get(iDesc).getBytes(StandardCharsets.UTF_8);
            ByteBuffer rec = ByteBuffer.allocate(8 + 4 + 2 + desc.length);
            rec.putDouble(rate);
            rec.putInt(epochDay);
            rec.putShort((short) desc.length);
            rec.put(desc);
            w.addRecord(row.get(iHsn), rec.array());
        }
    }

    private static int required(List<String> header, String name) {
        int i = header.indexOf(name);
        if (i < 0) throw new IllegalArgumentException("Missing CSV column: " + name);
        return i;
    }
}
