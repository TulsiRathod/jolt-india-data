package io.github.tulsirathod.joltindiadata.internal;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CsvParserTest {

    @Test
    void parsesSimpleHeaderAndRows() throws IOException {
        String csv = "a,b,c\n1,2,3\n4,5,6\n";
        CsvParser.Table t = parse(csv);
        assertEquals(List.of("a", "b", "c"), t.header());
        assertEquals(2, t.rows().size());
        assertEquals(List.of("1", "2", "3"), t.rows().get(0));
    }

    @Test
    void skipsCommentAndBlankLines() throws IOException {
        String csv = "# this is a comment\n\na,b\n# another\n1,2\n";
        CsvParser.Table t = parse(csv);
        assertEquals(List.of("a", "b"), t.header());
        assertEquals(1, t.rows().size());
    }

    @Test
    void handlesQuotedFieldsWithEmbeddedCommas() throws IOException {
        String csv = "name,address\n" +
                "Bank,\"Main St, Mumbai\"\n";
        CsvParser.Table t = parse(csv);
        assertEquals(List.of("Bank", "Main St, Mumbai"), t.rows().get(0));
    }

    @Test
    void handlesDoubleQuoteEscape() throws IOException {
        String csv = "x\n\"a \"\"b\"\" c\"\n";
        CsvParser.Table t = parse(csv);
        assertEquals("a \"b\" c", t.rows().get(0).get(0));
    }

    private CsvParser.Table parse(String s) throws IOException {
        return CsvParser.parse(new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8)));
    }
}
