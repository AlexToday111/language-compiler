package io.github.alextoday111.projectd.ast;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

public final class AstPrinter {
    public String print(AstNode root) {
        Objects.requireNonNull(root);
        StringBuilder result = new StringBuilder(label(root)).append('\n');
        Deque<Entry> pending = new ArrayDeque<>();
        pushChildren(pending, root, "");
        while (!pending.isEmpty()) {
            Entry entry = pending.pop();
            result.append(entry.prefix).append(entry.last ? "└── " : "├── ")
                    .append(label(entry.node)).append('\n');
            pushChildren(pending, entry.node, entry.prefix + (entry.last ? "    " : "│   "));
        }
        return result.toString();
    }

    private static void pushChildren(Deque<Entry> pending, AstNode node, String prefix) {
        List<? extends AstNode> children = node.getChildren();
        for (int i = children.size() - 1; i >= 0; i--) {
            pending.push(new Entry(children.get(i), prefix, i == children.size() - 1));
        }
    }

    private static String label(AstNode node) {
        String name = node.getClass().getSimpleName();
        if (node instanceof Identifier) {
            return name + ": " + escape(((Identifier) node).getName());
        }
        if (node instanceof Literal<?>) {
            Literal<?> literal = (Literal<?>) node;
            Object value = literal.getValue();
            String text = value == null ? "none" : escape(value.toString());
            return name + ": " + (node instanceof StringLiteral ? "\"" + text + "\"" : text);
        }
        if (node instanceof BinaryExpression) {
            return name + ": " + ((BinaryExpression) node).getOperator();
        }
        if (node instanceof UnaryExpression) {
            return name + ": " + ((UnaryExpression) node).getOperator();
        }
        if (node instanceof TypeIndicator) {
            return "TypeIndicator: " + ((TypeIndicator) node).getSpelling();
        }
        if (node instanceof TupleField) {
            TupleField field = (TupleField) node;
            return name + ": " + (field.getName() == null ? field.getPosition() : escape(field.getName()));
        }
        if (node instanceof IfStatement && ((IfStatement) node).isShortForm()) {
            return name + ": =>";
        }
        if (node instanceof ForLoop && ((ForLoop) node).getRangeEnd() != null) {
            return name + ": ..";
        }
        return name;
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "\\r")
                .replace("\n", "\\n").replace("\t", "\\t");
    }

    private static final class Entry {
        private final AstNode node;
        private final String prefix;
        private final boolean last;

        private Entry(AstNode node, String prefix, boolean last) {
            this.node = node;
            this.prefix = prefix;
            this.last = last;
        }
    }
}
