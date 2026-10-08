package io.github.alextoday111.projectd.parser;

import java.util.ArrayList;
import java.util.List;

import io.github.alextoday111.projectd.lexer.Token;
import io.github.alextoday111.projectd.lexer.TokenType;

import static io.github.alextoday111.projectd.lexer.TokenType.*;

/** Validates syntax only: no tree construction, name resolution, or evaluation. */
public final class Parser {
    private final List<Token> tokens;
    private int current;

    /** Takes a snapshot and rejects malformed streams with ParseException. */
    public Parser(List<Token> tokens) {
        Token fallback = new Token(EOF, "", null, 1, 1);
        if (tokens == null || tokens.isEmpty()) {
            throw invalidStream("a nonempty token stream ending in explicit EOF", fallback);
        }
        List<Token> snapshot = new ArrayList<>(tokens);
        for (int i = 0; i < snapshot.size(); i++) {
            Token token = snapshot.get(i);
            if (token == null || token.getType() == null || token.getLexeme() == null
                    || token.getLine() < 1 || token.getColumn() < 1) {
                throw invalidStream("a nonnull token with type, lexeme and positive source position", fallback);
            }
            fallback = token;
            if (token.getType() == EOF && i != snapshot.size() - 1) {
                Token next = snapshot.get(i + 1);
                throw invalidStream("end of token stream immediately after EOF", next == null ? token : next);
            }
        }
        if (snapshot.get(snapshot.size() - 1).getType() != EOF) {
            throw invalidStream("explicit EOF token at end of token stream", fallback);
        }
        this.tokens = List.copyOf(snapshot);
    }

    /** Parses from the beginning on each call and requires complete input. */
    public void parse() {
        current = 0;
        skipSeparators();
        consume(EOF, "EOF");
    }

    private void skipSeparators() {
        while (match(NEWLINE, SEMICOLON)) {
            // Separator repetition is iterative, including blank lines.
        }
    }

    private Token peek() {
        return lookahead(0);
    }

    private Token lookahead(int offset) {
        return tokens.get(Math.min(current + offset, tokens.size() - 1));
    }

    private Token previous() {
        return tokens.get(Math.max(0, current - 1));
    }

    private Token advance() {
        if (current < tokens.size()) {
            current++;
        }
        return previous();
    }

    private boolean check(TokenType type) {
        return peek().getType() == type;
    }

    private boolean checkAny(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                return true;
            }
        }
        return false;
    }

    private boolean match(TokenType... types) {
        if (checkAny(types)) {
            advance();
            return true;
        }
        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) {
            return advance();
        }
        throw error(message);
    }

    private boolean isAtEnd() {
        return check(EOF);
    }

    private ParseException error(String expected) {
        return new ParseException("Syntax error", expected, peek());
    }

    private static ParseException invalidStream(String expected, Token actual) {
        return new ParseException("Invalid token stream", expected, actual);
    }
}
