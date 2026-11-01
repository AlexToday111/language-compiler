package io.github.alextoday111.projectd.semantic;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

final class SemanticMainTest {
    @TempDir Path directory;

    private String[] invoke(int expectedCode, String... args) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream(), errors = new ByteArrayOutputStream();
        try (PrintStream out = new PrintStream(output, true, "UTF-8");
             PrintStream err = new PrintStream(errors, true, "UTF-8")) {
            assertEquals(expectedCode, SemanticMain.run(args, out, err), errors.toString("UTF-8"));
        }
        return new String[]{output.toString("UTF-8"), errors.toString("UTF-8")};
    }

    @Test
    void preservesOutputModesAndShowsBothTreesOnlyAfterValidation() throws Exception {
        Path source = Files.writeString(directory.resolve("valid.d"), "var x := 5 + 3 * 2; print x");
        String[] result = invoke(0, source.toString());
        assertEquals("Semantic analysis successful.", result[0].trim());
        assertEquals("", result[1]);
        result = invoke(0, "--ast", source.toString());
        assertTrue(result[0].startsWith("Program\n"));
        assertTrue(result[0].contains("BinaryExpression: +"));
        result = invoke(0, "--optimize", source.toString());
        assertTrue(result[0].contains("IntegerLiteral: 11"));
        assertFalse(result[0].contains("BinaryExpression"));
        result = invoke(0, "--compare", source.toString());
        assertTrue(result[0].startsWith("Original AST:\nProgram\n"));
        assertTrue(result[0].contains("Optimized AST:\nProgram\n"));
        assertEquals("", result[1]);
    }

    @ParameterizedTest
    @ValueSource(strings = {"--ast", "--optimize", "--compare"})
    void reportsSemanticErrorsBeforeProducingAnyAstOrOptimizing(String mode) throws Exception {
        Path source = Files.writeString(directory.resolve("invalid.d"), "if false => print missing");
        String[] result = invoke(1, mode, source.toString());
        assertEquals("", result[0]);
        assertTrue(result[1].contains("UNDECLARED_VARIABLE"));
        assertTrue(result[1].contains("missing"));
    }

    @Test
    void handlesLexicalSyntaxUsageAndFileErrors() throws Exception {
        Path source = Files.writeString(directory.resolve("bad.d"), "print @");
        assertTrue(invoke(1, source.toString())[1].contains("Lexical error"));
        Files.writeString(source, "var x :=");
        assertTrue(invoke(1, "--compare", source.toString())[1].contains("Syntax error"));
        assertTrue(invoke(2)[1].contains("Usage:"));
        invoke(2, "--unknown", source.toString());
        invoke(2, "--compare");
        invoke(2, "--ast", "--optimize", source.toString());
        assertTrue(invoke(3, directory.resolve("missing.d").toString())[1].contains("Cannot read"));
        assertTrue(invoke(3, "bad\u0000path")[1].contains("Cannot read"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"valid", "optimize", "undeclared", "duplicate", "invalid-return", "invalid-exit"})
    void laboratoryFixturesHaveExpectedExitCodes(String name) throws Exception {
        boolean valid = name.equals("valid") || name.equals("optimize");
        String[] result = invoke(valid ? 0 : 1, "--compare", "examples/semantic/" + name + ".d");
        if (valid) assertEquals("", result[1]);
        else assertEquals("", result[0]);
    }

    @Test
    void laboratoryBeforeAfterSnapshotMatchesCliOutput() throws Exception {
        String[] result = invoke(0, "--compare", "examples/semantic/optimize.d");
        assertEquals(Files.readString(Path.of("examples/semantic/optimize.ast.txt")), result[0]);
    }

    @ParameterizedTest
    @ValueSource(strings = {"var x := 1; print x", "return", "exit", "print missing"})
    void realProcessReturnsExpectedExitCode(String source) throws Exception {
        Path file = Files.writeString(directory.resolve("input.d"), source);
        Path stdout = directory.resolve("stdout.txt"), stderr = directory.resolve("stderr.txt");
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        Process process = new ProcessBuilder(java, "-cp", "target/classes", SemanticMain.class.getName(),
                "--compare", file.toString()).redirectOutput(stdout.toFile()).redirectError(stderr.toFile()).start();
        try {
            assertTrue(process.waitFor(10, TimeUnit.SECONDS));
            boolean valid = source.startsWith("var");
            assertEquals(valid ? 0 : 1, process.exitValue(), Files.readString(stderr));
            if (valid) assertTrue(Files.readString(stdout, StandardCharsets.UTF_8).contains("Optimized AST:"));
            else assertEquals("", Files.readString(stdout));
        } finally { process.destroyForcibly(); }
    }
}
