public final class LexicalException extends RuntimeException {
    public LexicalException(int line, int column, String message) {
        super("Lexical error at " + line + ":" + column + ": " + message);
    }
}
