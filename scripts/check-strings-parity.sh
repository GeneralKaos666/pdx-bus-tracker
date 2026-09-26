#!/bin/bash
# Fails when any values/strings.xml key lacks a values-es twin. Prints the missing keys.
set -euo pipefail
fail=0
while IFS= read -r base; do
	es="${base%/values/strings.xml}/values-es/strings.xml"
	[ -f "$es" ] || {
		echo "MISSING whole file: $es"
		fail=1
		continue
	}
	base_keys=$(grep -o 'name="[^"]*"' "$base" | sort -u)
	es_keys=$(grep -o 'name="[^"]*"' "$es" | sort -u)
	missing=$(comm -23 <(echo "$base_keys") <(echo "$es_keys") || true)
	if [ -n "$missing" ]; then
		echo "Missing es translations in $es:"
		echo "$missing"
		fail=1
	fi
done < <(find . -path "*/src/main/res/values/strings.xml" -not -path "*/build/*")
exit $fail
