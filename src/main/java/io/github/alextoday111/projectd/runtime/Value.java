package io.github.alextoday111.projectd.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeMap;
import io.github.alextoday111.projectd.ast.FunctionLiteral;
import io.github.alextoday111.projectd.ast.TypeIndicator;

public abstract class Value {
    public static final Value NONE = new Value(TypeIndicator.NONE) {
        @Override String render(Set<Value> active) { return "none"; }
    };

    private final TypeIndicator type;

    private Value(TypeIndicator type) {
        this.type = type;
    }

    public final TypeIndicator getType() {
        return type;
    }

    @Override
    public final String toString() {
        return render(Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    abstract String render(Set<Value> active);

    public static final class Scalar<T> extends Value {
        private final T value;

        private Scalar(TypeIndicator type, T value) {
            super(type);
            this.value = value;
        }

        public T getValue() { return value; }

        @Override String render(Set<Value> active) { return value.toString(); }
    }

    public static Scalar<Long> integer(long value) { return new Scalar<>(TypeIndicator.INT, value); }
    public static Scalar<Double> real(double value) { return new Scalar<>(TypeIndicator.REAL, value); }
    public static Scalar<Boolean> bool(boolean value) { return new Scalar<>(TypeIndicator.BOOL, value); }
    public static Scalar<String> string(String value) { return new Scalar<>(TypeIndicator.STRING, value); }

    public static final class Array extends Value {
        private final NavigableMap<Long, Value> elements = new TreeMap<>();

        public Array() { super(TypeIndicator.ARRAY); }

        public Value get(long key) {
            Value value = elements.get(key);
            if (value == null) throw new RuntimeError("Missing array element at index " + key);
            return value;
        }

        public void set(long key, Value value) {
            elements.put(key, value);
        }

        public List<Value> values() { return new ArrayList<>(elements.values()); }

        public Array concat(Array right) {
            Array result = new Array();
            result.elements.putAll(elements);
            long offset = elements.isEmpty() ? 0 : elements.lastKey();
            for (Map.Entry<Long, Value> entry : right.elements.entrySet()) {
                try {
                    result.set(Math.addExact(offset, entry.getKey()), entry.getValue());
                } catch (ArithmeticException overflow) {
                    throw new RuntimeError("Array index overflow for '+'");
                }
            }
            return result;
        }

        @Override String render(Set<Value> active) {
            if (!active.add(this)) return "[...]";
            StringJoiner result = new StringJoiner(", ", "[", "]");
            boolean sparse = !elements.isEmpty() && elements.lastKey() != elements.size();
            elements.forEach((key, value) -> result.add((sparse ? key + " := " : "") + value.render(active)));
            active.remove(this);
            return result.toString();
        }
    }

    public static final class Tuple extends Value {
        private final List<String> names;
        private final List<Value> fields;
        private final Map<String, Integer> positions = new HashMap<>();

        public Tuple(List<String> names, List<Value> fields) {
            super(TypeIndicator.TUPLE);
            this.names = new ArrayList<>(names);
            this.fields = new ArrayList<>(fields);
            for (int i = 0; i < names.size(); i++) {
                String name = names.get(i);
                if (name != null && positions.putIfAbsent(name, i) != null) {
                    throw new RuntimeError("Duplicate tuple field '" + name + "'");
                }
            }
        }

        public int index(String name, Long position) {
            if (name != null) {
                Integer index = positions.get(name);
                if (index == null) throw new RuntimeError("Missing tuple field '" + name + "'");
                return index;
            }
            if (position < 1 || position > fields.size()) {
                throw new RuntimeError("Missing tuple field at position " + position);
            }
            return position.intValue() - 1;
        }

        public Value get(int index) { return fields.get(index); }
        public void set(int index, Value value) { fields.set(index, value); }
        public List<Value> values() { return new ArrayList<>(fields); }

        public Tuple concat(Tuple right) {
            List<String> combinedNames = new ArrayList<>(names);
            combinedNames.addAll(right.names);
            List<Value> combinedFields = values();
            combinedFields.addAll(right.fields);
            return new Tuple(combinedNames, combinedFields);
        }

        @Override String render(Set<Value> active) {
            if (!active.add(this)) return "{...}";
            StringJoiner result = new StringJoiner(", ", "{", "}");
            for (int i = 0; i < fields.size(); i++) {
                String name = names.get(i);
                result.add((name == null ? "" : name + " := ") + fields.get(i).render(active));
            }
            active.remove(this);
            return result.toString();
        }
    }

    public static final class Function extends Value {
        private final FunctionLiteral literal;
        private final Environment closure;

        public Function(FunctionLiteral literal, Environment environment) {
            super(TypeIndicator.FUNC);
            this.literal = literal;
            this.closure = environment.capture();
        }

        public FunctionLiteral getLiteral() { return literal; }
        public Environment getClosure() { return closure; }

        @Override String render(Set<Value> active) { return "<func>"; }
    }
}
