package io.github.alextoday111.projectd.ast;

import java.util.Objects;

public final class RealLiteral extends Literal<Double> {
    public RealLiteral(Double value, String lexeme) {
        super(Objects.requireNonNull(value), lexeme);
    }
}
