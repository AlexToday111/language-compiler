package io.github.alextoday111.projectd.ast;

import java.util.List;
import java.util.Objects;

public final class FunctionCall implements Reference {
    private final Reference callee;
    private final List<Expression> arguments;

    public FunctionCall(Reference callee, List<Expression> arguments) {
        this.callee = Objects.requireNonNull(callee);
        this.arguments = List.copyOf(arguments);
    }

    public Reference getCallee() {
        return callee;
    }

    public List<Expression> getArguments() {
        return arguments;
    }

    @Override
    public List<? extends AstNode> getChildren() {
        return java.util.stream.Stream.concat(java.util.stream.Stream.of(callee), arguments.stream()).collect(java.util.stream.Collectors.toUnmodifiableList());
    }
}
