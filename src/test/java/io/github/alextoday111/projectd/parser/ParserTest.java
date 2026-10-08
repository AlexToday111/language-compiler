package io.github.alextoday111.projectd.parser;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import io.github.alextoday111.projectd.lexer.Lexer;
import io.github.alextoday111.projectd.lexer.Token;
import io.github.alextoday111.projectd.lexer.TokenType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static io.github.alextoday111.projectd.lexer.TokenType.*;
import static org.junit.jupiter.api.Assertions.*;

final class ParserTest {
    @ParameterizedTest(name = "valid: {0}")
    @MethodSource("validPrograms")
    void acceptsGrammarPrograms(String source) {
        assertDoesNotThrow(() -> new Parser(new Lexer(source).scanTokens()).parse());
    }

    static Stream<String> validPrograms() {
        return Stream.of(
                "", "\n;;\n;", "// only a comment",
                "var x := 10", "var a, b, c", "var x := 1, y := 2",
                "x := 20", "array[1] := 10", "tuple.field := 5",
                "print 1 + 2 * 3", "print (1 + 2) * 3",
                "print 10 - 4 - 2", "print 8 / 2 * 3",
                "print true or false and true xor false",
                "print 1 + 2 < 3 * 4 and x = y",
                "print a < b, a <= b, a > b, a >= b, a = b, a /= b",
                "print +1, -2, not true, -(x), not (flag)",
                "print x is int, x is real, x is bool, x is string, x is none",
                "print x is [], x is {}, x is func",
                "print x is int + 1",
                "if x > 10 then\nprint x\nelse\nprint 0\nend",
                "if x then print 1 end",
                "if x then if y then print 1 else print 2 end else print 3 end",
                "if x then while y loop y := y - 1 end end",
                "if x => print 1; print 2",
                "if x => if y => exit",
                "if x => while y loop exit end",
                "if x then if y => print 1 else print 2 end",
                "if x => if y then print 1 else print 2 end",
                "while x > 0 loop x := x - 1 end",
                "for i in 1..10 loop print i end",
                "for 1..3 loop print 1 end",
                "for i in array loop print i end",
                "for array loop print 1 end",
                "for array[1] loop print 1 end",
                "loop if condition => exit end",
                "for i in 1..3 loop for j in 1..2 loop print i, j end end",
                "return x", "return", "exit",
                "print x, y, z", "print 12.345, true, false, none, \"text\", 'value'",
                "\n;var x := 1;\n\n;x := 2; print x;\n",
                "if x then ;\nprint 1;\n; else ;print 2; end",
                "var f := func(x) => x + 1",
                "var f := func(x, y) is\nreturn x + y\nend",
                "var f := func => 1", "var f := func is return end",
                "var applyTwice := func(f, x) => f(f(x))",
                "print transform(rowSum, config.values, config)",
                "var f := func(x) is return func(y) => y end",
                "print a.1.2.3, data.metadata.version, matrix[2][1]",
                "print records[1].payload.samples[2], f(x).1.2",
                "print a.1[2].name.3, f(x)(y)[1].name",
                "var empty := []", "var a := [1, 2.5, true]",
                "var a := [[1, 2], [3, 4]]",
                "var t := {a := 10}", "var t := {x + 10}",
                "var t := {a := 1, 2, b := {3, 4}}",
                "var a := [{x := 1}, {x := 2}]",
                "var data := {\nmetadata := {\nversion := 1,\nname := \"demo\"\n},\nvalues := [[1, 2], [3, 4]]\n}",
                "var a := [\n1\n,\n2\n]", "var a := [\n\n]",
                "var t := {\na := 1\n,\nx + 10\n}",
                "var t := {f := func(x) is return x end, g := func(x) => x + 1}",
                // Syntax validation deliberately leaves these semantic issues alone.
                "return unknown + 1; exit", "var x, x; x := true + 10",
                "f(x) := value", "array[1.5] := value",
                "var t := {a := 1, a := 2}"
        );
    }

    @ParameterizedTest(name = "invalid: {0}")
    @MethodSource("invalidPrograms")
    void rejectsAtMarkedTokenWithExpectedDiagnostic(String marked, TokenType actualType, String expected) {
        int marker = marked.indexOf("<error>");
        assertTrue(marker >= 0);
        String source = marked.replace("<error>", "");
        String prefix = marked.substring(0, marker);
        int line = 1 + (int) prefix.chars().filter(c -> c == '\n').count();
        int column = prefix.length() - prefix.lastIndexOf('\n');
        List<Token> tokens = new Lexer(source).scanTokens();
        ParseException error = assertThrows(ParseException.class, () -> new Parser(tokens).parse());
        assertEquals("Syntax error", error.getErrorType());
        assertEquals(actualType, error.getActualToken().getType());
        assertEquals(line, error.getLine());
        assertEquals(column, error.getColumn());
        assertTrue(error.getExpected().contains(expected), error.getMessage());
        assertTrue(error.getMessage().contains("Syntax error at " + line + ":" + column));
        assertTrue(error.getMessage().contains("Expected ") && error.getMessage().contains("found " + actualType));
        assertTrue(tokens.stream().anyMatch(token -> token == error.getActualToken()));
    }

