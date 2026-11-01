var compute := func(value) is
    if 10 > 3 then
        var result := 5 + 3 * 2
        return result
    else
        print value
    end
    print "unreachable"
end
print compute(1), 1.5 + 2.5, not (true)
loop
    print "once"
    exit
    print "unreachable"
end
