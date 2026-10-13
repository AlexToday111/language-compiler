package io.github.alextoday111.projectd.parser;

import java.util.ArrayList;
import java.util.List;

import io.github.alextoday111.projectd.ast.*;

import io.github.alextoday111.projectd.lexer.Token;
import io.github.alextoday111.projectd.lexer.TokenType;

import static io.github.alextoday111.projectd.lexer.TokenType.*;


public final class Parser {
    private final List<Token> tokens;
    private int current;


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

    public Program parse() {
        current = 0;
        Block body = parseStatementSequence(false, EOF);
        consume(EOF, "EOF");
        return new Program(body.getStatements());
    }

    private Block parseStatementSequence(boolean nonempty, TokenType... endings) {
        List<Statement> statements = new ArrayList<>();
        skipSeparators();
        if (isAtEnd() || checkAny(endings)) {
            if (nonempty) {
                throw error("statement (nonempty body)");
            }
            return new Block(statements);
        }
        statements.add(parseStatement());
        while (!isAtEnd() && !checkAny(endings)) {
            if (!match(NEWLINE, SEMICOLON)) {
                throw error("NEWLINE or SEMICOLON between statements");
            }
            skipSeparators();
            if (isAtEnd() || checkAny(endings)) {
                break;
            }
            statements.add(parseStatement());
        }
        return new Block(statements);
    }

    private Statement parseStatement() {
        switch (peek().getType()) {
            case KW_VAR:
                advance();
                List<VariableDeclaration> declarations = new ArrayList<>();
                declarations.add(parseVariableDefinition());
                while (match(COMMA)) {
                    declarations.add(parseVariableDefinition());
                }
                return declarations.size() == 1 ? declarations.get(0)
                        : new VariableDeclarationGroup(declarations);
            case IDENT:
                Reference target = parseReference();
                consume(ASSIGN, "ASSIGN (:=)");
                return new Assignment(target, parseExpression());
            case KW_IF:
                advance();
                return parseIf();
            case KW_WHILE:
                advance();
                Expression condition = parseExpression();
                return new WhileLoop(condition, parseLoopBody());
            case KW_FOR:
                advance();
                Identifier variable = null;
                if (check(IDENT) && lookahead(1).getType() == KW_IN) {
                    variable = new Identifier(advance().getLexeme());
                    advance();
                }
                Expression iterable = parseExpression();
                Expression rangeEnd = match(RANGE) ? parseExpression() : null;
                return new ForLoop(variable, iterable, rangeEnd, parseLoopBody());
            case KW_LOOP:
                return new InfiniteLoop(parseLoopBody());
            case KW_EXIT:
                advance();
                return new ExitStatement();
            case KW_RETURN:
                advance();
                Expression value = checkAny(NEWLINE, SEMICOLON, KW_ELSE, KW_END, EOF)
                        ? null : parseExpression();
                return new ReturnStatement(value);
            case KW_PRINT:
                advance();
                List<Expression> expressions = new ArrayList<>();
                expressions.add(parseExpression());
                while (match(COMMA)) {
                    expressions.add(parseExpression());
                }
                return new PrintStatement(expressions);
            default:
                throw error("statement");
        }
    }

    private VariableDeclaration parseVariableDefinition() {
        Identifier identifier = new Identifier(consume(IDENT, "IDENT (variable name)").getLexeme());
        Expression initializer = match(ASSIGN) ? parseExpression() : null;
        return new VariableDeclaration(identifier, initializer);
    }

    private IfStatement parseIf() {
        Expression condition = parseExpression();
        if (match(FAT_ARROW)) {
            return new IfStatement(condition, new Block(List.of(parseStatement())), null, true);
        }
        consume(KW_THEN, "KW_THEN or FAT_ARROW");
        Block thenBranch = parseStatementSequence(true, KW_ELSE, KW_END);
        Else elseBranch = match(KW_ELSE) ? new Else(parseStatementSequence(true, KW_END)) : null;
        consume(KW_END, "KW_END");
        return new IfStatement(condition, thenBranch, elseBranch, false);
    }

    private Block parseLoopBody() {
        consume(KW_LOOP, "KW_LOOP");
        Block body = parseStatementSequence(true, KW_END);
        consume(KW_END, "KW_END");
        return body;
    }

    private Expression parseExpression() {
        Expression expression = parseRelation();
        while (match(KW_OR, KW_AND, KW_XOR)) {
            String operator = previous().getLexeme();
            expression = new BinaryExpression(operator, expression, parseRelation());
        }
        return expression;
    }

    private Expression parseRelation() {
        Expression expression = parseAdditive();
        if (match(LESS, LESS_EQUAL, GREATER, GREATER_EQUAL, EQUAL, NOT_EQUAL)) {
            String operator = previous().getLexeme();
            expression = new BinaryExpression(operator, expression, parseAdditive());
            if (checkAny(LESS, LESS_EQUAL, GREATER, GREATER_EQUAL, EQUAL, NOT_EQUAL)) {
                throw error("at most one comparison operator per relation");
            }
        }
        return expression;
    }

    private Expression parseAdditive() {
        Expression expression = parseMultiplicative();
        while (match(PLUS, MINUS)) {
            String operator = previous().getLexeme();
            expression = new BinaryExpression(operator, expression, parseMultiplicative());
        }
        return expression;
    }

    private Expression parseMultiplicative() {
        Expression expression = parseUnary();
        while (match(STAR, SLASH)) {
            String operator = previous().getLexeme();
            expression = new BinaryExpression(operator, expression, parseUnary());
        }
        return expression;
    }

