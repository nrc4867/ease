#!/usr/bin/env bash
# Prints code=true when the head SHA, against its merge base with the base SHA, changes anything but
# Markdown, else code=false.
# Usage: tools/ci-changed-code.sh <base sha> <head sha>
set -euo pipefail
files="$(git diff --name-only "$1...$2")"
code=false
while IFS= read -r file; do
  [ -z "$file" ] && continue
  case "$file" in
    *.md) ;;
    *) code=true ;;
  esac
done <<< "$files"
echo "code=$code"
