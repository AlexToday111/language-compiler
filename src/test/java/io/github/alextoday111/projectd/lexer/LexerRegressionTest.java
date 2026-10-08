package io.github.alextoday111.projectd.lexer;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

final class LexerRegressionTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("tokenSequences")
    void emitsExactTokenSequence(String source, String expected) {
        TokenType[] types = Arrays.stream((expected + " EOF").split(" "))
                .map(TokenType::valueOf).toArray(TokenType[]::new);
        TokenType[] actual = new Lexer(source).scanTokens().stream()
                .map(Token::getType).toArray(TokenType[]::new);
        assertArrayEquals(types, actual);
    }

    static Stream<Arguments> tokenSequences() {
        return Stream.of(
                Arguments.of("a.1", "IDENT DOT INTEGER"),
                Arguments.of("a.1.2", "IDENT DOT INTEGER DOT INTEGER"),
                Arguments.of("a.1.2.3", "IDENT DOT INTEGER DOT INTEGER DOT INTEGER"),
                Arguments.of("a.b.2", "IDENT DOT IDENT DOT INTEGER"),
                Arguments.of("a[1].2", "IDENT LBRACKET INTEGER RBRACKET DOT INTEGER"),
                Arguments.of("3.14", "REAL"),
                Arguments.of("12.345", "REAL"),
                Arguments.of("a.1 + 2.5", "IDENT DOT INTEGER PLUS REAL"),
                Arguments.of("a.1.2 + 3.14", "IDENT DOT INTEGER DOT INTEGER PLUS REAL"),
                Arguments.of("1..10", "INTEGER RANGE INTEGER"),
                Arguments.of("a.1..10", "IDENT DOT INTEGER RANGE INTEGER"),
                Arguments.of("a.b", "IDENT DOT IDENT"),
                Arguments.of("a.1[2]", "IDENT DOT INTEGER LBRACKET INTEGER RBRACKET"),
                Arguments.of("a[1].2[3].name.4", "IDENT LBRACKET INTEGER RBRACKET DOT INTEGER LBRACKET INTEGER RBRACKET DOT IDENT DOT INTEGER"),
                Arguments.of("f(x).1.2", "IDENT LPAREN IDENT RPAREN DOT INTEGER DOT INTEGER"),
                Arguments.of("a.12.345", "IDENT DOT INTEGER DOT INTEGER"),
                Arguments.of("a . 1 . 2", "IDENT DOT INTEGER DOT INTEGER"),
                Arguments.of("a.1\n2.5", "IDENT DOT INTEGER NEWLINE REAL"),
                Arguments.of("a. // comment\n1.2", "IDENT DOT NEWLINE REAL"),
                Arguments.of("a 3.14", "IDENT REAL"),
                Arguments.of("a[1.2]", "IDENT LBRACKET REAL RBRACKET"),
                Arguments.of("a.1[2.5]", "IDENT DOT INTEGER LBRACKET REAL RBRACKET"),
                Arguments.of("a.1..10.5", "IDENT DOT INTEGER RANGE REAL"),
                Arguments.of("1.2..3.4", "REAL RANGE REAL"),
                Arguments.of("1...2", "INTEGER RANGE DOT INTEGER"),
                Arguments.of("a.1; 2.5", "IDENT DOT INTEGER SEMICOLON REAL"),
                Arguments.of("42", "INTEGER"),
                Arguments.of("1.25 + 2.5 * 3.75", "REAL PLUS REAL STAR REAL"),
                Arguments.of("[1.25, 2.5]", "LBRACKET REAL COMMA REAL RBRACKET"),
                Arguments.of("{value := 1.25, 2.5}", "LBRACE IDENT ASSIGN REAL COMMA REAL RBRACE"),
                Arguments.of("[[1, 2], [3, 4]]", "LBRACKET LBRACKET INTEGER COMMA INTEGER RBRACKET COMMA LBRACKET INTEGER COMMA INTEGER RBRACKET RBRACKET"),
                Arguments.of("{{1, {2, 3}}}", "LBRACE LBRACE INTEGER COMMA LBRACE INTEGER COMMA INTEGER RBRACE RBRACE RBRACE"),
                Arguments.of("{rows := [[1], [2]]}", "LBRACE IDENT ASSIGN LBRACKET LBRACKET INTEGER RBRACKET COMMA LBRACKET INTEGER RBRACKET RBRACKET RBRACE"),
                Arguments.of("[{x := 1}, {x := 2}]", "LBRACKET LBRACE IDENT ASSIGN INTEGER RBRACE COMMA LBRACE IDENT ASSIGN INTEGER RBRACE RBRACKET"),
                Arguments.of("{items := [{nested := {1, [2.5]}}]}", "LBRACE IDENT ASSIGN LBRACKET LBRACE IDENT ASSIGN LBRACE INTEGER COMMA LBRACKET REAL RBRACKET RBRACE RBRACE RBRACKET RBRACE"),
                Arguments.of("{add := func(x) => x + 1}", "LBRACE IDENT ASSIGN KW_FUNC LPAREN IDENT RPAREN FAT_ARROW IDENT PLUS INTEGER RBRACE"),
                Arguments.of("func(row) => row[1] + row[2]", "KW_FUNC LPAREN IDENT RPAREN FAT_ARROW IDENT LBRACKET INTEGER RBRACKET PLUS IDENT LBRACKET INTEGER RBRACKET"),
                Arguments.of("func(config) => config.offset", "KW_FUNC LPAREN IDENT RPAREN FAT_ARROW IDENT DOT IDENT"),
                Arguments.of("func(f, x) => f(f(x))", "KW_FUNC LPAREN IDENT COMMA IDENT RPAREN FAT_ARROW IDENT LPAREN IDENT LPAREN IDENT RPAREN RPAREN"),
                Arguments.of("func(f, array, config) => f(array) + config.offset", "KW_FUNC LPAREN IDENT COMMA IDENT COMMA IDENT RPAREN FAT_ARROW IDENT LPAREN IDENT RPAREN PLUS IDENT DOT IDENT"),
                Arguments.of("transform(rowSum, config.values, config)", "IDENT LPAREN IDENT COMMA IDENT DOT IDENT COMMA IDENT RPAREN"),
                Arguments.of("func(options) is return options.increment end", "KW_FUNC LPAREN IDENT RPAREN KW_IS KW_RETURN IDENT DOT IDENT KW_END")
        );
    }

    @Test
    void preservesTupleChainLexemesValuesAndPositions() {
        List<Token> tokens = new Lexer("a.1.2.3").scanTokens();
        TokenType[] types = {TokenType.IDENT, TokenType.DOT, TokenType.INTEGER,
                TokenType.DOT, TokenType.INTEGER, TokenType.DOT, TokenType.INTEGER, TokenType.EOF};
        String[] lexemes = {"a", ".", "1", ".", "2", ".", "3", ""};
        Object[] values = {null, null, 1L, null, 2L, null, 3L, null};
        assertEquals(8, tokens.size());
        for (int i = 0; i < tokens.size(); i++) {
            assertToken(tokens.get(i), types[i], lexemes[i], values[i], 1, i + 1);
        }
    }

    @Test
    void preservesNumericValuesAndLexemesAcrossContexts() {
        List<Token> tokens = new Lexer("a.12.345 + 2.50; 42; [3.14]").scanTokens();
        assertToken(tokens.get(2), TokenType.INTEGER, "12", 12L, 1, 3);
        assertToken(tokens.get(4), TokenType.INTEGER, "345", 345L, 1, 6);
        assertToken(tokens.get(6), TokenType.REAL, "2.50", 2.5, 1, 12);
        assertToken(tokens.get(8), TokenType.INTEGER, "42", 42L, 1, 18);
        assertToken(tokens.get(11), TokenType.REAL, "3.14", 3.14, 1, 23);
        assertInstanceOf(Long.class, tokens.get(2).getValue());
        assertInstanceOf(Double.class, tokens.get(6).getValue());
    }

    @Test
    void preservesPositionsAcrossCrLfAndMixedAccess() {
        List<Token> tokens = new Lexer("a.1.2.3\r\nb[2].1 + 3.14").scanTokens();
        assertEquals(17, tokens.size());
        assertToken(tokens.get(7), TokenType.NEWLINE, "\r\n", null, 1, 8);
        assertToken(tokens.get(8), TokenType.IDENT, "b", null, 2, 1);
        assertToken(tokens.get(10), TokenType.INTEGER, "2", 2L, 2, 3);
        assertToken(tokens.get(13), TokenType.INTEGER, "1", 1L, 2, 6);
        assertToken(tokens.get(15), TokenType.REAL, "3.14", 3.14, 2, 10);
        assertToken(tokens.get(16), TokenType.EOF, "", null, 2, 14);
    }

    @Test
    void preservesRangePositionsAfterTupleAccess() {
        List<Token> tokens = new Lexer("a.1..10").scanTokens();
        assertEquals(6, tokens.size());
        assertToken(tokens.get(2), TokenType.INTEGER, "1", 1L, 1, 3);
        assertToken(tokens.get(3), TokenType.RANGE, "..", null, 1, 4);
        assertToken(tokens.get(4), TokenType.INTEGER, "10", 10L, 1, 6);
        assertToken(tokens.get(5), TokenType.EOF, "", null, 1, 8);
    }

    private static void assertToken(Token token, TokenType type, String lexeme,
                                    Object value, int line, int column) {
        assertEquals(type, token.getType());
        assertEquals(lexeme, token.getLexeme());
        if (value == null) {
            assertNull(token.getValue());
        } else {
            assertEquals(value, token.getValue());
        }
        assertEquals(line, token.getLine());
        assertEquals(column, token.getColumn());
    }
}
