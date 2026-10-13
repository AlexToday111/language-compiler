package io.github.alextoday111.projectd.ast;

import java.util.List;
import java.util.Objects;

public final class ArrayLiteral implements Expression {
    private final List<Expression> elements;

    public ArrayLiteral(List<Expression> elements) {
        this.elements = List.copyOf(elements);
    }

    public List<Expression> getElements() {
        return elements;
    }

    @Override
    public List<? extends AstNode> getChildren() {
        return elements;
    }
}
