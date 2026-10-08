import java.util.List;

public final class LexerTest {
    private static int checks;

    public static void main(String[] args) {
        expectTypes(
                "var total := 12.5",
                TokenType.KW_VAR,
                TokenType.IDENT,
                TokenType.ASSIGN,
                TokenType.REAL,
                TokenType.EOF
        );

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

        expectTypes(
                "[\"lexer\", 'demo']",
                TokenType.LBRACKET,
                TokenType.STRING,
                TokenType.COMMA,
                TokenType.STRING,
                TokenType.RBRACKET,
                TokenType.EOF
        );

        expectLexicalError("var x := @");
        expectLexicalError("print \"unfinished");

        System.out.println("All lexer tests passed: " + checks);
    }

    private static void expectTypes(String source, TokenType... expected) {
        List<Token> actual = new Lexer(source).scanTokens();
        if (actual.size() != expected.length) {
            throw new AssertionError("Expected " + expected.length + " tokens but got " + actual.size());
        }
        for (int i = 0; i < expected.length; i++) {
            if (actual.get(i).getType() != expected[i]) {
                throw new AssertionError(
                        "Token " + i + ": expected " + expected[i] + " but got " + actual.get(i).getType()
                );
            }
            checks++;
        }
    }

    private static void expectLexicalError(String source) {
        try {
            new Lexer(source).scanTokens();
            throw new AssertionError("Expected a lexical error for: " + source);
        } catch (LexicalException expected) {
            checks++;
        }
    }
}
