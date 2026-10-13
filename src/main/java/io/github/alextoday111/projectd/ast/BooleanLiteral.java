package io.github.alextoday111.projectd.ast;

import java.util.Objects;

public final class BooleanLiteral extends Literal<Boolean> {
    public BooleanLiteral(Boolean value, String lexeme) {
        super(Objects.requireNonNull(value), lexeme);
    }
}
