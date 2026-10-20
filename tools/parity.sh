#!/usr/bin/env bash
# Verifica que docs/lab.html y el motor Java den los mismos numeros (6 decimales).
set -euo pipefail
cd "$(dirname "$0")/.."
tmp=$(mktemp -d)
mvn -q compile && java -cp target/classes de.bottazzi.episteme.LabParity | sort > "$tmp/java.txt"
node tools/lab-parity.mjs | sort > "$tmp/js.txt"
if diff "$tmp/java.txt" "$tmp/js.txt"; then echo "paridad ok: $(wc -l < "$tmp/java.txt") valores"; else exit 1; fi
