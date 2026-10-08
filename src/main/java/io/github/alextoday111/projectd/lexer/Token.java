package io.github.alextoday111.projectd.lexer;

public final class Token {
    private final TokenType type;
    private final String lexeme;
    private final Object value;
    private final int line;
    private final int column;

    public Token(TokenType type, String lexeme, Object value, int line, int column) {
        this.type = type;
        this.lexeme = lexeme;
        this.value = value;
        this.line = line;
        this.column = column;
    }

    public TokenType getType() {
        return type;
    }

    public String getLexeme() {
        return lexeme;
    }

    public Object getValue() {
        return value;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public String valueForDisplay() {
        return value == null ? "" : String.valueOf(value);
    }

    public String lexemeForDisplay() {
        return lexeme
                .replace("\\", "\\\\")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }
}
