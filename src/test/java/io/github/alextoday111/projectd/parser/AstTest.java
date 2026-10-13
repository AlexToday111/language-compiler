package io.github.alextoday111.projectd.parser;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.github.alextoday111.projectd.ast.*;
import io.github.alextoday111.projectd.lexer.Lexer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

final class AstTest {
    @TempDir
    Path temporaryDirectory;

    private Program parse(String source) {
        return new Parser(new Lexer(source).scanTokens()).parse();
    }

    private Expression expression(String source) {
        return ((PrintStatement) parse("print " + source).getStatements().get(0)).getExpressions().get(0);
    }

    @Test
    void preservesArithmeticPrecedenceAndGrouping() {
        BinaryExpression sum = (BinaryExpression) expression("10 + 5 * 2");
        assertEquals("+", sum.getOperator());
        assertEquals(10L, ((IntegerLiteral) sum.getLeft()).getValue());
        BinaryExpression product = (BinaryExpression) sum.getRight();
        assertEquals("*", product.getOperator());
        assertEquals(5L, ((IntegerLiteral) product.getLeft()).getValue());
        assertEquals(2L, ((IntegerLiteral) product.getRight()).getValue());
        BinaryExpression grouped = (BinaryExpression) expression("(10 + 5) * 2");
        assertEquals("*", grouped.getOperator());
        assertEquals("+", ((BinaryExpression) grouped.getLeft()).getOperator());
    }

    @Test
    void foldsRepeatedOperatorsLeftAtEachTier() {
        BinaryExpression subtraction = (BinaryExpression) expression("10 - 4 - 2");
        assertEquals("-", subtraction.getOperator());
        assertEquals("-", ((BinaryExpression) subtraction.getLeft()).getOperator());
        assertEquals(2L, ((IntegerLiteral) subtraction.getRight()).getValue());
        BinaryExpression product = (BinaryExpression) expression("8 / 2 * 3");
        assertEquals("*", product.getOperator());
        assertEquals("/", ((BinaryExpression) product.getLeft()).getOperator());
        BinaryExpression logical = (BinaryExpression) expression("true or false and true xor false");
        assertEquals("xor", logical.getOperator());
        BinaryExpression and = (BinaryExpression) logical.getLeft();
        assertEquals("and", and.getOperator());
        assertEquals("or", ((BinaryExpression) and.getLeft()).getOperator());
    }

    @Test
    void nestsRelationsBetweenArithmeticAndLogic() {
        BinaryExpression logical = (BinaryExpression) expression("1 + 2 < 3 * 4 and x = y");
        assertEquals("and", logical.getOperator());
        BinaryExpression relation = (BinaryExpression) logical.getLeft();
        assertEquals("<", relation.getOperator());
        assertEquals("+", ((BinaryExpression) relation.getLeft()).getOperator());
        assertEquals("*", ((BinaryExpression) relation.getRight()).getOperator());
        assertEquals("=", ((BinaryExpression) logical.getRight()).getOperator());
    }

    @Test
    void preservesDeclarationsAndStatementOrder() {
        Program program = parse("var a, b := 2; var c := 3; a := b");
        assertEquals(3, program.getStatements().size());
        VariableDeclarationGroup group = (VariableDeclarationGroup) program.getStatements().get(0);
        assertEquals(2, group.getDeclarations().size());
        assertEquals("a", group.getDeclarations().get(0).getIdentifier().getName());
        assertNull(group.getDeclarations().get(0).getInitializer());
        assertEquals("b", group.getDeclarations().get(1).getIdentifier().getName());
        assertEquals(2L, ((IntegerLiteral) group.getDeclarations().get(1).getInitializer()).getValue());
        assertEquals("c", ((VariableDeclaration) program.getStatements().get(1)).getIdentifier().getName());
        Assignment assignment = (Assignment) program.getStatements().get(2);
        assertEquals("a", ((Identifier) assignment.getTarget()).getName());
        assertEquals("b", ((Identifier) assignment.getValue()).getName());
    }

    @Test
    void buildsNestedFunctionsWithOrderedParametersAndBothBodyForms() {
        VariableDeclaration declaration = (VariableDeclaration) parse(
                "var f := func(x, y) is return func(z) => x + z end").getStatements().get(0);
        FunctionLiteral outer = (FunctionLiteral) declaration.getInitializer();
        assertEquals("x", outer.getParameters().getIdentifiers().get(0).getName());
        assertEquals("y", outer.getParameters().getIdentifiers().get(1).getName());
        assertNull(outer.getExpressionBody());
        ReturnStatement returned = (ReturnStatement) outer.getBody().getStatements().get(0);
        FunctionLiteral inner = (FunctionLiteral) returned.getValue();
        assertEquals("z", inner.getParameters().getIdentifiers().get(0).getName());
        assertNull(inner.getBody());
        assertEquals("+", ((BinaryExpression) inner.getExpressionBody()).getOperator());
        FunctionLiteral noParameters = (FunctionLiteral) expression("func => 1");
        assertTrue(noParameters.getParameters().getIdentifiers().isEmpty());
        assertEquals(1L, ((IntegerLiteral) noParameters.getExpressionBody()).getValue());
    }

