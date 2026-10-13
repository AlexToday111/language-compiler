package io.github.alextoday111.projectd.ast;

import java.util.List;
import java.util.Objects;

public final class ForLoop implements Statement {
    private final Identifier variable;
    private final Expression iterable;
    private final Expression rangeEnd;
    private final Block body;

    public ForLoop(Identifier variable, Expression iterable, Expression rangeEnd, Block body) {
        this.variable = variable;
        this.iterable = Objects.requireNonNull(iterable);
        this.rangeEnd = rangeEnd;
        this.body = Objects.requireNonNull(body);
    }

    public Identifier getVariable() {
        return variable;
    }

    public Expression getIterable() {
        return iterable;
    }

    public Expression getRangeEnd() {
        return rangeEnd;
    }

    public Block getBody() {
        return body;
    }

    @Override
    public List<? extends AstNode> getChildren() {
        return java.util.stream.Stream.of(variable, iterable, rangeEnd, body).filter(Objects::nonNull).collect(java.util.stream.Collectors.toUnmodifiableList());
    }
}
