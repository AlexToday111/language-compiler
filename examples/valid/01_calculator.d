// Intended output values: 25, 15, 100, 4. Not runtime-verified.
// Functions are values; array indices start at one.
var calculator := {
    operations := {
        add := func(a, b) => a + b,
        sub := func(a, b) => a - b,
        mul := func(a, b) => a * b,
        div := func(a, b) => a / b
    },
    defaults := [20, 5],
    metadata := {
        version := 1,
        name := "calculator"
    }
}

var apply := func(operation, args) is
    var left := args[1]
    var right := args[2]
    return operation(left, right)
end

var results := {
    sum := apply(calculator.operations.add, calculator.defaults),
    difference := apply(calculator.operations.sub, calculator.defaults),
    product := apply(calculator.operations.mul, calculator.defaults),
    quotient := apply(calculator.operations.div, calculator.defaults)
}

print results.sum
print results.difference
print results.product
print results.quotient
