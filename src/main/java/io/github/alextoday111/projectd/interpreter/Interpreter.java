package io.github.alextoday111.projectd.interpreter;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.Consumer;
import io.github.alextoday111.projectd.ast.*;
import io.github.alextoday111.projectd.runtime.Environment;
import io.github.alextoday111.projectd.runtime.RuntimeError;
import io.github.alextoday111.projectd.runtime.Value;
import io.github.alextoday111.projectd.runtime.Value.Array;
import io.github.alextoday111.projectd.runtime.Value.Function;
import io.github.alextoday111.projectd.runtime.Value.Scalar;
import io.github.alextoday111.projectd.runtime.Value.Tuple;

public final class Interpreter {
    private final PrintStream output;

    public Interpreter(PrintStream output) {
        this.output = Objects.requireNonNull(output);
    }

    public void execute(Program program) {
        try {
            execute(program.getStatements(), new Environment(null));
        } catch (ControlFlow.Return signal) {
            throw new RuntimeError("return is only valid inside a function");
        } catch (ControlFlow.Exit signal) {
            throw new RuntimeError("exit is only valid inside a loop in the current function");
        }
    }

    private void execute(List<? extends Statement> statements, Environment environment) {
        for (Statement statement : statements) execute(statement, environment);
    }

    private void executeBlock(Block block, Environment environment) {
        execute(block.getStatements(), new Environment(environment));
    }

    private void execute(Statement statement, Environment environment) {
        if (statement instanceof VariableDeclaration) {
            VariableDeclaration declaration = (VariableDeclaration) statement;
            String name = declaration.getIdentifier().getName();
            environment.define(name, Value.NONE);
            if (declaration.getInitializer() != null) {
                environment.assign(name, evaluate(declaration.getInitializer(), environment));
            }
        } else if (statement instanceof VariableDeclarationGroup) {
            execute(((VariableDeclarationGroup) statement).getDeclarations(), environment);
        } else if (statement instanceof Assignment) {
            Assignment assignment = (Assignment) statement;
            Consumer<Value> target = assignmentTarget(assignment.getTarget(), environment);
            target.accept(evaluate(assignment.getValue(), environment));
        } else if (statement instanceof PrintStatement) {
            StringJoiner line = new StringJoiner(" ");
            for (Expression expression : ((PrintStatement) statement).getExpressions()) {
                line.add(evaluate(expression, environment).toString());
            }
            output.println(line);
        } else if (statement instanceof IfStatement) {
            IfStatement conditional = (IfStatement) statement;
            if (booleanValue(evaluate(conditional.getCondition(), environment), "if condition")) {
                executeBlock(conditional.getThenBranch(), environment);
            } else if (conditional.getElseBranch() != null) {
                executeBlock(conditional.getElseBranch().getBody(), environment);
            }
        } else if (statement instanceof ScopedBlock) {
            executeBlock(((ScopedBlock) statement).getBody(), environment);
        } else if (statement instanceof ReturnStatement) {
            Expression expression = ((ReturnStatement) statement).getValue();
            throw new ControlFlow.Return(expression == null ? Value.NONE : evaluate(expression, environment));
        } else if (statement instanceof ExitStatement) {
            throw ControlFlow.Exit.INSTANCE;
        } else {
            executeLoop(statement, environment);
        }
    }

    private void executeLoop(Statement statement, Environment environment) {
        try {
            if (statement instanceof WhileLoop) {
                WhileLoop loop = (WhileLoop) statement;
                while (booleanValue(evaluate(loop.getCondition(), environment), "while condition")) {
                    executeBlock(loop.getBody(), environment);
                }
            } else if (statement instanceof InfiniteLoop) {
                while (true) executeBlock(((InfiniteLoop) statement).getBody(), environment);
            } else if (statement instanceof ForLoop) {
                executeFor((ForLoop) statement, environment);
            } else {
                throw new RuntimeError("Unsupported statement " + statement.getClass().getSimpleName());
            }
        } catch (ControlFlow.Exit signal) {
            // An exit is consumed by the innermost active loop.
        }
    }

