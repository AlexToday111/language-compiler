package io.github.alextoday111.projectd.semantic;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import io.github.alextoday111.projectd.ast.*;
import io.github.alextoday111.projectd.lexer.Lexer;
import io.github.alextoday111.projectd.optimization.AstOptimizer;
import io.github.alextoday111.projectd.parser.Parser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import static io.github.alextoday111.projectd.semantic.SemanticDiagnostic.Code.*;
import static org.junit.jupiter.api.Assertions.*;

final class SemanticAnalyzerTest {
    private static Program parse(String source) { return new Parser(new Lexer(source).scanTokens()).parse(); }

    @ParameterizedTest
    @ValueSource(strings = {
            "", "var x; print x is none", "var x := x", "var a := 1, b := a; a := b",
            "var x := 1; if true then var x := 2; print x else var x := 3; print x end; print x",
            "if true => var x; var x", "while true loop var x; exit end; var x",
            "var i := 3; for i in i..i loop print i end; print i",
            "for i in 1..3 loop for i in 1..2 loop print i; exit end; print i end",
            "for [1] loop exit end; for 1..3 loop exit end; loop exit end",
            "var f := func(x) is if true then return x else while true loop return x end end end",
            "var f := func(x) is var g := func(y) => x + y; return g(x) end",
            "var x := 1; var f := func(x) => func(y) => x + y",
            "var f := func(n) is if n > 0 => return f(n - 1); return n end",
            "var f := func is return end", "var f := func => 1",
            "var f := func(x) is loop var g := func(y) is while true loop exit end; return y end; exit end; return x end",
            "var a := [1]; var t := {field := a}; t.field[1] := a[1]; print t.field, t.1",
            "var f := func(x) => x; var t := {notAVariable := f([1])}; print t.notAVariable",
            "var f := func(x) => x; f(1) := 2",
            "var f := func(x) is if true then var x; print x end; return x end"
    })
    void acceptsValidProgramsWithoutMutatingAst(String source) {
        Program program = parse(source);
        String before = new AstPrinter().print(program);
        new SemanticAnalyzer().analyze(program);
        assertEquals(before, new AstPrinter().print(program));
        new SemanticAnalyzer().analyze(new AstOptimizer().optimize(program));
    }

    static Stream<Arguments> invalidPrograms() {
        return Stream.of(
                Arguments.of("print x; var x", UNDECLARED_VARIABLE),
                Arguments.of("x := 1", UNDECLARED_VARIABLE),
                Arguments.of("var a := b, b := 1", UNDECLARED_VARIABLE),
                Arguments.of("var a, a", DUPLICATE_DECLARATION),
                Arguments.of("var a; var a", DUPLICATE_DECLARATION),
                Arguments.of("if true then var a end; print a", UNDECLARED_VARIABLE),
                Arguments.of("if true then var a else print a end", UNDECLARED_VARIABLE),
                Arguments.of("if true => var a; print a", UNDECLARED_VARIABLE),
                Arguments.of("while true loop var a; exit end; print a", UNDECLARED_VARIABLE),
                Arguments.of("for i in 1..2 loop print i end; print i", UNDECLARED_VARIABLE),
                Arguments.of("for i in i..3 loop exit end", UNDECLARED_VARIABLE),
                Arguments.of("for i in 1..3 loop var i end", DUPLICATE_DECLARATION),
                Arguments.of("var f := func(x, x) => x", DUPLICATE_DECLARATION),
                Arguments.of("var f := func(x) is var x; return x end", DUPLICATE_DECLARATION),
                Arguments.of("var f := func(x) => missing + x", UNDECLARED_VARIABLE),
                Arguments.of("var f := func => later; var later := 1", UNDECLARED_VARIABLE),
                Arguments.of("var f := func(x) => x; print x", UNDECLARED_VARIABLE),
                Arguments.of("var f := func(x) => g(x); var g := func(x) => x", UNDECLARED_VARIABLE),
                Arguments.of("var t := {field := field}", UNDECLARED_VARIABLE),
                Arguments.of("var a := []; a[index] := 1", UNDECLARED_VARIABLE),
                Arguments.of("var a := []; a[1] := missing", UNDECLARED_VARIABLE),
                Arguments.of("var f := func(x) => x; print f(missing)", UNDECLARED_VARIABLE),
                Arguments.of("print missing.field", UNDECLARED_VARIABLE),
                Arguments.of("print missing is int", UNDECLARED_VARIABLE),
                Arguments.of("return", RETURN_OUTSIDE_FUNCTION),
                Arguments.of("if true => return 1", RETURN_OUTSIDE_FUNCTION),
                Arguments.of("while true loop return end", RETURN_OUTSIDE_FUNCTION),
                Arguments.of("exit", EXIT_OUTSIDE_LOOP),
                Arguments.of("if true => exit", EXIT_OUTSIDE_LOOP),
                Arguments.of("var f := func is exit end", EXIT_OUTSIDE_LOOP),
                Arguments.of("loop var f := func is exit end; exit end", EXIT_OUTSIDE_LOOP),
                Arguments.of("loop var f := func is loop exit end; exit end; exit end", EXIT_OUTSIDE_LOOP),
                Arguments.of("var f := func is return; print missing end", UNDECLARED_VARIABLE),
                Arguments.of("if false => print missing", UNDECLARED_VARIABLE)
        );
    }

