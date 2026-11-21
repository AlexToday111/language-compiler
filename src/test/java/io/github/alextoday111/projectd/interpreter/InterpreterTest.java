package io.github.alextoday111.projectd.interpreter;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import io.github.alextoday111.projectd.ast.*;
import io.github.alextoday111.projectd.lexer.Lexer;
import io.github.alextoday111.projectd.optimization.AstOptimizer;
import io.github.alextoday111.projectd.parser.Parser;
import io.github.alextoday111.projectd.runtime.RuntimeError;
import io.github.alextoday111.projectd.semantic.SemanticAnalyzer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class InterpreterTest {
    @Test
    void evaluatesArithmeticComparisonsAndConversions() {
        assertOutput("7 9 2 -3 -3 2 2.5 3.5\n"
                        + "true true true true true true true true\n"
                        + "true true false true\n"
                        + "value=7 7!\n",
                "print 1 + 2 * 3, (1 + 2) * 3, 5 / 2, -5 / 2, 5 / -2, -5 / -2, 5 / 2.0, +2.5 + 1",
                "print 1 < 2, 2 <= 2, 3 > 2.0, 2.0 >= 2, 2 = 2.0, 2 /= 3, 0.0 = -0.0, 9007199254740993 > 9007199254740992.0",
                "print true = true, \"a\" /= \"b\", true and false, not false xor false",
                "print \"value=\" + 7, 7 + \"!\"");
    }

    @Test
    void shortCircuitsAndEvaluatesOperandsLeftToRight() {
        assertOutput("false true true 1\nleft2 right3\nfirst4 second5\n",
                "var count := 0",
                "var tick := func(result) is count := count + 1; return result end",
                "print false and tick(true), true or tick(false), false xor tick(true), count",
                "var mark := func(label) is count := count + 1; return label + count end",
                "print mark(\"left\"), mark(\"right\")",
                "var join := func(a, b) => a + \" \" + b",
                "print join(mark(\"first\"), mark(\"second\"))");
    }

    @Test
    void executesGroupedDeclarationsDynamicTypesAndScopedIfs() {
        assertOutput("none true 3\ninner true\n4\ntrue true true true true\n",
                "var a, x := 3, y := x",
                "print a, a is none, y",
                "if true then var x := \"inner\"; print x, x is string; y := y + 1 else print 0 end",
                "if false then print 0 else print y end",
                "x := true",
                "var r := 1.5, ar := [], t := {1}, f := func(v) => v",
                "if x => print x is bool, r is real, ar is [], t is {}, f is func");
    }

    @Test
    void handlesAllLoopFormsAndConsumesExitAtTheNearestLoop() {
        assertOutput("1\n2\n3\n1\n2\n3\n9223372036854775807\nanonymous\n4\n5\n",
                "var n := 0",
                "while n < 5 loop",
                "    n := n + 1",
                "    loop exit end",
                "    print n",
                "    if n = 3 => exit",
                "end",
                "for i in 1..3 loop print i end",
                "for i in 3..1 loop print 0 end",
                "for i in 9223372036854775807..9223372036854775807 loop print i end",
                "for 1..1 loop print \"anonymous\" end",
                "for i in [4, 5, 6] loop",
                "    if i = 6 => exit",
                "    print i",
                "end");
    }

    @Test
    void iteratesSparseArraysAndTuplesInStableOrder() {
        assertOutput("first\nmiddle\nlast\nfront\nback\n",
                "var a := []",
                "a[100] := \"last\"; a[50] := \"middle\"; a[1] := \"first\"",
                "for item in a loop",
                "    print item",
                "    a[50] := \"changed\"",
                "    a[200] := \"new\"",
                "end",
                "var t := {named := \"front\", \"back\"}",
                "for field in t loop print field end");
    }

    @Test
    void mutatesExistingTupleSlotsAndConcatenatesCollections() {
        assertOutput("1 2 3 4\n10 10 20 30 40\n1 2\n1 100 102 200\n102\n",
                "var t := {a := 1, 2}, u := {b := 3, 4}, pair := t + u",
                "print pair.a, pair.2, pair.b, pair.4",
                "pair.a := 10; pair.2 := 20; pair.b := 30; pair.4 := 40",
                "print pair.a, pair.1, pair.2, pair.3, pair.4",
                "print t.a, t.2",
                "var a := [1], b := []",
                "a[100] := 100; b[2] := 102; b[100] := 200",
                "var joined := a + b",
                "print joined[1], joined[100], joined[102], joined[200]",
                "var nested := {array := [joined]}",
                "print nested.array[1][102]");
    }

    @Test
    void shadowsDuringInitializationAndSharesNestedValuesAfterConcatenation() {
        assertOutput("true\n7 7 7\n5 6\n",
                "var x := 5",
                "if true then var x := x; print x is none end",
                "var nested := [1], a := [nested], b := a + [2]",
                "var t := {nested := nested}, u := t + {3}",
                "b[1][1] := 7; b[1] := [5]; u.2 := 6",
                "print a[1][1], t.nested[1], u.nested[1]",
                "print b[1][1], u.2");
    }

    @Test
    void capturesLexicalBindingsAndSupportsHigherOrderFunctionsAndRecursion() {
        assertOutput("12 15 11\n103\n120\n",
                "var make := func(x) is",
                "    return func(step) is x := x + step; return x end",
                "end",
                "var next := make(10), independent := make(10)",
                "var x := 100",
                "var apply := func(f, n) => f(n)",
                "print apply(next, 2), next(3), independent(1)",
                "if true then",
                "    var add := func(y) => x + y",
                "    var x := 900",
                "    print add(3)",
                "end",
                "var factorial := func(n) is",
                "    if n <= 1 => return 1",
                "    return n * factorial(n - 1)",
                "end",
                "print factorial(5)");
    }

    @Test
    void eachIterationHasItsOwnBindingsAndReturnUnwindsLoopsAndBlocks() {
        assertOutput("1 2 3 42\n",
                "var callbacks := []",
                "for i in 1..3 loop",
                "    var current := i",
                "    callbacks[i] := func(add) => current + i + add",
                "end",
                "var get := func(x) is",
                "    loop if true then while true loop return x end end end",
                "end",
                "print callbacks[1](-1), callbacks[2](-2), callbacks[3](-3), get(42)");
    }

    @Test
    void handlesImplicitAndBareReturnsAndZeroArgumentCallsInTheAst() {
        assertOutput("true true 42\n",
                "var implicit := func(x) is var local := x end",
                "var bare := func(x) is return end",
                "print implicit(1) is none, bare(1) is none, 42");

        // The existing source grammar requires at least one argument in a call.
        List<Statement> statements = new ArrayList<>(parse("var f := func => 42").getStatements());
        statements.add(new PrintStatement(List.of(new FunctionCall(new Identifier("f"), List.of()))));
        assertOutput("42\n", new Program(statements));
    }

    @Test
    void resolvesNestedTargetsOnceBeforeEvaluatingTheValue() {
        assertOutput("3 12\n",
                "var order := 0, a := [0]",
                "var receiver := func(x) is order := order * 10 + 1; return a end",
                "var index := func(x) is order := order * 10 + 2; return x end",
                "receiver(0)[index(1)] := order",
                "print 3, a[1]");
    }

    @Test
    void rendersCyclicCollectionsWithoutRecursingForever() {
        assertOutput("[[...]] {self := {...}} [100 := 7]\n",
                "var a := [], t := {self := none}, sparse := []",
                "a[1] := a; t.self := t; sparse[100] := 7",
                "print a, t, sparse");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "print 1 / 0 | Division by zero",
            "print 1.0 / -0.0 | Division by zero",
            "print 9223372036854775807 + 1 | Integer overflow",
            "print (-9223372036854775807 - 1) / -1 | Integer overflow",
            "print -(-9223372036854775807 - 1) | Integer overflow",
            "print true + 1 | Invalid operands",
            "print none = none | Invalid operands",
            "print \"text\" + none | Invalid operands",
            "var f := 1; print f(1) | Cannot call",
            "var f := func(x, y) => x; print f(1) | expects 2 argument(s), got 1",
            "var f := func(x) => x; f(1) := 3 | Invalid assignment target",
            "var a := []; a[0] := 1 | index must be positive",
            "var a := []; print a[1.5] | index requires int",
            "var a := []; print a[100] | Missing array element",
            "var a := []; a[9223372036854775807] := 1; print a + [2] | Array index overflow",
            "var t := {1}; t.2 := 2 | Missing tuple field",
            "var t := {1}; print t.noSuchField | Missing tuple field",
            "print {a := 1, a := 2} | Duplicate tuple field",
            "print {a := 1} + {a := 2} | Duplicate tuple field",
            "if 1 then print 1 end | condition requires bool",
            "while none loop exit end | condition requires bool",
            "for i in 1..2.5 loop print i end | range end requires int",
            "for i in 3 loop print i end | for requires an integer range"
    })
    void reportsRuntimeErrorsWithAndWithoutOptimization(String source, String diagnostic) {
        Program program = parse(source);
        new SemanticAnalyzer().analyze(program);
        for (Program version : List.of(program, new AstOptimizer().optimize(program))) {
            RuntimeError error = assertThrows(RuntimeError.class, () -> run(version));
            assertTrue(error.getMessage().contains(diagnostic), error.getMessage());
        }
    }

    private static void assertOutput(String expected, String... source) {
        assertOutput(expected, parse(String.join("\n", source)));
    }

    private static void assertOutput(String expected, Program program) {
        new SemanticAnalyzer().analyze(program);
        assertEquals(expected, run(program));
        assertEquals(expected, run(new AstOptimizer().optimize(program)));
    }

    private static Program parse(String source) {
        return new Parser(new Lexer(source).scanTokens()).parse();
    }

    private static String run(Program program) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        new Interpreter(new PrintStream(bytes, true, StandardCharsets.UTF_8)).execute(program);
        return bytes.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
    }
}