    private void executeFor(ForLoop loop, Environment environment) {
        Value iterable = evaluate(loop.getIterable(), environment);
        if (loop.getRangeEnd() != null) {
            long start = integerValue(iterable, "for range start");
            long end = integerValue(evaluate(loop.getRangeEnd(), environment), "for range end");
            for (long index = start; index <= end; index++) {
                executeIteration(loop, environment, Value.integer(index));
                if (index == end) break;
            }
            return;
        }

        List<Value> values;
        if (iterable instanceof Array) {
            values = ((Array) iterable).values();
        } else if (iterable instanceof Tuple) {
            values = ((Tuple) iterable).values();
        } else {
            throw new RuntimeError("for requires an integer range, array or tuple, found " + type(iterable));
        }
        for (Value value : values) executeIteration(loop, environment, value);
    }

    private void executeIteration(ForLoop loop, Environment environment, Value value) {
        Environment iteration = new Environment(environment);
        if (loop.getVariable() != null) iteration.define(loop.getVariable().getName(), value);
        execute(loop.getBody().getStatements(), iteration);
    }

    private Consumer<Value> assignmentTarget(Reference target, Environment environment) {
        if (target instanceof Identifier) {
            String name = ((Identifier) target).getName();
            return value -> environment.assign(name, value);
        }
        if (target instanceof ArrayIndex) {
            ArrayIndex index = (ArrayIndex) target;
            Array array = array(evaluate(index.getTarget(), environment));
            long key = arrayKey(evaluate(index.getIndex(), environment));
            return value -> array.set(key, value);
        }
        if (target instanceof TupleFieldAccess) {
            TupleFieldAccess access = (TupleFieldAccess) target;
            Tuple tuple = tuple(evaluate(access.getTarget(), environment));
            int index = tupleIndex(tuple, access);
            return value -> tuple.set(index, value);
        }
        throw new RuntimeError("Invalid assignment target: " + target.getClass().getSimpleName());
    }

    private Value evaluate(Expression expression, Environment environment) {
        if (expression instanceof IntegerLiteral) return Value.integer(((IntegerLiteral) expression).getValue());
        if (expression instanceof RealLiteral) return finiteReal(((RealLiteral) expression).getValue(), "real literal");
        if (expression instanceof BooleanLiteral) return Value.bool(((BooleanLiteral) expression).getValue());
        if (expression instanceof StringLiteral) return Value.string(((StringLiteral) expression).getValue());
        if (expression instanceof NoneLiteral) return Value.NONE;
        if (expression instanceof Identifier) return environment.get(((Identifier) expression).getName());
        if (expression instanceof UnaryExpression) return evaluateUnary((UnaryExpression) expression, environment);
        if (expression instanceof BinaryExpression) return evaluateBinary((BinaryExpression) expression, environment);
        if (expression instanceof TypeCheckExpression) {
            TypeCheckExpression check = (TypeCheckExpression) expression;
            return Value.bool(evaluate(check.getReference(), environment).getType() == check.getType());
        }
        if (expression instanceof ArrayLiteral) {
            Array array = new Array();
            long key = 1;
            for (Expression element : ((ArrayLiteral) expression).getElements()) {
                array.set(key++, evaluate(element, environment));
            }
            return array;
        }
        if (expression instanceof TupleLiteral) {
            List<String> names = new ArrayList<>();
            List<Value> fields = new ArrayList<>();
            for (TupleElement element : ((TupleLiteral) expression).getElements()) {
                names.add(element.getName() == null ? null : element.getName().getName());
                fields.add(evaluate(element.getValue(), environment));
            }
            return new Tuple(names, fields);
        }
        if (expression instanceof ArrayIndex) {
            ArrayIndex index = (ArrayIndex) expression;
            Array array = array(evaluate(index.getTarget(), environment));
            return array.get(arrayKey(evaluate(index.getIndex(), environment)));
        }
        if (expression instanceof TupleFieldAccess) {
            TupleFieldAccess access = (TupleFieldAccess) expression;
            Tuple tuple = tuple(evaluate(access.getTarget(), environment));
            return tuple.get(tupleIndex(tuple, access));
        }
        if (expression instanceof FunctionLiteral) return new Function((FunctionLiteral) expression, environment);
        if (expression instanceof FunctionCall) return call((FunctionCall) expression, environment);
        throw new RuntimeError("Unsupported expression " + expression.getClass().getSimpleName());
    }

