#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "$0")" && pwd)"
cd "$project_dir"

mkdir -p out
if command -v javac >/dev/null 2>&1; then
    javac -encoding UTF-8 -d out src/*.java
else
    # Some minimal JDK installations contain the compiler module but no javac launcher.
    java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -d out src/*.java
fi

if [ "$#" -eq 0 ]; then
    set -- examples/01_simple.d examples/02_calculator.d examples/03_nested_values.d
fi

java -cp out Main "$@"
