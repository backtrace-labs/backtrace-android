#!/usr/bin/env bash
set -euo pipefail

# Lint baselines may only carry pre-existing findings. This check fails when a production or test
# source file changed since <base-ref> is referenced by a module's lint baseline, which would mean
# a finding introduced by the change under review was silenced instead of fixed. Build scripts
# (*.gradle) are deliberately out of scope: enabling lint itself touches them.
#
# Usage: scripts/check_lint_baseline_scope.sh <base-ref>

base_ref="${1:?usage: $0 <base-ref>}"

baselines=(
    "backtrace-library/lint-baseline.xml"
    "example-app/lint-baseline.xml"
)

changed_sources="$(git diff --name-only "$base_ref" HEAD -- \
    '*.java' '*.kt' '*.xml' '*.c' '*.cc' '*.cpp' '*.h')"

violations=0
for baseline in "${baselines[@]}"; do
    [ -f "$baseline" ] || continue
    module_dir="$(dirname "$baseline")"

    while IFS= read -r changed_file; do
        [ -n "$changed_file" ] || continue
        case "$changed_file" in
            "$module_dir"/*) module_relative="${changed_file#"$module_dir"/}" ;;
            *) continue ;;
        esac
        if [ "$module_relative" = "$(basename "$baseline")" ]; then
            continue
        fi
        if grep -qF "file=\"$module_relative\"" "$baseline"; then
            echo "Baseline scope violation: changed source $changed_file is referenced by $baseline" >&2
            violations=1
        fi
    done <<< "$changed_sources"
done

if [ "$violations" -ne 0 ]; then
    echo "Fix the new lint findings instead of baselining them, or update the allowlist here." >&2
    exit 1
fi

echo "Lint baselines reference no source files changed since $base_ref."