    private Value call(FunctionCall call, Environment caller) {
        Value callee = evaluate(call.getCallee(), caller);
        if (!(callee instanceof Function)) throw new RuntimeError("Cannot call value of type " + type(callee));
        Function function = (Function) callee;
        FunctionLiteral literal = function.getLiteral();
        List<Identifier> parameters = literal.getParameters().getIdentifiers();
        List<Expression> arguments = call.getArguments();
        if (parameters.size() != arguments.size()) {
            throw new RuntimeError("Function expects " + parameters.size() + " argument(s), got " + arguments.size());
        }
        Environment invocation = new Environment(function.getClosure());
        for (int i = 0; i < arguments.size(); i++) {
            invocation.define(parameters.get(i).getName(), evaluate(arguments.get(i), caller));
        }
        if (literal.getExpressionBody() != null) return evaluate(literal.getExpressionBody(), invocation);
        try {
            execute(literal.getBody().getStatements(), invocation);
        } catch (ControlFlow.Return signal) {
            return signal.value;
        } catch (ControlFlow.Exit signal) {
            throw new RuntimeError("exit is only valid inside a loop in the current function");
        }
        return Value.NONE;
    }

    private Value evaluateUnary(UnaryExpression unary, Environment environment) {
        Value operand = evaluate(unary.getOperand(), environment);
        String operator = unary.getOperator();
        if (operator.equals("not")) return Value.bool(!booleanValue(operand, "'not'"));
        if (isNumeric(operand)) {
            if (operator.equals("+")) return operand;
            if (operator.equals("-")) {
                if (operand.getType() == TypeIndicator.REAL) return Value.real(-number(operand).doubleValue());
                try {
                    return Value.integer(Math.negateExact(number(operand).longValue()));
                } catch (ArithmeticException overflow) {
                    throw new RuntimeError("Integer overflow for unary '-'");
                }
            }
        }
        throw new RuntimeError("Invalid operand for unary '" + operator + "': " + type(operand));
    }

    private Value evaluateBinary(BinaryExpression binary, Environment environment) {
        String operator = binary.getOperator();
        Value left = evaluate(binary.getLeft(), environment);
        switch (operator) {
            case "and":
                return Value.bool(booleanValue(left, "'and'") &&
                        booleanValue(evaluate(binary.getRight(), environment), "'and'"));
            case "or":
                return Value.bool(booleanValue(left, "'or'") ||
                        booleanValue(evaluate(binary.getRight(), environment), "'or'"));
            case "xor":
                return Value.bool(booleanValue(left, "'xor'") ^
                        booleanValue(evaluate(binary.getRight(), environment), "'xor'"));
            default:
                return applyBinary(operator, left, evaluate(binary.getRight(), environment));
        }
    }

    private Value applyBinary(String operator, Value left, Value right) {
        if (left == Value.NONE || right == Value.NONE) throw invalidOperands(operator, left, right);
        if (operator.equals("+")) {
            if (left.getType() == TypeIndicator.STRING || right.getType() == TypeIndicator.STRING) {
                return Value.string(left.toString() + right);
            }
            if (left instanceof Array && right instanceof Array) return ((Array) left).concat((Array) right);
            if (left instanceof Tuple && right instanceof Tuple) return ((Tuple) left).concat((Tuple) right);
        }
        if (isNumeric(left) && isNumeric(right)) {
            switch (operator) {
                case "+": case "-": case "*": case "/":
                    return arithmetic(operator, left, right);
                case "<": return Value.bool(compareNumbers(left, right) < 0);
                case "<=": return Value.bool(compareNumbers(left, right) <= 0);
                case ">": return Value.bool(compareNumbers(left, right) > 0);
                case ">=": return Value.bool(compareNumbers(left, right) >= 0);
                case "=": return Value.bool(compareNumbers(left, right) == 0);
                case "/=": return Value.bool(compareNumbers(left, right) != 0);
                default: break;
            }
        }
        if ((operator.equals("=") || operator.equals("/=")) && left.getType() == right.getType()
                && left instanceof Scalar<?>) {
            boolean equal = ((Scalar<?>) left).getValue().equals(((Scalar<?>) right).getValue());
            return Value.bool(operator.equals("=") == equal);
        }
        throw invalidOperands(operator, left, right);
    }

