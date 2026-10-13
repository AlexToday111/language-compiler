package io.github.alextoday111.projectd.ast;

import java.util.List;
import java.util.Objects;

public final class ArrayIndex implements Reference {
    private final Reference target;
    private final Expression index;

    public ArrayIndex(Reference target, Expression index) {
        this.target = Objects.requireNonNull(target);
        this.index = Objects.requireNonNull(index);
    }

    public Reference getTarget() {
        return target;
    }

    public Expression getIndex() {
        return index;
    }

    @Override
    public List<? extends AstNode> getChildren() {
        return List.of(target, index);
    }
}