    static Stream<Arguments> invalidPrograms() {
        return Stream.of(
                Arguments.of("var x :=<error>", EOF, "expression"),
                Arguments.of("var <error>:= 10", ASSIGN, "IDENT"),
                Arguments.of("var x := 1 +<error>", EOF, "expression"),
                Arguments.of("if x > 0 then<error>", EOF, "statement"),
                Arguments.of("while x > 0 loop<error>", EOF, "statement"),
                Arguments.of("print<error>", EOF, "expression"),
                Arguments.of("a.1.<error>", EOF, "IDENT or INTEGER"),
                Arguments.of("print (1 + 2<error>", EOF, "RPAREN"),
                Arguments.of("print a[1<error>", EOF, "RBRACKET"),
                Arguments.of("print [1<error>", EOF, "RBRACKET"),
                Arguments.of("print {a := 1<error>", EOF, "RBRACE"),
                Arguments.of("if x then print 1<error>", EOF, "KW_END"),
                Arguments.of("while x loop print 1<error>", EOF, "KW_END"),
                Arguments.of("<error>else", KW_ELSE, "statement"),
                Arguments.of("<error>end", KW_END, "statement"),
                Arguments.of("print 1 + <error>* 3", STAR, "expression"),
                Arguments.of("var t := {a :=<error>}", RBRACE, "expression"),
                Arguments.of("print a < b <error>< c", LESS, "comparison"),
                Arguments.of("print a = b <error>= c", EQUAL, "comparison"),
                Arguments.of("var x := 1 <error>var y := 2", KW_VAR, "NEWLINE or SEMICOLON"),
                Arguments.of("print 1,<error>", EOF, "expression"),
                Arguments.of("print f(<error>)", RPAREN, "expression"),
                Arguments.of("var f := func(<error>) => 1", RPAREN, "IDENT"),
                Arguments.of("print {<error>}", RBRACE, "expression"),
                Arguments.of("print -<error>x", IDENT, "expression"),
                Arguments.of("print not <error>x", IDENT, "expression"),
                Arguments.of("print -<error>-1", MINUS, "expression"),
                Arguments.of("print 1 <error>is int", KW_IS, "NEWLINE or SEMICOLON"),
                Arguments.of("print (x) <error>is int", KW_IS, "NEWLINE or SEMICOLON"),
                Arguments.of("print x is <error>true", KW_TRUE, "type indicator"),
                Arguments.of("print x is [<error>1]", INTEGER, "RBRACKET"),
                Arguments.of("print x is {<error>a}", IDENT, "RBRACE"),
                Arguments.of("print a.<error>", EOF, "IDENT or INTEGER"),
                Arguments.of("print f(1,<error>)", RPAREN, "expression"),
                Arguments.of("print f(1<error>\n,2)", NEWLINE, "RPAREN"),
                Arguments.of("print [1\n<error>2]", INTEGER, "COMMA or RBRACKET"),
                Arguments.of("print [1<error>;2]", SEMICOLON, "COMMA or RBRACKET"),
                Arguments.of("print [1,<error>]", RBRACKET, "expression"),
                Arguments.of("print {a := 1,<error>}", RBRACE, "expression"),
                Arguments.of("print {a\n<error>:= 1}", ASSIGN, "COMMA or RBRACE"),
                Arguments.of("if x =><error>\nprint 1", NEWLINE, "statement"),
                Arguments.of("var f := func(x) =><error>\nx", NEWLINE, "expression"),
                Arguments.of("if x then <error>end", KW_END, "statement"),
                Arguments.of("if x then print 1 else <error>end", KW_END, "statement"),
                Arguments.of("loop <error>end", KW_END, "statement"),
                Arguments.of("if x then\nwhile y loop\nprint 1\nend<error>", EOF, "KW_END"),
                Arguments.of("if x => print 1\n<error>else print 2", KW_ELSE, "statement"),
                Arguments.of("if x => print 1\n<error>end", KW_END, "statement"),
                Arguments.of("var x <error>= 1", EQUAL, "NEWLINE or SEMICOLON"),
                Arguments.of("x <error>= 1", EQUAL, "ASSIGN"),
                Arguments.of("fun <error>f(x) { return x }", IDENT, "ASSIGN"),
                Arguments.of("var f := lambda(x) <error>=> x", FAT_ARROW, "NEWLINE or SEMICOLON"),
                Arguments.of("if x <error>{print 1}", LBRACE, "KW_THEN"),
                Arguments.of("while x <error>{print 1}", LBRACE, "KW_LOOP"),
                Arguments.of("var f := func(x, <error>) => x", RPAREN, "IDENT"),
                Arguments.of("var f := func(x) <error>then print x", KW_THEN, "function body"),
                Arguments.of("var f := func(x) is return x<error>", EOF, "KW_END"),
                Arguments.of("var f := func is <error>end", KW_END, "statement"),
                Arguments.of("print 1 <error>2", INTEGER, "NEWLINE or SEMICOLON"),
                Arguments.of("print (1 <error>2)", INTEGER, "RPAREN"),
                Arguments.of("print 1 +<error>\n2", NEWLINE, "expression"),
                Arguments.of("print (<error>\n1)", NEWLINE, "expression"),
                Arguments.of("print a[<error>\n1]", NEWLINE, "expression"),
                Arguments.of("var f := func(<error>\nx) => x", NEWLINE, "IDENT"),
                Arguments.of("print a.1<error>..10", RANGE, "NEWLINE or SEMICOLON"),
                Arguments.of("print <error>.1.2", DOT, "expression"),
                Arguments.of("print [1]<error>[2]", LBRACKET, "NEWLINE or SEMICOLON"),
                Arguments.of("print (f)<error>(1)", LPAREN, "NEWLINE or SEMICOLON"),
                Arguments.of("f(x)<error>", EOF, "ASSIGN"),
                Arguments.of("print 1\r\nvar x := <error>end", KW_END, "expression"),
                Arguments.of("return <error>then", KW_THEN, "expression"),
                Arguments.of("for i in <error>loop print 1 end", KW_LOOP, "expression"),
                Arguments.of("for 1.. <error>loop print 1 end", KW_LOOP, "expression")
        );
    }

