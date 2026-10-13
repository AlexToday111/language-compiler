package io.github.alextoday111.projectd.ast;

import java.util.List;
import java.util.Objects;

public final class TupleFieldAccess implements Reference {
    private final Reference target;
    private final TupleField field;

    public TupleFieldAccess(Reference target, TupleField field) {
        this.target = Objects.requireNonNull(target);
        this.field = Objects.requireNonNull(field);
    }

    public Reference getTarget() {
        return target;
    }

    public TupleField getField() {
        return field;
    }

    @Override
    public List<? extends AstNode> getChildren() {
        return List.of(target, field);
    }
}
