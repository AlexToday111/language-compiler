package io.github.alextoday111.projectd.optimization;

import java.util.List;
import java.util.stream.Stream;
import io.github.alextoday111.projectd.ast.*;
import io.github.alextoday111.projectd.lexer.Lexer;
import io.github.alextoday111.projectd.parser.Parser;
import io.github.alextoday111.projectd.semantic.SemanticAnalyzer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

final class AstOptimizerTest {
    private static Program parse(String source) { return new Parser(new Lexer(source).scanTokens()).parse(); }
    private static Program optimize(String source) {
        Program original = parse(source);
        new SemanticAnalyzer().analyze(original);
        String before = new AstPrinter().print(original);
        Program optimized = new AstOptimizer().optimize(original);
        assertEquals(before, new AstPrinter().print(original));
        new SemanticAnalyzer().analyze(optimized);
        assertEquals(new AstPrinter().print(optimized), new AstPrinter().print(new AstOptimizer().optimize(optimized)));
        validateStructure(optimized);
        return optimized;
    }

    private static void validateStructure(AstNode node) {
        assertNotNull(node);
        if (node instanceof FunctionLiteral) {
            FunctionLiteral function = (FunctionLiteral) node;
            assertTrue((function.getBody() == null) != (function.getExpressionBody() == null));
        }
        if (node instanceof Assignment) assertInstanceOf(Reference.class, ((Assignment) node).getTarget());
        for (AstNode child : node.getChildren()) validateStructure(child);
        assertThrows(UnsupportedOperationException.class, () -> node.getChildren().clear());
    }

    static Stream<Arguments> constants() {
        return Stream.of(
                Arguments.of("5 + 3 * 2", 11L), Arguments.of("(5 + 3) * 2", 16L),
                Arguments.of("10 - 4 - 2", 4L), Arguments.of("-7 / 3", -3L),
                Arguments.of("7 / -3", -3L), Arguments.of("-7 / -3", 2L),
                Arguments.of("8 / 2 * 3", 12L), Arguments.of("+2", 2L),
                Arguments.of("-(2 + 3)", -5L), Arguments.of("1.5 + 2.5", 4.0),
                Arguments.of("1 + 2.5", 3.5), Arguments.of("5.0 - 1.5", 3.5),
                Arguments.of("1.5 * 2.0", 3.0), Arguments.of("5.0 / 2", 2.5),
                Arguments.of("-1.5", -1.5), Arguments.of("+1.5", 1.5),
                Arguments.of("10 > 3", true), Arguments.of("10 < 3", false),
                Arguments.of("3 <= 3", true), Arguments.of("3 >= 4", false),
                Arguments.of("3 = 3", true), Arguments.of("3 /= 3", false),
                Arguments.of("1.5 < 2", true), Arguments.of("2 = 2.0", true),
                Arguments.of("1.5 /= 2.5", true), Arguments.of("-0.0 = 0.0", true),
                Arguments.of("not (true)", false), Arguments.of("true and false", false),
                Arguments.of("true or false", true), Arguments.of("true xor true", false),
                Arguments.of("true or false and false", false),
                Arguments.of("(5 + 3 * 2 > 10) and not (false)", true),
                Arguments.of("9223372036854775807 > 9223372036854775806", true)
        );
    }

    @ParameterizedTest
    @MethodSource("constants")
    void foldsConstantsPreservingTheNumericType(String source, Object expected) {
        Expression result = ((PrintStatement) optimize("print " + source).getStatements().get(0)).getExpressions().get(0);
        assertInstanceOf(Literal.class, result);
        assertEquals(expected, ((Literal<?>) result).getValue());
        assertEquals(expected.getClass(), ((Literal<?>) result).getValue().getClass());
    }

