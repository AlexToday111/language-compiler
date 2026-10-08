// Intended output values: 30, 1, 3, 60, 2.5, "beta", 9.
// Not runtime-verified. Array/tuple positions start at one.
var a := {
    {1, {10, 20, 30}},
    {2, {40, 50, 60}}
}

var grid := [[1, 2], [3, 4]]

var records := [
    {
        label := "alpha",
        payload := {
            score := 7,
            samples := [1.5, 2.5]
        }
    },
    {
        label := "beta",
        payload := {
            score := 9,
            samples := [3.5, 4.5]
        }
    }
]

var data := {
    metadata := {version := 1, name := "demo"},
    matrix := grid,
    nested := a,
    records := records
}

print a.1.2.3
print data.metadata.version
print data.matrix[2][1]
print data.nested.2.2.3
print data.records[1].payload.samples[2]
print records[2].label
print records[2].payload.score
