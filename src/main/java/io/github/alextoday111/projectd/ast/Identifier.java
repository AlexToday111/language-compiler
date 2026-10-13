package io.github.alextoday111.projectd.ast;

import java.util.List;
import java.util.Objects;

public final class Identifier implements Reference {
    private final String name;

    public Identifier(String name) {
        this.name = Objects.requireNonNull(name);
    }

    public String getName() {
        return name;
    }
}
