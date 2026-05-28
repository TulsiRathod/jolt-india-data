package io.github.tulsirathod.joltindiadata.builder;

import io.github.tulsirathod.joltindiadata.internal.BinaryWriter;
import io.github.tulsirathod.joltindiadata.internal.CsvParser;
import io.github.tulsirathod.joltindiadata.internal.HSNLookupImpl;
import io.github.tulsirathod.joltindiadata.internal.IFSCLookupImpl;
import io.github.tulsirathod.joltindiadata.internal.PincodeLookupImpl;
import io.github.tulsirathod.joltindiadata.internal.RTOLookupImpl;
import io.github.tulsirathod.joltindiadata.internal.ResourceBootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Offline build tool. Converts the raw CSV inputs in {@code data/raw/} into
 * gzipped {@code .jid} files under {@code src/main/resources/jolt-india-data/}
 * so they ship with the published jar.
 *
 * <p>Invoked from the {@link #main} entry point — typically by the
 * {@code refresh-data.yml} GitHub Actions workflow, but also runnable by
 * maintainers locally:
 *
 * <pre>
 *   mvn compile exec:java -Dexec.mainClass=io.github.tulsirathod.joltindiadata.builder.Builder
 * </pre>
 *
 * <p>The CSV column conventions are the same as those accepted by each
 * {@code *LookupImpl}; see their javadoc.
 *
 * <p>This class is not part of the supported runtime API and lives in the
 * jar only as a convenience for downstream consumers who want to bundle
 * their own datasets. It depends on no external libraries beyond the JDK.
 */
public final class Builder {

    private static final Path RAW_DIR = Paths.get("data", "raw");
    private static final Path OUT_DIR = Paths.get("src", "main", "resources", "jolt-india-data");

    /** Maps raw-CSV filename -> output base name + encoder. */
    private static final Map<String, Dataset> DATASETS = new LinkedHashMap<>();
    static {
        DATASETS.put("ifsc.csv", new Dataset("ifsc", IFSCLookupImpl::encode));
        DATASETS.put("pincode.csv", new Dataset("pincode", PincodeLookupImpl::encode));
        DATASETS.put("hsn.csv", new Dataset("hsn", HSNLookupImpl::encode));
        DATASETS.put("rto.csv", new Dataset("rto", RTOLookupImpl::encode));
    }

    private record Dataset(String outName, ResourceBootstrap.CsvToBinary encoder) {}

    public static void main(String[] args) throws IOException {
        List<String> only = List.of(args);
        Files.createDirectories(OUT_DIR);

        int built = 0;
        for (Map.Entry<String, Dataset> e : DATASETS.entrySet()) {
            String csvName = e.getKey();
            Dataset ds = e.getValue();
            if (!only.isEmpty() && !only.contains(ds.outName())) continue;

            Path raw = RAW_DIR.resolve(csvName);
            if (!Files.exists(raw)) {
                System.out.printf("skip  %s (no %s)%n", ds.outName(), raw);
                continue;
            }

            CsvParser.Table table;
            try (InputStream in = Files.newInputStream(raw)) {
                table = CsvParser.parse(in);
            }

            BinaryWriter w = new BinaryWriter();
            ds.encoder().encode(w, table);
            byte[] gz = w.buildGzipped();

            Path out = OUT_DIR.resolve(ds.outName() + ".jid.gz");
            Files.write(out, gz);
            System.out.printf("build %s -> %s (%d rows, %d bytes gzipped)%n",
                    csvName, out, table.rows().size(), gz.length);
            built++;
        }

        if (built == 0) {
            System.out.println("Nothing built. Place CSVs in " + RAW_DIR + "/ to refresh.");
        }
    }
}
