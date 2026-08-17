#!/usr/bin/env bash
set -euo pipefail

# Self-test for check_lint_baseline_scope.sh. Builds a sandbox git repository mirroring the
# checked baseline layout and proves the checker passes a clean tree, ignores unbaselined and
# build-script changes, fails on baselined Java and XML source changes, and tolerates a missing
# baseline explicitly. Run from the repository root.

checker="scripts/check_lint_baseline_scope.sh"
if [ ! -f "$checker" ]; then
    echo "Self-test must run from the repository root; missing $checker" >&2
    exit 1
fi

sandbox="$(mktemp -d)"
trap 'rm -rf "$sandbox"' EXIT

mkdir -p "$sandbox/scripts"
cp "$checker" "$sandbox/scripts/"

# Capture the subshell status explicitly: with errexit active the epilogue would be unreachable on
# failure, and wrapping the subshell in `|| { ... }` would disable errexit inside it.
set +e
(
    set -e
    cd "$sandbox"
    git init -q .
    git config user.email selftest@example.invalid
    git config user.name selftest

    mkdir -p backtrace-library/src/main/java example-app/src/main/res
    cat > backtrace-library/lint-baseline.xml <<'EOF'
<issues>
    <issue id="Fake"><location file="src/main/java/Baselined.java" line="1"/></issue>
    <issue id="Fake"><location file="build.gradle" line="1"/></issue>
</issues>
EOF
    cat > example-app/lint-baseline.xml <<'EOF'
<issues>
    <issue id="Fake"><location file="src/main/res/baselined.xml" line="1"/></issue>
</issues>
EOF
    echo "class Baselined {}" > backtrace-library/src/main/java/Baselined.java
    echo "class Unbaselined {}" > backtrace-library/src/main/java/Unbaselined.java
    echo "<resources/>" > example-app/src/main/res/baselined.xml
    echo "// build" > backtrace-library/build.gradle
    git add -A && git commit -qm base

    run_checker() { scripts/check_lint_baseline_scope.sh HEAD~1 > /dev/null 2>&1; }
    failures=0

    # 1. No source changes at all: pass.
    git commit -q --allow-empty -m noop
    run_checker || { echo "FAIL: clean tree rejected" >&2; failures=1; }

    # 2. Changed source that is not baselined: pass.
    echo "class Unbaselined { int x; }" > backtrace-library/src/main/java/Unbaselined.java
    git commit -qam unbaselined
    run_checker || { echo "FAIL: unbaselined source change rejected" >&2; failures=1; }

    # 3. Changed baselined Java source: fail.
    echo "class Baselined { int x; }" > backtrace-library/src/main/java/Baselined.java
    git commit -qam baselined-java
    if run_checker; then echo "FAIL: baselined Java change accepted" >&2; failures=1; fi

    # 4. Changed baselined XML source: fail.
    echo "<resources><bool name=\"x\">true</bool></resources>" > example-app/src/main/res/baselined.xml
    git commit -qam baselined-xml
    if run_checker; then echo "FAIL: baselined XML change accepted" >&2; failures=1; fi

    # 5. Changed build.gradle stays allowed even though it appears in a baseline.
    echo "// build v2" > backtrace-library/build.gradle
    git commit -qam gradle
    run_checker || { echo "FAIL: build.gradle change rejected" >&2; failures=1; }

    # 6. Missing baseline is tolerated explicitly (skipped, not an error).
    git rm -q example-app/lint-baseline.xml
    echo "<resources/>" > example-app/src/main/res/other.xml
    git add -A && git commit -qm no-baseline
    run_checker || { echo "FAIL: missing baseline treated as an error" >&2; failures=1; }

    exit "$failures"
)
sub_status=$?
set -e

if [ "$sub_status" -ne 0 ]; then
    echo "Lint baseline-scope self-test FAILED." >&2
    exit 1
fi

echo "Lint baseline-scope self-test passed: 6 scenarios behaved as required."
