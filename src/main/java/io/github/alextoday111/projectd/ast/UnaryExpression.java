package io.github.alextoday111.projectd.ast;

import java.util.List;
import java.util.Objects;

public final class UnaryExpression implements Expression {
    private final String operator;
    private final Expression operand;

    public UnaryExpression(String operator, Expression operand) {
        this.operator = Objects.requireNonNull(operator);
        this.operand = Objects.requireNonNull(operand);
    }

    public String getOperator() {
        return operator;
    }

    public Expression getOperand() {
        return operand;
    }

    @Override
    public List<? extends AstNode> getChildren() {
        return List.of(operand);
    }
}