    private Expression parseUnary() {
        if (check(IDENT)) {
            Reference reference = parseReference();
            return match(KW_IS) ? new TypeCheckExpression(reference, parseTypeIndicator()) : reference;
        }
        if (match(PLUS, MINUS, KW_NOT)) {
            String operator = previous().getLexeme();
            return new UnaryExpression(operator, parsePrimary());
        }
        return parsePrimary();
    }

    private Expression parsePrimary() {
        if (match(INTEGER, REAL, STRING, KW_TRUE, KW_FALSE, KW_NONE)) {
            Token token = previous();
            switch (token.getType()) {
                case INTEGER: return new IntegerLiteral((Long) token.getValue(), token.getLexeme());
                case REAL: return new RealLiteral((Double) token.getValue(), token.getLexeme());
                case STRING: return new StringLiteral((String) token.getValue(), token.getLexeme());
                case KW_TRUE: return new BooleanLiteral(true, token.getLexeme());
                case KW_FALSE: return new BooleanLiteral(false, token.getLexeme());
                case KW_NONE: return new NoneLiteral();
                default: throw new IllegalStateException("Unexpected literal token");
            }
        }
        if (match(LPAREN)) {
            Expression expression = parseExpression();
            consume(RPAREN, "RPAREN");
            return expression;
        }
        if (check(KW_FUNC)) {
            return parseFunctionLiteral();
        }
        if (check(LBRACKET)) {
            return parseArray();
        }
        if (check(LBRACE)) {
            return parseTuple();
        }
        throw error("expression (literal, function, collection or parenthesized expression)");
    }

    private FunctionLiteral parseFunctionLiteral() {
        consume(KW_FUNC, "KW_FUNC");
        List<Identifier> parameters = new ArrayList<>();
        if (match(LPAREN)) {
            parameters.add(new Identifier(consume(IDENT, "IDENT (nonempty parameter list)").getLexeme()));
            while (match(COMMA)) {
                parameters.add(new Identifier(consume(IDENT, "IDENT (parameter name)").getLexeme()));
            }
            consume(RPAREN, "RPAREN");
        }
        if (match(KW_IS)) {
            Block body = parseStatementSequence(true, KW_END);
            consume(KW_END, "KW_END");
            return new FunctionLiteral(new Parameters(parameters), body);
        }
        consume(FAT_ARROW, "KW_IS or FAT_ARROW (function body)");
        return new FunctionLiteral(new Parameters(parameters), parseExpression());
    }

    private ArrayLiteral parseArray() {
        consume(LBRACKET, "LBRACKET");
        List<Expression> elements = new ArrayList<>();
        skipLineBreaks();
        if (match(RBRACKET)) {
            return new ArrayLiteral(elements);
        }
        elements.add(parseExpression());
        while (true) {
            skipLineBreaks();
            if (!match(COMMA)) {
                break;
            }
            skipLineBreaks();
            elements.add(parseExpression());
        }
        consume(RBRACKET, "COMMA or RBRACKET");
        return new ArrayLiteral(elements);
    }

    private TupleLiteral parseTuple() {
        consume(LBRACE, "LBRACE");
        skipLineBreaks();
        List<TupleElement> elements = new ArrayList<>();
        elements.add(parseTupleElement());
        while (true) {
            skipLineBreaks();
            if (!match(COMMA)) {
                break;
            }
            skipLineBreaks();
            elements.add(parseTupleElement());
        }
        consume(RBRACE, "COMMA or RBRACE");
        return new TupleLiteral(elements);
    }

    private TupleElement parseTupleElement() {
        Identifier name = null;
        if (check(IDENT) && lookahead(1).getType() == ASSIGN) {
            name = new Identifier(advance().getLexeme());
            advance();
        }
        return new TupleElement(name, parseExpression());
    }

    private void skipLineBreaks() {
        while (match(NEWLINE)) {
        }
    }

    private TypeIndicator parseTypeIndicator() {
        if (match(KW_INT, KW_REAL, KW_BOOL, KW_STRING, KW_NONE, KW_FUNC)) {
            return TypeIndicator.valueOf(previous().getType().name().substring(3));
        }
        if (match(LBRACKET)) {
            consume(RBRACKET, "RBRACKET in array type indicator");
            return TypeIndicator.ARRAY;
        }
        if (match(LBRACE)) {
            consume(RBRACE, "RBRACE in tuple type indicator");
            return TypeIndicator.TUPLE;
        }
        throw error("type indicator (int, real, bool, string, none, [], {}, func)");
    }

    private Reference parseReference() {
        Reference reference = new Identifier(consume(IDENT, "IDENT (reference root)").getLexeme());
        while (true) {
            if (match(LBRACKET)) {
                Expression index = parseExpression();
                consume(RBRACKET, "RBRACKET");
                reference = new ArrayIndex(reference, index);
            } else if (match(LPAREN)) {
                List<Expression> arguments = new ArrayList<>();
                arguments.add(parseExpression());
                while (match(COMMA)) {
                    arguments.add(parseExpression());
                }
                consume(RPAREN, "RPAREN");
                reference = new FunctionCall(reference, arguments);
            } else if (match(DOT)) {
                if (!match(IDENT, INTEGER)) {
                    throw error("IDENT or INTEGER after DOT");
                }
                Token field = previous();
                reference = new TupleFieldAccess(reference, field.getType() == IDENT
                        ? new TupleField(field.getLexeme()) : new TupleField((Long) field.getValue()));
            } else {
                return reference;
            }
        }
    }

    private void skipSeparators() {
        while (match(NEWLINE, SEMICOLON)) {

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