    @Test
    void buildsNestedCallsAndPreservesArgumentOrder() {
        FunctionCall call = (FunctionCall) expression("f(g(1), 2)(3)");
        assertEquals(3L, ((IntegerLiteral) call.getArguments().get(0)).getValue());
        FunctionCall inner = (FunctionCall) call.getCallee();
        assertEquals("f", ((Identifier) inner.getCallee()).getName());
        assertEquals(2, inner.getArguments().size());
        FunctionCall argument = (FunctionCall) inner.getArguments().get(0);
        assertEquals("g", ((Identifier) argument.getCallee()).getName());
        assertEquals(1L, ((IntegerLiteral) argument.getArguments().get(0)).getValue());
        assertEquals(2L, ((IntegerLiteral) inner.getArguments().get(1)).getValue());
    }

    @Test
    void preservesNestedCollectionsAndNamedAndPositionalTupleElements() {
        ArrayLiteral array = (ArrayLiteral) expression("[{items := [1, {2, flag := true}]}, []]");
        assertEquals(2, array.getElements().size());
        assertTrue(((ArrayLiteral) array.getElements().get(1)).getElements().isEmpty());
        TupleLiteral tuple = (TupleLiteral) array.getElements().get(0);
        TupleElement items = tuple.getElements().get(0);
        assertEquals("items", items.getName().getName());
        ArrayLiteral nested = (ArrayLiteral) items.getValue();
        assertEquals(1L, ((IntegerLiteral) nested.getElements().get(0)).getValue());
        TupleLiteral pair = (TupleLiteral) nested.getElements().get(1);
        assertNull(pair.getElements().get(0).getName());
        assertEquals(2L, ((IntegerLiteral) pair.getElements().get(0).getValue()).getValue());
        assertEquals("flag", pair.getElements().get(1).getName().getName());
        assertTrue(((BooleanLiteral) pair.getElements().get(1).getValue()).getValue());
    }

    @Test
    void preservesFullAndShortConditionalsAndElseOwnership() {
        Program program = parse("if x then if y => exit else print 2 end; print 3");
        assertEquals(2, program.getStatements().size());
        IfStatement outer = (IfStatement) program.getStatements().get(0);
        assertFalse(outer.isShortForm());
        assertEquals("x", ((Identifier) outer.getCondition()).getName());
        IfStatement inner = (IfStatement) outer.getThenBranch().getStatements().get(0);
        assertTrue(inner.isShortForm());
        assertNull(inner.getElseBranch());
        assertInstanceOf(ExitStatement.class, inner.getThenBranch().getStatements().get(0));
        PrintStatement alternative = (PrintStatement) outer.getElseBranch().getBody().getStatements().get(0);
        assertEquals(2L, ((IntegerLiteral) alternative.getExpressions().get(0)).getValue());
    }

    @Test
    void preservesWhileForAndInfiniteLoopsAndNestedBodies() {
        Program program = parse("while x > 0 loop x := x - 1 end; "
                + "for i in 1..3 loop loop exit end end; for items loop print 1 end");
        WhileLoop whileLoop = (WhileLoop) program.getStatements().get(0);
        assertEquals(">", ((BinaryExpression) whileLoop.getCondition()).getOperator());
        assertInstanceOf(Assignment.class, whileLoop.getBody().getStatements().get(0));
        ForLoop range = (ForLoop) program.getStatements().get(1);
        assertEquals("i", range.getVariable().getName());
        assertEquals(1L, ((IntegerLiteral) range.getIterable()).getValue());
        assertEquals(3L, ((IntegerLiteral) range.getRangeEnd()).getValue());
        InfiniteLoop infinite = (InfiniteLoop) range.getBody().getStatements().get(0);
        assertInstanceOf(ExitStatement.class, infinite.getBody().getStatements().get(0));
        ForLoop collection = (ForLoop) program.getStatements().get(2);
        assertNull(collection.getVariable());
        assertNull(collection.getRangeEnd());
        assertEquals("items", ((Identifier) collection.getIterable()).getName());
    }

    @Test
    void supportsAllForHeaderForms() {
        ForLoop unnamedRange = (ForLoop) parse("for 2..4 loop exit end").getStatements().get(0);
        assertNull(unnamedRange.getVariable());
        assertEquals(4L, ((IntegerLiteral) unnamedRange.getRangeEnd()).getValue());
        ForLoop namedCollection = (ForLoop) parse("for item in items loop print item end").getStatements().get(0);
        assertEquals("item", namedCollection.getVariable().getName());
        assertNull(namedCollection.getRangeEnd());
        assertEquals("items", ((Identifier) namedCollection.getIterable()).getName());
    }

