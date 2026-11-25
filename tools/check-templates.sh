#!/usr/bin/env bash
# Generates a game from every project template, adds one of every generator to it, checks it with
# ease check, builds it against this checkout (a composite build) and runs its tests, formatting
# and rules.
# Usage: tools/check-templates.sh [work folder]; the folder defaults to a new temporary one.
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"
work="${1:-$(mktemp -d)}"
mkdir -p "$work"
cd "$root"

./gradlew --quiet :ease-cli:installDist
ease="$root/ease-cli/build/install/ease/bin/ease"

# The first commit ease new makes needs an identity, which a CI runner has none of.
export GIT_AUTHOR_NAME="Ease Templates" GIT_AUTHOR_EMAIL="templates@ease.invalid"
export GIT_COMMITTER_NAME="$GIT_AUTHOR_NAME" GIT_COMMITTER_EMAIL="$GIT_AUTHOR_EMAIL"

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
  # ease new put the game in git, with ease's merge driver registered, and ease check passes on it.
  [ -d "$game/.git" ] || { echo "$template: ease new made no git repository" >&2; exit 1; }
  driver="$(cd "$game" && git config --local merge.ease.driver)"
  case "$driver" in
    *merge-driver*) ;;
    *) echo "$template: the merge driver is not registered" >&2; exit 1 ;;
  esac
  (cd "$game" && "$ease" check)
  # The game's own wrapper builds it; ease.dir points at this checkout.
  (cd "$game" && ./gradlew --quiet --console=plain "-Pease.dir=$root" check)
  # The run task belongs to the game's module alone: with a generated module beside it, running
  # the game still finds exactly one game.
  (cd "$game" && ./gradlew --quiet --console=plain "-Pease.dir=$root" runHeadless)
  # A pin that differs from the checkout fails the build, naming both versions.
  if pinned="$(cd "$game" && ./gradlew --quiet --console=plain "-Pease.dir=$root" -Pease.version=9.9.9 help 2>&1)"; then
    echo "A game pinning Ease 9.9.9 built against a different checkout" >&2
    exit 1
  fi
  case "$pinned" in
    *9.9.9*) ;;
    *) echo "The pin failure does not name the pinned version: $pinned" >&2; exit 1 ;;
  esac
done
echo "All templates build: $(echo $projects)"