    @ParameterizedTest
    @ValueSource(strings = {"9223372036854775807 + 1", "9223372036854775807 * 2",
            "(-9223372036854775807 - 1) - 1", "(-9223372036854775807 - 1) / -1",
            "-(-9223372036854775807 - 1)", "1 / 0", "1.0 / 0.0", "1.0 / 3.0",
            "0.1 + 0.2", "0.0 * -1.0", "1.0 - 1.0", "9007199254740993 + 1.0",
            "9007199254740993 = 9007199254740992.0", "\"a\" + \"b\"", "true = false", "none = none",
            "[1] + [2]", "{1} + {2}", "true + 1"})
    void retainsUncertainInvalidOrExceptionalOperations(String source) {
        Expression result = ((PrintStatement) optimize("print " + source).getStatements().get(0)).getExpressions().get(0);
        assertFalse(result instanceof Literal<?>, source);
    }

    @Test
    void preservesNonFiniteValuesAndNegativeZeroWithoutGuessingTheirArithmetic() {
        Expression infinity = new RealLiteral(Double.POSITIVE_INFINITY, "infinity");
        Program original = new Program(List.of(new PrintStatement(List.of(
                new BinaryExpression("+", infinity, infinity),
                new UnaryExpression("-", new RealLiteral(0.0, "0.0"))))));
        List<Expression> result = ((PrintStatement) new AstOptimizer().optimize(original).getStatements().get(0)).getExpressions();
        assertInstanceOf(BinaryExpression.class, result.get(0));
        assertEquals(Double.doubleToRawLongBits(-0.0),
                Double.doubleToRawLongBits(((RealLiteral) result.get(1)).getValue()));
    }

    @Test
    void selectsBranchesWithoutFlatteningTheirScopesOrChangingOrder() {
        Program program = optimize("var x := 1; if 10 > 3 then var x := 5 + 3 * 2; print x else print 99 end; print x");
        assertEquals(3, program.getStatements().size());
        ScopedBlock selected = (ScopedBlock) program.getStatements().get(1);
        VariableDeclaration inner = (VariableDeclaration) selected.getBody().getStatements().get(0);
        assertEquals(11L, ((IntegerLiteral) inner.getInitializer()).getValue());
        assertInstanceOf(PrintStatement.class, selected.getBody().getStatements().get(1));
        Program alternative = optimize("if false then print 1 else print 2 end; if false => print 3; print 4");
        assertEquals(2, alternative.getStatements().size());
        PrintStatement print = (PrintStatement) ((ScopedBlock) alternative.getStatements().get(0)).getBody().getStatements().get(0);
        assertEquals(2L, ((IntegerLiteral) print.getExpressions().get(0)).getValue());
    }

    private static FunctionLiteral function(Program program) {
        return (FunctionLiteral) ((VariableDeclaration) program.getStatements().get(0)).getInitializer();
    }

    @Test
    void removesOnlyUnreachableStatementsAfterReturnsIncludingBothTerminatingBranches() {
        Program program = optimize("var f := func(x) is if x then return 1 else return 2 end; print x; return 3 end; print 4");
        assertEquals(1, function(program).getBody().getStatements().size());
        assertEquals(2, program.getStatements().size());
        program = optimize("var f := func(x) is if true => return x; print 99 end");
        assertEquals(1, function(program).getBody().getStatements().size());
        program = optimize("var f := func(x) is if x => return 1; print x end");
        assertEquals(2, function(program).getBody().getStatements().size());
    }

    @Test
    void exitTerminatesOnlyTheApplicableLoopBodyAndFunctionsResetLoopContext() {
        Program program = optimize("loop loop exit; print 99 end; print 1; exit; print 2 end; print 3");
        InfiniteLoop outer = (InfiniteLoop) program.getStatements().get(0);
        assertEquals(3, outer.getBody().getStatements().size());
        assertEquals(1, ((InfiniteLoop) outer.getBody().getStatements().get(0)).getBody().getStatements().size());
        assertEquals(2, program.getStatements().size());
        program = optimize("var f := func(x) is loop if x then return x else exit end; print 99 end; print 1; return x end");
        assertEquals(3, function(program).getBody().getStatements().size());
        assertEquals(1, ((InfiniteLoop) function(program).getBody().getStatements().get(0)).getBody().getStatements().size());

        assertEquals(2, new AstOptimizer().optimize(parse("exit; print 1")).getStatements().size());
        assertEquals(2, new AstOptimizer().optimize(parse("return; print 1")).getStatements().size());
    }

