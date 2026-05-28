package io.github.tulsirathod.joltindiadata.internal;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TwoTierDataLoaderTest {

    @Test
    void unknownResourceThrows() {
        TwoTierDataLoader loader = new TwoTierDataLoader("test");
        assertThrows(IOException.class, () -> loader.open("/does/not/exist.gz"));
    }

    @Test
    void loadsGzippedResourceFromClasspath() throws IOException {
        // Verify the public open(...) contract handles a resource that
        // *is* on the classpath. We synthesise one by writing a temp
        // resource into target/test-classes at test time.
        BinaryWriter w = new BinaryWriter();
        w.addRecord("K", new byte[]{1, 2, 3});
        byte[] gz = w.buildGzipped();

        java.nio.file.Path testClasses = java.nio.file.Paths.get("target", "test-classes");
        java.nio.file.Files.createDirectories(testClasses);
        java.nio.file.Path out = testClasses.resolve("loader-roundtrip.jid.gz");
        java.nio.file.Files.write(out, gz);

        TwoTierDataLoader loader = new TwoTierDataLoader("v0");
        ByteBuffer buf = loader.open("/loader-roundtrip.jid.gz");
        BinaryReader r = new BinaryReader(buf);
        byte[] read = r.lookup("K", (in, k, d) -> {
            byte[] b = new byte[in.remaining()];
            in.get(b);
            return b;
        }).orElseThrow();
        assertEquals(3, read.length);
        assertEquals(1, read[0]);
    }
}
