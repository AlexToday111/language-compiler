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
        parseStatementSequence(false, EOF);
        consume(EOF, "EOF");
    }

    private void parseStatementSequence(boolean nonempty, TokenType... endings) {
        skipSeparators();
        if (isAtEnd() || checkAny(endings)) {
            if (nonempty) {
                throw error("statement (nonempty body)");
            }
            return;
        }
        parseStatement();
        while (!isAtEnd() && !checkAny(endings)) {
            if (!match(NEWLINE, SEMICOLON)) {
                throw error("NEWLINE or SEMICOLON between statements");
            }
            skipSeparators();
            if (isAtEnd() || checkAny(endings)) {
                return;
            }
            parseStatement();
        }
    }

    private void parseStatement() {
        switch (peek().getType()) {
            case KW_VAR:
                advance();
                parseVariableDefinition();
                while (match(COMMA)) {
                    parseVariableDefinition();
                }
                return;
            case IDENT:
                parseReference();
                consume(ASSIGN, "ASSIGN (:=)");
                parseExpression();
                return;
            case KW_IF:
                advance();
                parseIf();
                return;
            case KW_WHILE:
                advance();
                parseExpression();
                parseLoopBody();
                return;
            case KW_FOR:
                advance();
                if (check(IDENT) && lookahead(1).getType() == KW_IN) {
                    advance();
                    advance();
                }
                parseExpression();
                if (match(RANGE)) {
                    parseExpression();
                }
                parseLoopBody();
                return;
            case KW_LOOP:
                parseLoopBody();
                return;
            case KW_EXIT:
                advance();
                return;
            case KW_RETURN:
                advance();
                if (!checkAny(NEWLINE, SEMICOLON, KW_ELSE, KW_END, EOF)) {
                    parseExpression();
                }
                return;
            case KW_PRINT:
                advance();
                parseExpression();
                while (match(COMMA)) {
                    parseExpression();
                }
                return;
            default:
                throw error("statement");
        }
    }

    private void parseVariableDefinition() {
        consume(IDENT, "IDENT (variable name)");
        if (match(ASSIGN)) {
            parseExpression();
        }
    }

    private void parseIf() {
        parseExpression();
        if (match(FAT_ARROW)) {
            // N2: exactly one statement, possibly an entire nested construct.
            parseStatement();
            return;
        }
        consume(KW_THEN, "KW_THEN or FAT_ARROW");
        parseStatementSequence(true, KW_ELSE, KW_END);
        if (match(KW_ELSE)) {
            parseStatementSequence(true, KW_END);
        }
        consume(KW_END, "KW_END");
    }

    private void parseLoopBody() {
        consume(KW_LOOP, "KW_LOOP");
        parseStatementSequence(true, KW_END);
        consume(KW_END, "KW_END");
    }

    private void parseExpression() {
        parseRelation();
        while (match(KW_OR, KW_AND, KW_XOR)) {
            parseRelation();
        }
    }

    private void parseRelation() {
        parseAdditive();
        if (match(LESS, LESS_EQUAL, GREATER, GREATER_EQUAL, EQUAL, NOT_EQUAL)) {
            parseAdditive();
            if (checkAny(LESS, LESS_EQUAL, GREATER, GREATER_EQUAL, EQUAL, NOT_EQUAL)) {
                throw error("at most one comparison operator per relation");
            }
        }
    }

    private void parseAdditive() {
        parseMultiplicative();
        while (match(PLUS, MINUS)) {
            parseMultiplicative();
        }
    }

    private void parseMultiplicative() {
        parseUnary();
        while (match(STAR, SLASH)) {
            parseUnary();
        }
    }

    private void parseUnary() {
        if (check(IDENT)) {
            parseReference();
            if (match(KW_IS)) {
                parseTypeIndicator();
            }
        } else {
            // N5: prefixes apply to Primary, not directly to Reference.
            match(PLUS, MINUS, KW_NOT);
            parsePrimary();
        }
    }

    private void parsePrimary() {
        if (match(INTEGER, REAL, STRING, KW_TRUE, KW_FALSE, KW_NONE)) {
            return;
        }
        if (match(LPAREN)) {
            parseExpression();
            consume(RPAREN, "RPAREN");
            return;
        }
        throw error("expression (literal, function, collection or parenthesized expression)");
    }

    private void parseTypeIndicator() {
        if (match(KW_INT, KW_REAL, KW_BOOL, KW_STRING, KW_NONE, KW_FUNC)) {
            return;
        }
        if (match(LBRACKET)) {
            consume(RBRACKET, "RBRACKET in array type indicator");
        } else if (match(LBRACE)) {
            consume(RBRACE, "RBRACE in tuple type indicator");
        } else {
            throw error("type indicator (int, real, bool, string, none, [], {}, func)");
        }
    }

    private void parseReference() {
        consume(IDENT, "IDENT (reference root)");
        while (true) {
            if (match(LBRACKET)) {
                parseExpression();
                consume(RBRACKET, "RBRACKET");
            } else if (match(LPAREN)) {
                parseExpression();
                while (match(COMMA)) {
                    parseExpression();
                }
                consume(RPAREN, "RPAREN");
            } else if (match(DOT)) {
                if (!match(IDENT, INTEGER)) {
                    throw error("IDENT or INTEGER after DOT");
                }
            } else {
                return;
            }
        }
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
