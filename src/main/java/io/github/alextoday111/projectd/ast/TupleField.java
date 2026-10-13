package io.github.alextoday111.projectd.ast;

import java.util.Objects;

public final class TupleField implements AstNode {
    private final String name;
    private final Long position;

    public TupleField(String name) {
        this.name = Objects.requireNonNull(name);
        this.position = null;
    }

    public TupleField(long position) {
        this.name = null;
        this.position = position;
    }

    public String getName() { return name; }
    public Long getPosition() { return position; }
}
