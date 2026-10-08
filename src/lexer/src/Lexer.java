import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Lexer {
    private static final Map<String, TokenType> KEYWORDS;

    static {
        Map<String, TokenType> keywords = new HashMap<>();
        keywords.put("var", TokenType.KW_VAR);
        keywords.put("if", TokenType.KW_IF);
        keywords.put("then", TokenType.KW_THEN);
        keywords.put("else", TokenType.KW_ELSE);
        keywords.put("end", TokenType.KW_END);
        keywords.put("while", TokenType.KW_WHILE);
        keywords.put("for", TokenType.KW_FOR);
        keywords.put("in", TokenType.KW_IN);
        keywords.put("loop", TokenType.KW_LOOP);
        keywords.put("exit", TokenType.KW_EXIT);
        keywords.put("return", TokenType.KW_RETURN);
        keywords.put("print", TokenType.KW_PRINT);
        keywords.put("func", TokenType.KW_FUNC);
        keywords.put("is", TokenType.KW_IS);
        keywords.put("int", TokenType.KW_INT);
        keywords.put("real", TokenType.KW_REAL);
        keywords.put("bool", TokenType.KW_BOOL);
        keywords.put("string", TokenType.KW_STRING);
        keywords.put("none", TokenType.KW_NONE);
        keywords.put("true", TokenType.KW_TRUE);
        keywords.put("false", TokenType.KW_FALSE);
        keywords.put("and", TokenType.KW_AND);
        keywords.put("or", TokenType.KW_OR);
        keywords.put("xor", TokenType.KW_XOR);
        keywords.put("not", TokenType.KW_NOT);
        KEYWORDS = Collections.unmodifiableMap(keywords);
    }

    private final String source;
    private final List<Token> tokens = new ArrayList<>();

    private int start;
    private int current;
    private int line = 1;
    private int column = 1;
    private int startLine;
    private int startColumn;

    public Lexer(String source) {
        this.source = source == null ? "" : source;
    }

    public List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            startLine = line;
            startColumn = column;
            scanToken();
        }

        tokens.add(new Token(TokenType.EOF, "", null, line, column));
        return Collections.unmodifiableList(tokens);
    }

    private void scanToken() {
        char c = advance();

        switch (c) {
            case ' ':
            case '\t':
            case '\f':
                return;
            case '\r':
                if (match('\n')) {
                    addToken(TokenType.NEWLINE);
                } else {
                    addToken(TokenType.NEWLINE);
                    line++;
                    column = 1;
                }
                return;
            case '\n':
                addToken(TokenType.NEWLINE);
                return;
            case '(':
                addToken(TokenType.LPAREN);
                return;
            case ')':
                addToken(TokenType.RPAREN);
                return;
            case '[':
                addToken(TokenType.LBRACKET);
                return;
            case ']':
                addToken(TokenType.RBRACKET);
                return;
            case '{':
                addToken(TokenType.LBRACE);
                return;
            case '}':
                addToken(TokenType.RBRACE);
                return;
            case ',':
                addToken(TokenType.COMMA);
                return;
            case ';':
                addToken(TokenType.SEMICOLON);
                return;
            case '+':
                addToken(TokenType.PLUS);
                return;
            case '-':
                addToken(TokenType.MINUS);
                return;
            case '*':
                addToken(TokenType.STAR);
                return;
            case ':':
                requireAndAdd('=', TokenType.ASSIGN, "expected '=' after ':'");
                return;
            case '=':
                addToken(match('>') ? TokenType.FAT_ARROW : TokenType.EQUAL);
                return;
            case '<':
                addToken(match('=') ? TokenType.LESS_EQUAL : TokenType.LESS);
                return;
            case '>':
                addToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER);
                return;
            case '.':
                addToken(match('.') ? TokenType.RANGE : TokenType.DOT);
                return;
            case '/':
                if (match('/')) {
                    skipLineComment();
                } else if (match('=')) {
                    addToken(TokenType.NOT_EQUAL);
                } else {
                    addToken(TokenType.SLASH);
                }
                return;
            case '\'':
            case '"':
                scanString(c);
                return;
            default:
                if (isDigit(c)) {
                    scanNumber();
                } else if (isIdentifierStart(c)) {
                    scanIdentifier();
                } else {
                    throw error("unexpected character '" + c + "'");
                }
        }
    }

    private void scanIdentifier() {
        while (isIdentifierPart(peek())) {
            advance();
        }

        String text = source.substring(start, current);
        TokenType type = KEYWORDS.getOrDefault(text, TokenType.IDENT);
        Object value = null;
        if (type == TokenType.KW_TRUE) {
            value = true;
        } else if (type == TokenType.KW_FALSE) {
            value = false;
        }
        addToken(type, value);
    }

    private void scanNumber() {
        while (isDigit(peek())) {
            advance();
        }

        boolean isReal = peek() == '.' && isDigit(peekNext());
        if (isReal) {
            advance();
            while (isDigit(peek())) {
                advance();
            }
        }

        String text = source.substring(start, current);
        try {
            if (isReal) {
                addToken(TokenType.REAL, Double.parseDouble(text));
            } else {
                addToken(TokenType.INTEGER, Long.parseLong(text));
            }
        } catch (NumberFormatException exception) {
            throw error("invalid numeric literal '" + text + "'");
        }
    }

    private void scanString(char quote) {
        StringBuilder value = new StringBuilder();

        while (!isAtEnd() && peek() != quote) {
            if (peek() == '\n' || peek() == '\r') {
                throw error("unterminated string literal");
            }

            char c = advance();
            if (c == '\\') {
                if (isAtEnd()) {
                    throw error("unterminated string literal");
                }
                char escaped = advance();
                switch (escaped) {
                    case 'n': value.append('\n'); break;
                    case 'r': value.append('\r'); break;
                    case 't': value.append('\t'); break;
                    case '\\': value.append('\\'); break;
                    case '\'': value.append('\''); break;
                    case '"': value.append('"'); break;
                    default: value.append(escaped); break;
                }
            } else {
                value.append(c);
            }
        }

        if (isAtEnd()) {
            throw error("unterminated string literal");
        }

        advance();
        addToken(TokenType.STRING, value.toString());
    }

    private void skipLineComment() {
        while (!isAtEnd() && peek() != '\n' && peek() != '\r') {
            advance();
        }
    }

    private void requireAndAdd(char expected, TokenType type, String message) {
        if (!match(expected)) {
            throw error(message);
        }
        addToken(type);
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object value) {
        String lexeme = source.substring(start, current);
        tokens.add(new Token(type, lexeme, value, startLine, startColumn));
    }

    private char advance() {
        char c = source.charAt(current++);
        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return c;
    }

    private boolean match(char expected) {
        if (isAtEnd() || source.charAt(current) != expected) {
            return false;
        }
        advance();
        return true;
    }

    private char peek() {
        return isAtEnd() ? '\0' : source.charAt(current);
    }

    private char peekNext() {
        return current + 1 >= source.length() ? '\0' : source.charAt(current + 1);
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private LexicalException error(String message) {
        return new LexicalException(startLine, startColumn, message);
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isIdentifierStart(char c) {
        return c == '_' || Character.isLetter(c);
    }

    private static boolean isIdentifierPart(char c) {
        return c == '_' || Character.isLetterOrDigit(c);
    }
}
