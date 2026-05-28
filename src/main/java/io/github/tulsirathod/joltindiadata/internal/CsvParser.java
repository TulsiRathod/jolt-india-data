package io.github.tulsirathod.joltindiadata.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal CSV reader, sufficient for the structured datasets shipped with
 * this library. Handles quoted fields, embedded commas, and {@code ""}
 * escaping. Does not handle multi-line records (none of our datasets need
 * them).
 *
 * <p>The first non-blank, non-comment line is the header. Lines starting
 * with {@code #} are skipped. Trailing blank lines are tolerated.
 */
public final class CsvParser {

    private CsvParser() {}

    public record Table(List<String> header, List<List<String>> rows) {}

    public static Table parse(InputStream in) throws IOException {
        List<String> header = null;
        List<List<String>> rows = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) continue;
                List<String> fields = parseLine(line);
                if (header == null) header = fields;
                else rows.add(fields);
            }
        }
        if (header == null) throw new IOException("Empty CSV (no header line)");
        return new Table(header, rows);
    }

    private static List<String> parseLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    field.append(c);
                }
            } else {
                if (c == ',') {
                    out.add(field.toString());
                    field.setLength(0);
                } else if (c == '"' && field.length() == 0) {
                    inQuotes = true;
                } else {
                    field.append(c);
                }
            }
        }
        out.add(field.toString());
        return out;
    }
}
