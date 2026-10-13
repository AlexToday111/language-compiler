package io.github.alextoday111.projectd.ast;

import java.util.List;
import java.util.Objects;

public final class BinaryExpression implements Expression {
    private final String operator;
    private final Expression left;
    private final Expression right;

    public BinaryExpression(String operator, Expression left, Expression right) {
        this.operator = Objects.requireNonNull(operator);
        this.left = Objects.requireNonNull(left);
        this.right = Objects.requireNonNull(right);
    }

    public String getOperator() {
        return operator;
    }

    public Expression getLeft() {
        return left;
    }

    public Expression getRight() {
        return right;
    }

    @Override
    public List<? extends AstNode> getChildren() {
        return List.of(left, right);
    }
}