    private Value arithmetic(String operator, Value left, Value right) {
        if (left.getType() == TypeIndicator.INT && right.getType() == TypeIndicator.INT) {
            long a = number(left).longValue();
            long b = number(right).longValue();
            try {
                switch (operator) {
                    case "+": return Value.integer(Math.addExact(a, b));
                    case "-": return Value.integer(Math.subtractExact(a, b));
                    case "*": return Value.integer(Math.multiplyExact(a, b));
                    case "/":
                        if (b == 0) throw new RuntimeError("Division by zero");
                        if (a == Long.MIN_VALUE && b == -1) throw new ArithmeticException();
                        return Value.integer(Math.floorDiv(a, b));
                    default: throw invalidOperands(operator, left, right);
                }
            } catch (ArithmeticException overflow) {
                throw new RuntimeError("Integer overflow for '" + operator + "'");
            }
        }

        double a = number(left).doubleValue();
        double b = number(right).doubleValue();
        double result;
        switch (operator) {
            case "+": result = a + b; break;
            case "-": result = a - b; break;
            case "*": result = a * b; break;
            case "/":
                if (b == 0) throw new RuntimeError("Division by zero");
                result = a / b;
                break;
            default: throw invalidOperands(operator, left, right);
        }
        return finiteReal(result, "'" + operator + "'");
    }

    private static int compareNumbers(Value left, Value right) {
        Number a = number(left);
        Number b = number(right);
        if (left.getType() != right.getType()) {
            // Compare the integer exactly even when it cannot be represented as a double.
            return exactNumber(a).compareTo(exactNumber(b));
        }
        if (left.getType() == TypeIndicator.INT) return Long.compare(a.longValue(), b.longValue());
        return a.doubleValue() == b.doubleValue() ? 0 : Double.compare(a.doubleValue(), b.doubleValue());
    }

    private static BigDecimal exactNumber(Number number) {
        return number instanceof Long ? BigDecimal.valueOf(number.longValue()) : new BigDecimal(number.doubleValue());
    }

    private static Value finiteReal(double value, String operation) {
        if (!Double.isFinite(value)) throw new RuntimeError("Non-finite real value for " + operation);
        return Value.real(value);
    }

    private static boolean isNumeric(Value value) {
        return value.getType() == TypeIndicator.INT || value.getType() == TypeIndicator.REAL;
    }

    private static Number number(Value value) { return (Number) ((Scalar<?>) value).getValue(); }

    private static long integerValue(Value value, String operation) {
        if (value.getType() != TypeIndicator.INT) {
            throw new RuntimeError(operation + " requires int, found " + type(value));
        }
        return number(value).longValue();
    }

    private static boolean booleanValue(Value value, String operation) {
        if (value.getType() != TypeIndicator.BOOL) {
            throw new RuntimeError(operation + " requires bool, found " + type(value));
        }
        return (Boolean) ((Scalar<?>) value).getValue();
    }

    private static Array array(Value value) {
        if (!(value instanceof Array)) throw new RuntimeError("Array indexing requires [], found " + type(value));
        return (Array) value;
    }

    private static long arrayKey(Value value) {
        long key = integerValue(value, "Array index");
        if (key < 1) throw new RuntimeError("Array index must be positive, found " + key);
        return key;
    }

    private static Tuple tuple(Value value) {
        if (!(value instanceof Tuple)) throw new RuntimeError("Tuple field access requires {}, found " + type(value));
        return (Tuple) value;
    }

    private static int tupleIndex(Tuple tuple, TupleFieldAccess access) {
        return tuple.index(access.getField().getName(), access.getField().getPosition());
    }

    private static String type(Value value) { return value.getType().getSpelling(); }

    private static RuntimeError invalidOperands(String operator, Value left, Value right) {
        return new RuntimeError("Invalid operands for '" + operator + "': " + type(left) + " and " + type(right));
    }
}
