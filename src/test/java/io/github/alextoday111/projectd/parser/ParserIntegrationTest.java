package io.github.alextoday111.projectd.parser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import io.github.alextoday111.projectd.lexer.Lexer;
import io.github.alextoday111.projectd.lexer.TokenType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

final class ParserIntegrationTest {
    @TempDir
    Path temporaryDirectory;

    @ParameterizedTest(name = "Parser-valid: {0}")
    @ValueSource(strings = {"01_calculator.d", "02_nested_collections.d", "03_bubble_sort.d",
            "04_higher_order.d", "01_simple.d", "03_nested_values.d"})
    void parsesConfirmedExamples(String fileName) throws IOException {
        String source = Files.readString(Path.of("examples", "valid", fileName), StandardCharsets.UTF_8);
        assertDoesNotThrow(() -> new Parser(new Lexer(source).scanTokens()).parse());
    }

    @Test
    void keepsOriginalCalculatorLexerOnlyWithoutExpandingGrammar() throws IOException {
        String source = Files.readString(Path.of("examples", "valid", "02_calculator.d"), StandardCharsets.UTF_8);
        ParseException error = assertThrows(ParseException.class,
                () -> new Parser(new Lexer(source).scanTokens()).parse());
        assertEquals(TokenType.NEWLINE, error.getActualToken().getType());
        assertEquals(13, error.getLine());
        assertEquals(32, error.getColumn());
        assertTrue(error.getExpected().contains("expression"));
    }

    @ParameterizedTest
    @CsvSource({"01_calculator.d, 0", "02_calculator.d, 1"})
    void cliReturnsCorrectProcessExitCodes(String fileName, int expectedCode) throws Exception {
        Path java = Path.of(System.getProperty("java.home"), "bin",
                System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java");
        Path classes = Path.of(ParserMain.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Process process = new ProcessBuilder(java.toString(), "-cp", classes.toString(),
                ParserMain.class.getName(), Path.of("examples", "valid", fileName).toString())
                .redirectErrorStream(true).start();
        try {
            assertTrue(process.waitFor(10, TimeUnit.SECONDS), "Parser CLI did not terminate");
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(expectedCode, process.exitValue(), output);
            if (expectedCode == 0) {
                assertEquals("Parsing successful. Syntax is valid.", output.trim());
            } else {
                assertTrue(output.contains("Syntax error at 13:32") && output.contains("found NEWLINE"), output);
                assertFalse(output.contains("Parsing successful"));
            }
        } finally {
            process.destroyForcibly();
        }
    }

    @Test
    void cliReportsSuccessOnlyOnStandardOutput() throws IOException {
        Path file = writeSource("good.d", "var x := 1; print x");
        assertCli(new String[]{file.toString()}, 0, "Parsing successful. Syntax is valid.", "");
    }

    @Test
    void cliReportsSyntaxFailureOnlyOnStandardError() throws IOException {
        Path file = writeSource("bad.d", "var x :=\nend");
        assertCli(new String[]{file.toString()}, 1, "", "Syntax error at 1:9");
    }

    @Test
    void cliPreservesLexicalErrors() throws IOException {
        Path file = writeSource("lexical.d", "var x := @");
        assertCli(new String[]{file.toString()}, 1, "", "Lexical error at 1:10");
    }

    @Test
    void cliRejectsMissingArguments() throws IOException {
        assertCli(new String[0], 2, "", "Usage: ParserMain <file.d>");
    }

    @Test
    void cliRejectsMultipleArguments() throws IOException {
        assertCli(new String[]{"one.d", "two.d"}, 2, "", "Usage: ParserMain <file.d>");
    }

    @Test
    void cliReportsUnreadableFiles() throws IOException {
        Path missing = temporaryDirectory.resolve("missing.d");
        assertCli(new String[]{missing.toString()}, 3, "", "Cannot read " + missing);
    }

    @Test
    void cliReportsInvalidPathsWithoutLeakingRuntimeExceptions() throws IOException {
        assertCli(new String[]{"bad\u0000path.d"}, 3, "", "Cannot read");
    }

    private Path writeSource(String name, String source) throws IOException {
        return Files.writeString(temporaryDirectory.resolve(name), source, StandardCharsets.UTF_8);
    }

    private static void assertCli(String[] args, int code, String expectedOut, String expectedErr) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        try (PrintStream out = new PrintStream(output, true, "UTF-8");
             PrintStream err = new PrintStream(errors, true, "UTF-8")) {
            assertEquals(code, ParserMain.run(args, out, err));
        }
        String actualOut = output.toString("UTF-8").trim();
        String actualErr = errors.toString("UTF-8").trim();
        assertEquals(expectedOut, actualOut);
        if (expectedErr.isEmpty()) {
            assertEquals("", actualErr);
        } else {
            assertTrue(actualErr.contains(expectedErr), actualErr);
        }
    }
}