    @Test
    void preservesReturnWithAndWithoutValue() {
        Program program = parse("return; return none; return 42");
        assertNull(((ReturnStatement) program.getStatements().get(0)).getValue());
        assertInstanceOf(NoneLiteral.class, ((ReturnStatement) program.getStatements().get(1)).getValue());
        assertEquals(42L, ((IntegerLiteral) ((ReturnStatement) program.getStatements().get(2)).getValue()).getValue());
    }

    @Test
    void preservesCompositeAssignmentTargets() {
        Assignment assignment = (Assignment) parse("data.records[1].payload.score := 7").getStatements().get(0);
        TupleFieldAccess score = (TupleFieldAccess) assignment.getTarget();
        assertEquals("score", score.getField().getName());
        TupleFieldAccess payload = (TupleFieldAccess) score.getTarget();
        assertEquals("payload", payload.getField().getName());
        ArrayIndex index = (ArrayIndex) payload.getTarget();
        assertEquals(1L, ((IntegerLiteral) index.getIndex()).getValue());
        TupleFieldAccess records = (TupleFieldAccess) index.getTarget();
        assertEquals("records", records.getField().getName());
        assertEquals("data", ((Identifier) records.getTarget()).getName());
        assertEquals(7L, ((IntegerLiteral) assignment.getValue()).getValue());
    }

    @Test
    void preservesNumericFieldsCallsAndIndicesInOneReference() {
        TupleFieldAccess field = (TupleFieldAccess) expression("f(x).1[2].3");
        assertEquals(3L, field.getField().getPosition());
        assertNull(field.getField().getName());
        ArrayIndex index = (ArrayIndex) field.getTarget();
        assertEquals(2L, ((IntegerLiteral) index.getIndex()).getValue());
        TupleFieldAccess first = (TupleFieldAccess) index.getTarget();
        assertEquals(1L, first.getField().getPosition());
        assertEquals("f", ((Identifier) ((FunctionCall) first.getTarget()).getCallee()).getName());
    }

    @ParameterizedTest
    @ValueSource(strings = {"int", "real", "bool", "string", "none", "[]", "{}", "func"})
    void preservesEveryTypeIndicator(String type) {
        TypeCheckExpression check = (TypeCheckExpression) expression("data[1].value is " + type);
        assertEquals(type, check.getType().getSpelling());
        assertEquals("value", ((TupleFieldAccess) check.getReference()).getField().getName());
    }

    @Test
    void preservesTypedLiteralValuesAndUnaryNodes() {
        PrintStatement print = (PrintStatement) parse("print -2, +1.25, not (flag), 'a\\nb', false, none")
                .getStatements().get(0);
        UnaryExpression negative = (UnaryExpression) print.getExpressions().get(0);
        assertEquals("-", negative.getOperator());
        assertEquals(2L, ((IntegerLiteral) negative.getOperand()).getValue());
        UnaryExpression positive = (UnaryExpression) print.getExpressions().get(1);
        assertEquals("+", positive.getOperator());
        assertEquals(1.25, ((RealLiteral) positive.getOperand()).getValue());
        UnaryExpression not = (UnaryExpression) print.getExpressions().get(2);
        assertEquals("not", not.getOperator());
        assertEquals("flag", ((Identifier) not.getOperand()).getName());
        StringLiteral string = (StringLiteral) print.getExpressions().get(3);
        assertEquals("a\nb", string.getValue());
        assertEquals("'a\\nb'", string.getLexeme());
        assertFalse(((BooleanLiteral) print.getExpressions().get(4)).getValue());
        assertNull(((NoneLiteral) print.getExpressions().get(5)).getValue());
    }

    @Test
    void snapshotsAstListsAndParsesFreshTrees() {
        List<Statement> statements = new ArrayList<>();
        statements.add(new ExitStatement());
        Program program = new Program(statements);
        statements.clear();
        assertEquals(1, program.getStatements().size());
        assertThrows(UnsupportedOperationException.class, () -> program.getStatements().clear());
        Parser parser = new Parser(new Lexer("var x := 1").scanTokens());
        Program first = parser.parse();
        Program second = parser.parse();
        assertNotSame(first, second);
        assertEquals(new AstPrinter().print(first), new AstPrinter().print(second));
        assertTrue(parse("\n;;").getStatements().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"print -x", "print f()", "print {}", "print a < b < c",
            "if x =>\nexit", "var f := func() => 1", "print [1,]", "var x := 1 var y := 2"})
    void rejectsInvalidSyntaxWhileBuildingTrees(String source) {
        ParseException failure = assertThrows(ParseException.class, () -> parse(source));
        assertTrue(failure.getLine() > 0);
        assertTrue(failure.getColumn() > 0);
        assertEquals("Syntax error", failure.getErrorType());
    }

