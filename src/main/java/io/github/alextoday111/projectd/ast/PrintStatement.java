package io.github.alextoday111.projectd.ast;

import java.util.List;
import java.util.Objects;

public final class PrintStatement implements Statement {
    private final List<Expression> expressions;

    public PrintStatement(List<Expression> expressions) {
        this.expressions = List.copyOf(expressions);
    }

    public List<Expression> getExpressions() {
        return expressions;
    }

    @Override
    public List<? extends AstNode> getChildren() {
        return expressions;
    }
}
