package io.github.alextoday111.projectd.ast;

import java.util.List;
import java.util.Objects;

public final class ReturnStatement implements Statement {
    private final Expression value;

    public ReturnStatement(Expression value) {
        this.value = value;
    }

    public Expression getValue() {
        return value;
    }

    @Override
    public List<? extends AstNode> getChildren() {
        return value == null ? List.of() : List.of(value);
    }
}
