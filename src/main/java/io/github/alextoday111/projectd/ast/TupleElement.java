package io.github.alextoday111.projectd.ast;

import java.util.List;
import java.util.Objects;

public final class TupleElement implements AstNode {
    private final Identifier name;
    private final Expression value;

    public TupleElement(Identifier name, Expression value) {
        this.name = name;
        this.value = Objects.requireNonNull(value);
    }

    public Identifier getName() {
        return name;
    }

    public Expression getValue() {
        return value;
    }

    @Override
    public List<? extends AstNode> getChildren() {
        return name == null ? List.of(value) : List.of(name, value);
    }
}
