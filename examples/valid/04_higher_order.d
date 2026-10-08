// Intended output values: 25, 7, then 25, 16, 34, then 11.
// Not runtime-verified. Ascending for ranges assume inclusive endpoints.
var rowSum := func(row) => row[1] + row[2] + row[3]
var transform := func(operation, array, config) => operation(array) + config.offset
var applyTwice := func(callback, value) => callback(callback(value))

var config := {
    offset := 10,
    values := [4, 5, 6],
    callbacks := {
        increment := func(x) => x + 1,
        double := func(x) => x * 2
    }
}

var rows := [[4, 5, 6], [1, 2, 3], [7, 8, 9]]

var processRows := func(callback, matrix, options) is
    var results := []
    for i in 1..3 loop
        results[i] := callback(matrix[i]) + options.offset
    end
    return results
end

// Returns an existing function value; captured locals are not required.
var selectIncrement := func(options) is
    return options.callbacks.increment
end

print transform(rowSum, config.values, config)
print applyTwice(config.callbacks.increment, 5)

var processed := processRows(rowSum, rows, config)
print processed[1], processed[2], processed[3]

var selected := selectIncrement(config)
print selected(config.offset)