    @Test
    void printsExactDeterministicTree() {
        Program program = parse("var x := 10 + 5 * 2");
        String expected = "Program\n"
                + "└── VariableDeclaration\n"
                + "    ├── Identifier: x\n"
                + "    └── BinaryExpression: +\n"
                + "        ├── IntegerLiteral: 10\n"
                + "        └── BinaryExpression: *\n"
                + "            ├── IntegerLiteral: 5\n"
                + "            └── IntegerLiteral: 2\n";
        assertEquals(expected, new AstPrinter().print(program));
        assertEquals(expected, new AstPrinter().print(program));
        assertEquals("Program\n", new AstPrinter().print(parse("")));
    }

    @Test
    void printsBranchGuidesEscapesAndNestedNodes() {
        String printed = new AstPrinter().print(parse("if x then print 'a\\nb', x is [] else loop exit end end; return"));
        assertTrue(printed.contains("│   ├── Identifier: x\n"), printed);
        assertTrue(printed.contains("StringLiteral: \"a\\nb\""), printed);
        assertTrue(printed.contains("TypeIndicator: []"), printed);
        assertTrue(printed.contains("Else\n"), printed);
        assertTrue(printed.contains("InfiniteLoop\n"), printed);
        assertTrue(printed.endsWith("└── ReturnStatement\n"), printed);
        assertFalse(printed.matches("(?s).*@[0-9a-f]+.*"));
    }

    @Test
    void printsDeepTreesWithoutRecursiveTraversal() {
        Expression value = new IntegerLiteral(1L, "1");
        for (int i = 0; i < 1500; i++) value = new UnaryExpression("-", value);
        String printed = new AstPrinter().print(value);
        assertEquals(1500, printed.lines().filter(line -> line.contains("UnaryExpression")).count());
        assertTrue(printed.endsWith("IntegerLiteral: 1\n"));
    }

    @Test
    void cliPrintsAstAndPreservesFailureStreamsAndCodes() throws Exception {
        Path file = Files.writeString(temporaryDirectory.resolve("good.d"), "var x := 10 + 5 * 2");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        try (PrintStream out = new PrintStream(output, true, "UTF-8");
             PrintStream err = new PrintStream(errors, true, "UTF-8")) {
            assertEquals(0, ParserMain.run(new String[]{"--ast", file.toString()}, out, err));
            assertEquals(new AstPrinter().print(parse("var x := 10 + 5 * 2")), output.toString("UTF-8"));
            assertEquals("", errors.toString("UTF-8"));
            output.reset();
            assertEquals(1, ParserMain.run(new String[]{"--ast", "examples/valid/02_calculator.d"}, out, err));
            assertEquals("", output.toString("UTF-8"));
            assertTrue(errors.toString("UTF-8").contains("Syntax error at 13:32"));
            errors.reset();
            Files.writeString(file, "print @");
            assertEquals(1, ParserMain.run(new String[]{"--ast", file.toString()}, out, err));
            assertEquals("", output.toString("UTF-8"));
            assertTrue(errors.toString("UTF-8").contains("Lexical error"));
            assertEquals(2, ParserMain.run(new String[]{"--ast"}, out, err));
            assertEquals(2, ParserMain.run(new String[]{"--unknown", file.toString()}, out, err));
            assertEquals(3, ParserMain.run(new String[]{"--ast", temporaryDirectory.resolve("missing.d").toString()}, out, err));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"01_calculator.d", "02_calculator.d"})
    void astCliHasCorrectProcessExitCodes(String file) throws Exception {
        Path java = Path.of(System.getProperty("java.home"), "bin", "java");
        Path stdout = temporaryDirectory.resolve("stdout.txt");
        Path stderr = temporaryDirectory.resolve("stderr.txt");
        Process process = new ProcessBuilder(java.toString(), "-cp", "target/classes",
                ParserMain.class.getName(), "--ast", "examples/valid/" + file)
                .redirectOutput(stdout.toFile()).redirectError(stderr.toFile()).start();
        try {
            assertTrue(process.waitFor(10, TimeUnit.SECONDS));
            String output = Files.readString(stdout, StandardCharsets.UTF_8);
            String errors = Files.readString(stderr, StandardCharsets.UTF_8);
            if (file.startsWith("01")) {
                assertEquals(0, process.exitValue(), errors);
                assertTrue(output.startsWith("Program\n"), output);
                assertTrue(output.contains("└──"), output);
                assertEquals("", errors);
            } else {
                assertEquals(1, process.exitValue());
                assertEquals("", output);
                assertTrue(errors.contains("Syntax error at 13:32"), errors);
            }
        } finally {
            process.destroyForcibly();
        }
    }
}
