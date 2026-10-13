package io.github.alextoday111.projectd.ast;

import java.util.List;
import java.util.Objects;

public final class Assignment implements Statement {
    private final Reference target;
    private final Expression value;

    public Assignment(Reference target, Expression value) {
        this.target = Objects.requireNonNull(target);
        this.value = Objects.requireNonNull(value);
    }

    public Reference getTarget() {
        return target;
    }

    public Expression getValue() {
        return value;
    }

    @Override
    public List<? extends AstNode> getChildren() {
        return List.of(target, value);
    }
}
