package io.github.alextoday111.projectd.optimization;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import io.github.alextoday111.projectd.ast.*;

public final class AstOptimizer {
    public Program optimize(Program program) {
        return new Program(statements(Objects.requireNonNull(program).getStatements(), false, 0));
    }

    private List<Statement> statements(List<Statement> original, boolean function, int loops) {
        List<Statement> result = new ArrayList<>();
        for (Statement statement : original) {
            Statement replacement = statement(statement, function, loops);
            if (replacement == null) continue;
            result.add(replacement);
            if (terminates(replacement, function, loops)) break;
        }
        return result;
    }

    private Block block(Block original, boolean function, int loops) {
        return new Block(statements(original.getStatements(), function, loops));
    }

    private Statement statement(Statement original, boolean function, int loops) {
        if (original instanceof VariableDeclaration) {
            VariableDeclaration declaration = (VariableDeclaration) original;
            return new VariableDeclaration(declaration.getIdentifier(), optional(declaration.getInitializer()));
        }
        if (original instanceof VariableDeclarationGroup) {
            List<VariableDeclaration> declarations = new ArrayList<>();
            for (VariableDeclaration declaration : ((VariableDeclarationGroup) original).getDeclarations())
                declarations.add((VariableDeclaration) statement(declaration, function, loops));
            return new VariableDeclarationGroup(declarations);
        }
        if (original instanceof Assignment) {
            Assignment assignment = (Assignment) original;
            return new Assignment(reference(assignment.getTarget()), expression(assignment.getValue()));
        }
        if (original instanceof PrintStatement)
            return new PrintStatement(expressions(((PrintStatement) original).getExpressions()));
        if (original instanceof ReturnStatement)
            return new ReturnStatement(optional(((ReturnStatement) original).getValue()));
        if (original instanceof IfStatement) {
            IfStatement conditional = (IfStatement) original;
            Expression condition = expression(conditional.getCondition());
            if (condition instanceof BooleanLiteral) {
                Block selected = ((BooleanLiteral) condition).getValue() ? conditional.getThenBranch()
                        : conditional.getElseBranch() == null ? null : conditional.getElseBranch().getBody();
                return selected == null ? null : new ScopedBlock(block(selected, function, loops));
            }
            return new IfStatement(condition, block(conditional.getThenBranch(), function, loops),
                    conditional.getElseBranch() == null ? null
                            : new Else(block(conditional.getElseBranch().getBody(), function, loops)),
                    conditional.isShortForm());
        }
        if (original instanceof WhileLoop) {
            WhileLoop loop = (WhileLoop) original;
            return new WhileLoop(expression(loop.getCondition()), block(loop.getBody(), function, loops + 1));
        }
        if (original instanceof ForLoop) {
            ForLoop loop = (ForLoop) original;
            return new ForLoop(loop.getVariable(), expression(loop.getIterable()), optional(loop.getRangeEnd()),
                    block(loop.getBody(), function, loops + 1));
        }
        if (original instanceof InfiniteLoop)
            return new InfiniteLoop(block(((InfiniteLoop) original).getBody(), function, loops + 1));
        if (original instanceof ScopedBlock)
            return new ScopedBlock(block(((ScopedBlock) original).getBody(), function, loops));
        if (original instanceof ExitStatement) return original;
        throw new IllegalArgumentException("Unsupported statement: " + original.getClass().getName());
    }

    private boolean terminates(Statement statement, boolean function, int loops) {
        if (statement instanceof ReturnStatement) return function;
        if (statement instanceof ExitStatement) return loops > 0;
        if (statement instanceof ScopedBlock)
            return terminates(((ScopedBlock) statement).getBody(), function, loops);
        if (statement instanceof IfStatement) {
            IfStatement conditional = (IfStatement) statement;
            return conditional.getElseBranch() != null && terminates(conditional.getThenBranch(), function, loops)
                    && terminates(conditional.getElseBranch().getBody(), function, loops);
        }

        return false;
    }

    private boolean terminates(Block block, boolean function, int loops) {
        List<Statement> body = block.getStatements();
        return !body.isEmpty() && terminates(body.get(body.size() - 1), function, loops);
    }

    private Expression optional(Expression original) { return original == null ? null : expression(original); }
    private List<Expression> expressions(List<Expression> original) {
        return original.stream().map(this::expression).collect(Collectors.toList());
    }
    private Reference reference(Reference original) { return (Reference) expression(original); }

