package io.github.tulsirathod.joltindiadata.internal;

import io.github.tulsirathod.joltindiadata.spi.BinaryFormat;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BinaryFormatRoundTripTest {

    @Test
    void roundTripSingleRecord() {
        BinaryWriter w = new BinaryWriter();
        int bankIdx = w.intern("HDFC BANK");
        ByteBuffer rec = ByteBuffer.allocate(2);
        rec.putShort((short) bankIdx);
        w.addRecord("HDFC0000001", rec.array());

        BinaryReader r = new BinaryReader(ByteBuffer.wrap(w.build()));
        assertEquals(1, r.recordCount());

        Optional<String> looked = r.lookup("HDFC0000001",
                (in, key, dict) -> dict[in.getShort() & 0xFFFF]);
        assertTrue(looked.isPresent());
        assertEquals("HDFC BANK", looked.get());
    }

    @Test
    void sortsByKeyEvenIfInsertedOutOfOrder() {
        BinaryWriter w = new BinaryWriter();
        w.addRecord("C", new byte[]{3});
        w.addRecord("A", new byte[]{1});
        w.addRecord("B", new byte[]{2});

        BinaryReader r = new BinaryReader(ByteBuffer.wrap(w.build()));
        assertEquals(1, r.lookup("A", (in, k, d) -> in.get() & 0xFF).orElseThrow());
        assertEquals(2, r.lookup("B", (in, k, d) -> in.get() & 0xFF).orElseThrow());
        assertEquals(3, r.lookup("C", (in, k, d) -> in.get() & 0xFF).orElseThrow());
    }

    @Test
    void unknownKeyReturnsEmpty() {
        BinaryWriter w = new BinaryWriter();
        w.addRecord("X", new byte[]{1});
        BinaryReader r = new BinaryReader(ByteBuffer.wrap(w.build()));
        assertFalse(r.lookup("Y", (in, k, d) -> in.get() & 0xFF).isPresent());
    }

    @Test
    void multiRecordSameKey() {
        BinaryWriter w = new BinaryWriter();
        ByteBuffer r1 = ByteBuffer.allocate(4);
        r1.putFloat(5.0f);
        w.addRecord("6109", r1.array());
        ByteBuffer r2 = ByteBuffer.allocate(4);
        r2.putFloat(12.0f);
        w.addRecord("6109", r2.array());

        BinaryReader r = new BinaryReader(ByteBuffer.wrap(w.build()));
        List<Float> all = r.lookupAll("6109", (in, k, d) -> in.getFloat());
        assertEquals(2, all.size());
        assertTrue(all.contains(5.0f));
        assertTrue(all.contains(12.0f));
    }

    @Test
    void stringInterningDeduplicates() {
        BinaryWriter w = new BinaryWriter();
        int a = w.intern("Maharashtra");
        int b = w.intern("Maharashtra");
        int c = w.intern("Gujarat");
        assertEquals(a, b);
        assertEquals(0, a);
        assertEquals(1, c);
    }

    @Test
    void gzipRoundTrip() throws Exception {
        BinaryWriter w = new BinaryWriter();
        w.addRecord("K", "hello world".getBytes(StandardCharsets.UTF_8));
        byte[] gz = w.buildGzipped();
        assertTrue(gz.length > 0);
        ByteBuffer raw = ResourceBootstrap.ungzip(gz);
        BinaryReader r = new BinaryReader(raw);
        byte[] read = r.lookup("K", (in, k, d) -> {
            byte[] buf = new byte[in.remaining()];
            in.get(buf);
            return buf;
        }).orElseThrow();
        assertArrayEquals("hello world".getBytes(StandardCharsets.UTF_8), read);
    }

    @Test
    void rejectsBadMagic() {
        ByteBuffer bad = ByteBuffer.allocate(BinaryFormat.HEADER_BYTES);
        bad.putInt(0xDEADBEEF);
        bad.putShort(BinaryFormat.FORMAT_VERSION);
        bad.putShort((short) 0);
        bad.putInt(0);
        bad.putInt(0);
        bad.position(0);
        assertThrows(IllegalArgumentException.class, () -> new BinaryReader(bad));
    }
}
