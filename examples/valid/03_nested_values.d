var grid := [[1, 2], [3, 4]]

var packet := {
    meta := {id := 7},
    rows := grid,
    tags := ["lexer", "demo"]
}

var apply := func(f, value) => f(value)
var rowSum := func(row) => row[1] + row[2]

print apply(rowSum, packet.rows[2])