    @ParameterizedTest
    @MethodSource("invalidPrograms")
    void reportsStructuredDiagnosticsEvenForUnreachableCode(String source, SemanticDiagnostic.Code code) {
        Program program = parse(source);
        String before = new AstPrinter().print(program);
        SemanticException error = assertThrows(SemanticException.class, () -> new SemanticAnalyzer().analyze(program));
        assertEquals(code, error.getDiagnostic().getCode());
        assertNotNull(error.getDiagnostic().getNode());
        assertTrue(error.getDiagnostic().getPath().startsWith("Program."));
        assertTrue(error.getMessage().contains(code.name()));
        assertEquals(before, new AstPrinter().print(program));
    }

    @ParameterizedTest
    @ValueSource(strings = {"01_simple.d", "03_nested_values.d", "01_calculator.d",
            "02_nested_collections.d", "03_bubble_sort.d", "04_higher_order.d"})
    void validatesExistingParserValidExamplesAndTheirOptimizations(String file) throws Exception {
        Program program = parse(Files.readString(Path.of("examples", "valid", file)));
        SemanticAnalyzer analyzer = new SemanticAnalyzer();
        analyzer.analyze(program);
        analyzer.analyze(new AstOptimizer().optimize(program));
    }

    @Test
    void diagnosticsIdentifyTheActualReferenceAndAnalyzerCanBeReusedAfterFailure() {
        SemanticAnalyzer analyzer = new SemanticAnalyzer();
        Program program = parse("var x; print unknown");
        Identifier reference = (Identifier) ((PrintStatement) program.getStatements().get(1)).getExpressions().get(0);
        SemanticException error = assertThrows(SemanticException.class, () -> analyzer.analyze(program));
        assertSame(reference, error.getDiagnostic().getNode());
        analyzer.analyze(parse("var x"));
        assertThrows(SemanticException.class, () -> analyzer.analyze(parse("print x")));
    }

    @Test
    void symbolTableUsesLexicalLookupAndRejectsOnlyLocalDuplicates() {
        SymbolTable symbols = new SymbolTable();
        assertTrue(symbols.declare("outer"));
        assertFalse(symbols.declare("outer"));
        symbols.enterScope();
        assertTrue(symbols.isDeclared("outer"));
        assertTrue(symbols.declare("outer"));
        assertTrue(symbols.declare("inner"));
        symbols.leaveScope();
        assertFalse(symbols.isDeclared("inner"));
        assertTrue(symbols.isDeclared("outer"));
        assertThrows(IllegalStateException.class, symbols::leaveScope);
    }
}
