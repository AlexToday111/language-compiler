#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "$0")" && pwd)"
cd "$project_dir"

mkdir -p out
sources=(src/*.java tests/*.java)

if command -v javac >/dev/null 2>&1; then
    javac -encoding UTF-8 -d out "${sources[@]}"
else
    java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -d out "${sources[@]}"
fi

java -cp out LexerTest
