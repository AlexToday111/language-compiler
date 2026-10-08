package io.github.alextoday111.projectd.parser;

import io.github.alextoday111.projectd.lexer.Token;

/** A fail-fast syntax or token-stream error at the token where it was detected. */
public final class ParseException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final String errorType;
    private final String expected;
    private final Token actualToken;

    ParseException(String errorType, String expected, Token actualToken) {
        super(errorType + " at " + actualToken.getLine() + ":" + actualToken.getColumn()
                + "\nExpected " + expected + ", found " + actualToken.getType());
        this.errorType = errorType;
        this.expected = expected;
        this.actualToken = actualToken;
    }

    public String getErrorType() {
        return errorType;
    }

    public String getExpected() {
        return expected;
    }

    public Token getActualToken() {
        return actualToken;
    }

    public int getLine() {
        return actualToken.getLine();
    }

    public int getColumn() {
        return actualToken.getColumn();
    }
}
