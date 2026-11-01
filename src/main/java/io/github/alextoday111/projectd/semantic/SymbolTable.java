package io.github.alextoday111.projectd.semantic;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class SymbolTable {
    private final Deque<Set<String>> scopes = new ArrayDeque<>();

    public SymbolTable() { enterScope(); }
    public void enterScope() { scopes.push(new HashSet<>()); }
    public void leaveScope() {
        if (scopes.size() == 1) throw new IllegalStateException("Cannot leave the root scope");
        scopes.pop();
    }
    public boolean declare(String name) { return scopes.peek().add(Objects.requireNonNull(name)); }
    public boolean isDeclared(String name) {
        for (Set<String> scope : scopes) {
            if (scope.contains(name)) return true;
        }
        return false;
    }
}
