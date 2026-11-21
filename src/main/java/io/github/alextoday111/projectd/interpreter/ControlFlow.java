package io.github.alextoday111.projectd.interpreter;

import io.github.alextoday111.projectd.runtime.Value;

abstract class ControlFlow extends RuntimeException {
    private ControlFlow() {
        super(null, null, false, false);
    }

    static final class Exit extends ControlFlow {
        static final Exit INSTANCE = new Exit();
        private Exit() { }
    }

    static final class Return extends ControlFlow {
        final Value value;

        Return(Value value) {
            this.value = value;
        }
    }
}
