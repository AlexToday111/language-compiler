package io.github.alextoday111.projectd.semantic;

import java.util.List;
import java.util.Objects;
import io.github.alextoday111.projectd.ast.*;
import static io.github.alextoday111.projectd.semantic.SemanticDiagnostic.Code.*;

public final class SemanticAnalyzer {
    public void analyze(Program program) {
        new Analysis().visit(Objects.requireNonNull(program), "Program", false, 0);
    }

    private static final class Analysis {
        private final SymbolTable symbols = new SymbolTable();

        private void declare(Identifier identifier, String path) {
            if (!symbols.declare(identifier.getName())) {
                fail(DUPLICATE_DECLARATION, "Duplicate declaration '" + identifier.getName() + "' in this scope",
                        path, identifier);
            }
        }

        private void block(Block block, String path, boolean function, int loops) {
            symbols.enterScope();
            try { visit(block, path, function, loops); }
            finally { symbols.leaveScope(); }
        }

        private void visit(AstNode node, String path, boolean function, int loops) {
            if (node == null) return;
            if (node instanceof Identifier) {
                String name = ((Identifier) node).getName();
                if (!symbols.isDeclared(name)) fail(UNDECLARED_VARIABLE,
                        "Variable '" + name + "' is not declared before this use", path, node);
                return;
            }
            if (node instanceof VariableDeclaration) {
                VariableDeclaration declaration = (VariableDeclaration) node;

                declare(declaration.getIdentifier(), path + ".identifier");
                visit(declaration.getInitializer(), path + ".initializer", function, loops);
                return;
            }
            if (node instanceof FunctionLiteral) {
                FunctionLiteral literal = (FunctionLiteral) node;
                symbols.enterScope();
                try {
                    List<Identifier> parameters = literal.getParameters().getIdentifiers();
                    for (int i = 0; i < parameters.size(); i++) declare(parameters.get(i), path + ".parameters[" + i + "]");

                    visit(literal.getBody(), path + ".body", true, 0);
                    visit(literal.getExpressionBody(), path + ".expressionBody", true, 0);
                } finally { symbols.leaveScope(); }
                return;
            }
            if (node instanceof IfStatement) {
                IfStatement conditional = (IfStatement) node;
                visit(conditional.getCondition(), path + ".condition", function, loops);
                block(conditional.getThenBranch(), path + ".then", function, loops);
                if (conditional.getElseBranch() != null)
                    block(conditional.getElseBranch().getBody(), path + ".else", function, loops);
                return;
            }
            if (node instanceof ForLoop) {
                ForLoop loop = (ForLoop) node;

                visit(loop.getIterable(), path + ".iterable", function, loops);
                visit(loop.getRangeEnd(), path + ".rangeEnd", function, loops);
                symbols.enterScope();
                try {
                    if (loop.getVariable() != null) declare(loop.getVariable(), path + ".variable");
                    visit(loop.getBody(), path + ".body", function, loops + 1);
                } finally { symbols.leaveScope(); }
                return;
            }
            if (node instanceof WhileLoop) {
                WhileLoop loop = (WhileLoop) node;
                visit(loop.getCondition(), path + ".condition", function, loops);
                block(loop.getBody(), path + ".body", function, loops + 1);
                return;
            }
            if (node instanceof InfiniteLoop) {
                block(((InfiniteLoop) node).getBody(), path + ".body", function, loops + 1);
                return;
            }
            if (node instanceof ScopedBlock) {
                block(((ScopedBlock) node).getBody(), path + ".body", function, loops);
                return;
            }
            if (node instanceof TupleElement) {

                visit(((TupleElement) node).getValue(), path + ".value", function, loops);
                return;
            }
            if (node instanceof ReturnStatement && !function)
                fail(RETURN_OUTSIDE_FUNCTION, "return is only valid inside a function", path, node);
            if (node instanceof ExitStatement && loops == 0)
                fail(EXIT_OUTSIDE_LOOP, "exit is only valid inside a loop in the current function", path, node);
            List<? extends AstNode> children = node.getChildren();
            for (int i = 0; i < children.size(); i++)
                visit(children.get(i), path + ".children[" + i + "]", function, loops);
        }

        private void fail(SemanticDiagnostic.Code code, String message, String path, AstNode node) {
            throw new SemanticException(new SemanticDiagnostic(code, message, path, node));
        }
    }
}
