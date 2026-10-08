#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$project_dir"

mvn --batch-mode --no-transfer-progress package
if [ "$#" -eq 0 ]; then
    set -- examples/valid/01_simple.d examples/valid/02_calculator.d examples/valid/03_nested_values.d
fi
java -jar target/language-compiler-0.1.0-SNAPSHOT.jar "$@"
