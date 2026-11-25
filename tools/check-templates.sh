#!/usr/bin/env bash
# Generates a game from every project template, adds one of every generator to it, builds it
# against this checkout (a composite build) and runs its tests, formatting and rules.
# Usage: tools/check-templates.sh [work folder]; the folder defaults to a new temporary one.
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"
work="${1:-$(mktemp -d)}"
mkdir -p "$work"
cd "$root"

./gradlew --quiet :ease-cli:installDist
ease="$root/ease-cli/build/install/ease/bin/ease"

templates="$("$ease" templates)"
projects="$(printf '%s\n' "$templates" | awk -F'\t' '$1 == "project" { print $2 }')"
generators="$(printf '%s\n' "$templates" | awk -F'\t' '$1 == "generator" { print $2 }')"
[ -n "$projects" ] || { echo "ease templates lists no project template" >&2; exit 1; }

for template in $projects; do
  game="$work/$template-game"
  echo "== $template"
  (cd "$work" && "$ease" new "$template" --name "Check $template" --dir "$game")
  for generator in $generators; do
    (cd "$game" && "$ease" generate "$generator" Sample)
  done
  # The game's own wrapper builds it; ease.dir points at this checkout.
  (cd "$game" && ./gradlew --quiet --console=plain "-Pease.dir=$root" check)
done
echo "All templates build: $(echo $projects)"
