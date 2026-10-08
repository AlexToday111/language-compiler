var add := func(a, b) => a + b
var multiply := func(a, b) => a * b

var calculator := {
    left := 12,
    right := 30,
    operations := {
        add := add,
        multiply := multiply
    }
}

var apply := func(op, input) =>
    op(input.left, input.right)

print apply(calculator.operations.add, calculator)
print apply(calculator.operations.multiply, calculator)
