package io.github.alextoday111.projectd.lexer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class LexerTest {
    @Test
    void tokenizesVariableDeclaration() {
        expectTypes(
                "var total := 12.5",
                TokenType.KW_VAR,
                TokenType.IDENT,
                TokenType.ASSIGN,
                TokenType.REAL,
                TokenType.EOF
        );
    }

    @Test
    void tokenizesFunctionLiteral() {
        expectTypes(
                "func(a, b) => a + b",
                TokenType.KW_FUNC,
                TokenType.LPAREN,
                TokenType.IDENT,
                TokenType.COMMA,
                TokenType.IDENT,
                TokenType.RPAREN,
                TokenType.FAT_ARROW,
                TokenType.IDENT,
                TokenType.PLUS,
                TokenType.IDENT,
                TokenType.EOF
        );
    }

    @Test
    void tokenizesRangeLoopAndComment() {
        expectTypes(
                "for i in 1..3 loop // comment\nprint i\nend",
                TokenType.KW_FOR,
                TokenType.IDENT,
                TokenType.KW_IN,
                TokenType.INTEGER,
                TokenType.RANGE,
                TokenType.INTEGER,
                TokenType.KW_LOOP,
                TokenType.NEWLINE,
                TokenType.KW_PRINT,
                TokenType.IDENT,
                TokenType.NEWLINE,
                TokenType.KW_END,
                TokenType.EOF
        );
    }

    @Test
    void tokenizesComparisonAndLogic() {
        expectTypes(
                "x <= 10 and x /= 5",
                TokenType.IDENT,
                TokenType.LESS_EQUAL,
                TokenType.INTEGER,
                TokenType.KW_AND,
                TokenType.IDENT,
                TokenType.NOT_EQUAL,
                TokenType.INTEGER,
                TokenType.EOF
        );
    }

    @Test
    void tokenizesQuotedStrings() {
        expectTypes(
                "[\"lexer\", 'demo']",
                TokenType.LBRACKET,
                TokenType.STRING,
                TokenType.COMMA,
                TokenType.STRING,
                TokenType.RBRACKET,
                TokenType.EOF
        );
    }

    @Test
    void rejectsUnknownCharacter() {
        assertThrows(LexicalException.class, () -> new Lexer("var x := @").scanTokens());
    }

    @Test
    void rejectsUnterminatedString() {
        assertThrows(LexicalException.class, () -> new Lexer("print \"unfinished").scanTokens());
    }

    private static void expectTypes(String source, TokenType... expected) {
        TokenType[] actual = new Lexer(source).scanTokens().stream()
                .map(Token::getType)
                .toArray(TokenType[]::new);
        assertArrayEquals(expected, actual);
    }
}