    @ParameterizedTest
    @MethodSource("invalidStreams")
    void rejectsMalformedTokenStreamsSafely(List<Token> tokens) {
        ParseException error = assertThrows(ParseException.class, () -> new Parser(tokens).parse());
        assertEquals("Invalid token stream", error.getErrorType());
        assertNotNull(error.getActualToken());
        assertNotNull(error.getExpected());
    }

    static Stream<Arguments> invalidStreams() {
        Token eof = new Token(EOF, "", null, 1, 1);
        Token ident = new Token(IDENT, "x", null, 1, 2);
        return Stream.of(
                Arguments.of((Object) null), Arguments.of(List.of()),
                Arguments.of(Arrays.asList((Token) null)),
                Arguments.of(List.of(ident)),
                Arguments.of(List.of(eof, ident)),
                Arguments.of(List.of(eof, eof)),
                Arguments.of(Arrays.asList(eof, null)),
                Arguments.of(List.of(new Token(null, "x", null, 1, 1), eof)),
                Arguments.of(List.of(new Token(IDENT, null, null, 1, 1), eof)),
                Arguments.of(List.of(new Token(IDENT, "x", null, 0, 1), eof)),
                Arguments.of(List.of(new Token(IDENT, "x", null, 1, 0), eof)),
                Arguments.of(Arrays.asList(ident, null, eof))
        );
    }

    @Test
    void snapshotsTheCallerListWithoutChangingTokens() {
        List<Token> tokens = new ArrayList<>(new Lexer("print 1").scanTokens());
        List<Token> original = new ArrayList<>(tokens);
        Parser parser = new Parser(tokens);
        tokens.clear();
        assertDoesNotThrow(parser::parse);
        assertEquals("1", original.get(1).getLexeme());
        assertEquals(1L, original.get(1).getValue());
    }

    @Test
    void parsesAgainFromTheBeginning() {
        Parser parser = new Parser(new Lexer("var x := 1; print x").scanTokens());
        assertDoesNotThrow(parser::parse);
        assertDoesNotThrow(parser::parse);
    }

    @Test
    void neverIgnoresTokensAfterEof() {
        Token eof = new Token(EOF, "", null, 2, 1);
        Token tail = new Token(KW_PRINT, "print", null, 2, 2);
        ParseException error = assertThrows(ParseException.class, () -> new Parser(List.of(eof, tail)).parse());
        assertSame(tail, error.getActualToken());
        assertEquals(2, error.getLine());
        assertEquals(2, error.getColumn());
    }

    @Test
    void handlesLongFlatSequencesIteratively() {
        String source = "var x;".repeat(5000) + "print " + "1 + ".repeat(5000) + "1";
        assertTimeoutPreemptively(Duration.ofSeconds(10),
                () -> new Parser(new Lexer(source).scanTokens()).parse());
    }
}