    @Test
    void rewritesEveryCompositeExpressionAndStatementWithoutDroppingEffects() {
        Program program = optimize("var f := func(x) => x + (1 + 2), a := [1 + 2], "
                + "t := {field := f(1 + 2), other := func(x) is return 1 + 2; print 99 end}; "
                + "a[1 + 1] := f(2 + 2); print f(1) * 0, false and f(2), t.field is int; "
                + "while f(1) loop print 1 + 2; exit; print 99 end; "
                + "for i in 1 + 1..2 + 2 loop print f(i); exit; print 99 end");
        String tree = new AstPrinter().print(program);
        assertTrue(tree.contains("IntegerLiteral: 3"));
        assertFalse(tree.contains("IntegerLiteral: 99"));
        PrintStatement print = (PrintStatement) program.getStatements().get(2);
        assertInstanceOf(BinaryExpression.class, print.getExpressions().get(0));
        assertInstanceOf(BinaryExpression.class, print.getExpressions().get(1));
        assertInstanceOf(TypeCheckExpression.class, print.getExpressions().get(2));
        assertInstanceOf(WhileLoop.class, program.getStatements().get(3));
        ForLoop loop = (ForLoop) program.getStatements().get(4);
        assertEquals(2L, ((IntegerLiteral) loop.getIterable()).getValue());
        assertEquals(4L, ((IntegerLiteral) loop.getRangeEnd()).getValue());
    }

    @Test
    void doesNotPropagateVariablesDropUnusedDeclarationsOrAssumeLoopTermination() {
        Program program = optimize("var unused := 1; var x := 2; print x + 1; while false loop print 3 end; print 4");
        assertEquals(5, program.getStatements().size());
        assertInstanceOf(BinaryExpression.class, ((PrintStatement) program.getStatements().get(2)).getExpressions().get(0));
        assertEquals(1, ((WhileLoop) program.getStatements().get(3)).getBody().getStatements().size());
    }

    @Test
    void selectedBranchesPreserveCapturedShadowedBindings() {
        Program program = optimize("var x := 1; var selected; if true then var x := 2; "
                + "selected := func(value) => x + value end; print x, selected(3)");
        ScopedBlock scope = (ScopedBlock) program.getStatements().get(2);
        assertEquals(2, scope.getBody().getStatements().size());
        assertInstanceOf(VariableDeclaration.class, scope.getBody().getStatements().get(0));
        assertInstanceOf(Assignment.class, scope.getBody().getStatements().get(1));

        assertEquals(4, program.getStatements().size());
    }

    @Test
    void optimizerAlsoResetsContextForAFunctionInsideALoopWhenUsedIndependently() {
        Program original = parse("loop var f := func is exit; print 1 end; exit; print 2 end");
        InfiniteLoop loop = (InfiniteLoop) new AstOptimizer().optimize(original).getStatements().get(0);
        assertEquals(2, loop.getBody().getStatements().size());
        FunctionLiteral nested = (FunctionLiteral) ((VariableDeclaration) loop.getBody().getStatements().get(0)).getInitializer();
        assertEquals(2, nested.getBody().getStatements().size());
    }

    @Test
    void preservesCallAndArgumentOrderAndRetainsUnusedSideEffectingInitializers() {
        Program original = parse("var effect := func(x) is print x; return x end; "
                + "var unused := effect(1); print effect(2) + effect(3), effect(effect(4))");
        new SemanticAnalyzer().analyze(original);
        Program optimized = new AstOptimizer().optimize(original);
        assertEquals(new AstPrinter().print(original), new AstPrinter().print(optimized));
    }
}
