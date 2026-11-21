package io.github.alextoday111.projectd.interpreter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class InterpreterIntegrationTest {
    @TempDir
    Path temporary;

    @ParameterizedTest
    @CsvSource({
            "01_simple.d, 12.5",
            "01_calculator.d, 25|15|100|4",
            "02_nested_collections.d, 30|1|3|60|2.5|beta|9",
            "03_nested_values.d, 7",
            "03_bubble_sort.d, 1 2 4 5 8",
            "04_higher_order.d, 25|7|25 16 34|11"
    })
    void executesExistingExamplesInBothCliModes(String file, String output) {
        String source = Path.of("examples", "valid", file).toString();
        String expected = output.replace('|', '\n') + "\n";
        assertRun(0, expected, "", source);
        assertRun(0, expected, "", "--optimize", source);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "print @ | 4 | unexpected character",
            "print | 5 | Syntax error",
            "print missing | 6 | not declared",
            "print 1 / 0 | 7 | Runtime error: Division by zero"
    })
    void reportsTheFailingPipelineStage(String source, int code, String diagnostic) throws IOException {
        Path file = Files.writeString(temporary.resolve("input.d"), source, StandardCharsets.UTF_8);
        assertRun(code, "", diagnostic, "--optimize", file.toString());
    }

    @Test
    void reportsUsageFileFailuresAndLegacySyntaxErrors() {
        assertRun(2, "", "Usage: InterpreterMain");
        assertRun(3, "", "Cannot read", temporary.resolve("missing.d").toString());
        assertRun(5, "", "Syntax error at 13:32", "examples/valid/02_calculator.d");
    }

    private static void assertRun(int code, String output, String diagnostic, String... args) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        assertEquals(code, InterpreterMain.run(args, new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8)), err.toString(StandardCharsets.UTF_8));
        assertEquals(output, out.toString(StandardCharsets.UTF_8).replace("\r\n", "\n"));
        String error = err.toString(StandardCharsets.UTF_8);
        if (diagnostic.isEmpty()) {
            assertEquals("", error);
        } else {
            assertTrue(error.contains(diagnostic), error);
            assertFalse(error.contains("\tat "), error);
        }
    }
}
