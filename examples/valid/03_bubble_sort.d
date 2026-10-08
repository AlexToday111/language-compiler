// Intended output values: 1, 2, 4, 5, 8. Not runtime-verified.
// Assumption: ascending for ranges include both endpoints.
// count is the known contiguous array size, at least two.
var bubbleSort := func(values, count) is
    for pass in 1..count loop
        var swaps := 0
        for i in 1..(count - 1) loop
            if values[i] > values[i + 1] then
                var temp := values[i]
                values[i] := values[i + 1]
                values[i + 1] := temp
                swaps := swaps + 1
            end
        end
        if swaps = 0 then
            return values
        end
    end
    return values
end

var numbers := [5, 1, 4, 2, 8]
var sorted := bubbleSort(numbers, 5)
print sorted[1], sorted[2], sorted[3], sorted[4], sorted[5]
