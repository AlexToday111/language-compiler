var base := 10
var makeAdder := func(offset) is
    return func(value) => base + offset + value
end
var add := makeAdder(2)
for i in 1..3 loop
    if i > 1 then
        var base := i
        print base, add(i)
    end
end
