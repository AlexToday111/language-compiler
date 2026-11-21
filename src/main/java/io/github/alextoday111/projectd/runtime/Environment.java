package io.github.alextoday111.projectd.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class Environment {
    private final Map<String, Binding> bindings = new HashMap<>();
    private final Environment parent;

    public Environment(Environment parent) {
        this.parent = parent;
    }

    public void define(String name, Value value) {
        if (bindings.putIfAbsent(name, new Binding(value)) != null) {
            throw new RuntimeError("Duplicate declaration '" + name + "' in this scope");
        }
    }

    public Value get(String name) {
        return resolve(name).value;
    }

    public void assign(String name, Value value) {
        resolve(name).value = Objects.requireNonNull(value);
    }

    public Environment capture() {
        // Preserve the names visible at creation while sharing their mutable bindings.
        Environment captured = new Environment(parent == null ? null : parent.capture());
        captured.bindings.putAll(bindings);
        return captured;
    }

    private Binding resolve(String name) {
        for (Environment scope = this; scope != null; scope = scope.parent) {
            Binding binding = scope.bindings.get(name);
            if (binding != null) return binding;
        }
        throw new RuntimeError("Undeclared variable '" + name + "'");
    }

    private static final class Binding {
        private Value value;

        private Binding(Value value) {
            this.value = Objects.requireNonNull(value);
        }
    }
}
