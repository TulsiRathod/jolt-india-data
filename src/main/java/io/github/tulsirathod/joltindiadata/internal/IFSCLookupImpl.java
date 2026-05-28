package io.github.tulsirathod.joltindiadata.internal;

import io.github.tulsirathod.joltindiadata.ifsc.IFSCInfo;
import io.github.tulsirathod.joltindiadata.ifsc.IFSCLookup;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * Default IFSC provider, discovered by {@link java.util.ServiceLoader}.
 *
 * <p>Record layout in the {@code .jid} payload (big-endian):
 * <pre>
 *   uint16 bankIdx          dictionary index
 *   uint16 branchLen        free-form, UTF-8
 *   bytes  branch
 *   uint16 cityIdx          dictionary index
 *   uint16 stateIdx         dictionary index
 *   uint16 addressLen       free-form, UTF-8
 *   bytes  address
 *   uint8  flags            bit 0 = RTGS, bit 1 = NEFT, bit 2 = IMPS
 * </pre>
 *
 * <p>CSV column order (for the bootstrap path): {@code ifsc, bank, branch,
 * city, state, address, rtgs, neft, imps}. Boolean columns accept
 * {@code true/false/1/0/Y/N} case-insensitively.
 */
public final class IFSCLookupImpl implements IFSCLookup {

    static final String BASE = "/jolt-india-data/ifsc";

    private volatile BinaryReader reader;

    @Override
    public Optional<IFSCInfo> lookup(String key) {
        if (key == null) return Optional.empty();
        return reader().lookup(key, IFSCLookupImpl::decode);
    }

    private BinaryReader reader() {
        BinaryReader r = reader;
        if (r != null) return r;
        synchronized (this) {
            if (reader == null) {
                reader = ResourceBootstrap.load(BASE, IFSCLookupImpl::encode);
            }
            return reader;
        }
    }

    static IFSCInfo decode(ByteBuffer in, String key, String[] dict) {
        int bankIdx = in.getShort() & 0xFFFF;
        String branch = readInlineString(in);
        int cityIdx = in.getShort() & 0xFFFF;
        int stateIdx = in.getShort() & 0xFFFF;
        String address = readInlineString(in);
        int flags = in.get() & 0xFF;
        return new IFSCInfo(
                key,
                dict[bankIdx],
                branch,
                dict[cityIdx],
                dict[stateIdx],
                address,
                (flags & 0x1) != 0,
                (flags & 0x2) != 0,
                (flags & 0x4) != 0);
    }

    static void encode(BinaryWriter w, CsvParser.Table table) {
        List<String> header = table.header();
        int iIfsc = required(header, "ifsc");
        int iBank = required(header, "bank");
        int iBranch = required(header, "branch");
        int iCity = required(header, "city");
        int iState = required(header, "state");
        int iAddress = required(header, "address");
        int iRtgs = required(header, "rtgs");
        int iNeft = required(header, "neft");
        int iImps = required(header, "imps");

        for (List<String> row : table.rows()) {
            int bankIdx = w.intern(row.get(iBank));
            int cityIdx = w.intern(row.get(iCity));
            int stateIdx = w.intern(row.get(iState));

            byte[] branch = row.get(iBranch).getBytes(StandardCharsets.UTF_8);
            byte[] address = row.get(iAddress).getBytes(StandardCharsets.UTF_8);
            int flags = (parseBool(row.get(iRtgs)) ? 0x1 : 0)
                    | (parseBool(row.get(iNeft)) ? 0x2 : 0)
                    | (parseBool(row.get(iImps)) ? 0x4 : 0);

            ByteBuffer rec = ByteBuffer.allocate(2 + 2 + branch.length + 2 + 2 + 2 + address.length + 1);
            rec.putShort((short) bankIdx);
            rec.putShort((short) branch.length);
            rec.put(branch);
            rec.putShort((short) cityIdx);
            rec.putShort((short) stateIdx);
            rec.putShort((short) address.length);
            rec.put(address);
            rec.put((byte) flags);
            w.addRecord(row.get(iIfsc), rec.array());
        }
    }

    private static int required(List<String> header, String name) {
        int i = header.indexOf(name);
        if (i < 0) throw new IllegalArgumentException("Missing CSV column: " + name);
        return i;
    }

    private static boolean parseBool(String s) {
        String t = s.trim();
        return t.equalsIgnoreCase("true") || t.equals("1")
                || t.equalsIgnoreCase("y") || t.equalsIgnoreCase("yes");
    }

    private static String readInlineString(ByteBuffer in) {
        int len = in.getShort() & 0xFFFF;
        byte[] b = new byte[len];
        in.get(b);
        return new String(b, StandardCharsets.UTF_8);
    }
}
