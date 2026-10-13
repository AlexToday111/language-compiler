package io.github.alextoday111.projectd.ast;

import java.util.List;
import java.util.Objects;

public final class FunctionLiteral implements Expression {
    private final Parameters parameters;
    private final Block body;
    private final Expression expressionBody;

    public FunctionLiteral(Parameters parameters, Block body) {
        this.parameters = Objects.requireNonNull(parameters);
        this.body = Objects.requireNonNull(body);
        this.expressionBody = null;
    }

    public FunctionLiteral(Parameters parameters, Expression expressionBody) {
        this.parameters = Objects.requireNonNull(parameters);
        this.expressionBody = Objects.requireNonNull(expressionBody);
        this.body = null;
    }

    public Parameters getParameters() { return parameters; }
    public Block getBody() { return body; }
    public Expression getExpressionBody() { return expressionBody; }

    @Override
    public List<? extends AstNode> getChildren() {
        return List.of(parameters, body == null ? expressionBody : body);
    }
}
