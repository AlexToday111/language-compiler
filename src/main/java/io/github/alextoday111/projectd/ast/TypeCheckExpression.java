package io.github.alextoday111.projectd.ast;

import java.util.List;
import java.util.Objects;

public final class TypeCheckExpression implements Expression {
    private final Reference reference;
    private final TypeIndicator type;

    public TypeCheckExpression(Reference reference, TypeIndicator type) {
        this.reference = Objects.requireNonNull(reference);
        this.type = Objects.requireNonNull(type);
    }

    public Reference getReference() {
        return reference;
    }

    public TypeIndicator getType() {
        return type;
    }

    @Override
    public List<? extends AstNode> getChildren() {
        return List.of(reference, type);
    }
}