    private Expression expression(Expression original) {
        if (original instanceof Literal<?> || original instanceof Identifier) return original;
        if (original instanceof BinaryExpression) {
            BinaryExpression binary = (BinaryExpression) original;
            return foldBinary(new BinaryExpression(binary.getOperator(),
                    expression(binary.getLeft()), expression(binary.getRight())));
        }
        if (original instanceof UnaryExpression) {
            UnaryExpression unary = (UnaryExpression) original;
            return foldUnary(new UnaryExpression(unary.getOperator(), expression(unary.getOperand())));
        }
        if (original instanceof ArrayLiteral) return new ArrayLiteral(expressions(((ArrayLiteral) original).getElements()));
        if (original instanceof TupleLiteral) {
            List<TupleElement> elements = new ArrayList<>();
            for (TupleElement element : ((TupleLiteral) original).getElements())
                elements.add(new TupleElement(element.getName(), expression(element.getValue())));
            return new TupleLiteral(elements);
        }
        if (original instanceof ArrayIndex) {
            ArrayIndex index = (ArrayIndex) original;
            return new ArrayIndex(reference(index.getTarget()), expression(index.getIndex()));
        }
        if (original instanceof TupleFieldAccess) {
            TupleFieldAccess access = (TupleFieldAccess) original;
            return new TupleFieldAccess(reference(access.getTarget()), access.getField());
        }
        if (original instanceof FunctionCall) {
            FunctionCall call = (FunctionCall) original;
            return new FunctionCall(reference(call.getCallee()), expressions(call.getArguments()));
        }
        if (original instanceof TypeCheckExpression) {
            TypeCheckExpression check = (TypeCheckExpression) original;
            return new TypeCheckExpression(reference(check.getReference()), check.getType());
        }
        if (original instanceof FunctionLiteral) {
            FunctionLiteral literal = (FunctionLiteral) original;
            return literal.getBody() == null
                    ? new FunctionLiteral(literal.getParameters(), expression(literal.getExpressionBody()))
                    : new FunctionLiteral(literal.getParameters(), block(literal.getBody(), true, 0));
        }
        throw new IllegalArgumentException("Unsupported expression: " + original.getClass().getName());
    }

    private static Expression foldUnary(UnaryExpression expression) {
        Expression value = expression.getOperand();
        String operator = expression.getOperator();
        if (value instanceof BooleanLiteral && operator.equals("not"))
            return bool(!((BooleanLiteral) value).getValue());
        if (value instanceof IntegerLiteral) {
            long integer = ((IntegerLiteral) value).getValue();
            if (operator.equals("+")) return value;
            if (operator.equals("-") && integer != Long.MIN_VALUE) return integer(-integer);
        }
        if (value instanceof RealLiteral && Double.isFinite(((RealLiteral) value).getValue())) {
            double real = ((RealLiteral) value).getValue();
            if (operator.equals("+")) return value;
            if (operator.equals("-")) return real(-real);
        }
        return expression;
    }

    private static Expression foldBinary(BinaryExpression expression) {
        Expression left = expression.getLeft();
        Expression right = expression.getRight();
        String operator = expression.getOperator();
        if (left instanceof BooleanLiteral && right instanceof BooleanLiteral) {
            boolean a = ((BooleanLiteral) left).getValue(), b = ((BooleanLiteral) right).getValue();
            switch (operator) {
                case "and": return bool(a && b);
                case "or": return bool(a || b);
                case "xor": return bool(a ^ b);
                default: return expression;
            }
        }
        if (left instanceof IntegerLiteral && right instanceof IntegerLiteral) {
            long a = ((IntegerLiteral) left).getValue(), b = ((IntegerLiteral) right).getValue();
            Boolean comparison = compare(operator, Long.compare(a, b));
            if (comparison != null) return bool(comparison);
            try {
                switch (operator) {
                    case "+": return integer(Math.addExact(a, b));
                    case "-": return integer(Math.subtractExact(a, b));
                    case "*": return integer(Math.multiplyExact(a, b));
                    case "/":
                        if (b != 0 && !(a == Long.MIN_VALUE && b == -1)) return integer(Math.floorDiv(a, b));
                        break;
                    default: break;
                }
            } catch (ArithmeticException overflow) {
                return expression;
            }
            return expression;
        }
        BigDecimal a = exactNumber(left), b = exactNumber(right);
        if (a == null || b == null) return expression;
        Boolean comparison = compare(operator, a.compareTo(b));
        if (comparison != null) return bool(comparison);

        if (a.signum() == 0 || b.signum() == 0) return expression;
        try {
            BigDecimal result;
            switch (operator) {
                case "+": result = a.add(b); break;
                case "-": result = a.subtract(b); break;
                case "*": result = a.multiply(b); break;
                case "/": result = a.divide(b); break;
                default: return expression;
            }
            double value = result.doubleValue();
            if (value != 0 && Double.isFinite(value) && new BigDecimal(value).compareTo(result) == 0)
                return real(value);
        } catch (ArithmeticException inexactDivision) {
            return expression;
        }
        return expression;
    }

    private static BigDecimal exactNumber(Expression expression) {
        if (expression instanceof RealLiteral) {
            double value = ((RealLiteral) expression).getValue();
            return Double.isFinite(value) ? new BigDecimal(value) : null;
        }
        if (expression instanceof IntegerLiteral) {
            long value = ((IntegerLiteral) expression).getValue();
            BigDecimal exact = BigDecimal.valueOf(value);

            return new BigDecimal((double) value).compareTo(exact) == 0 ? exact : null;
        }
        return null;
    }

    private static Boolean compare(String operator, int comparison) {
        switch (operator) {
            case "<": return comparison < 0;
            case "<=": return comparison <= 0;
            case ">": return comparison > 0;
            case ">=": return comparison >= 0;
            case "=": return comparison == 0;
            case "/=": return comparison != 0;
            default: return null;
        }
    }

    private static IntegerLiteral integer(long value) { return new IntegerLiteral(value, Long.toString(value)); }
    private static RealLiteral real(double value) { return new RealLiteral(value, Double.toString(value)); }
    private static BooleanLiteral bool(boolean value) { return new BooleanLiteral(value, Boolean.toString(value)); }
}
