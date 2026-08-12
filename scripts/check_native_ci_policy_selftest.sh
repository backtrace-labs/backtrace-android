#!/usr/bin/env bash
set -euo pipefail

# Self-test for check_native_ci_policy.sh: the real workflow must pass, and a sabotaged copy per
# rule must fail. Run from the repository root.

checker="scripts/check_native_ci_policy.sh"
workflow=".github/workflows/test.yml"
if [ ! -f "$checker" ] || [ ! -f "$workflow" ]; then
    echo "Self-test must run from the repository root" >&2
    exit 1
fi

sandbox="$(mktemp -d)"
trap 'rm -rf "$sandbox"' EXIT

failures=0

expect_pass() {
    if ! "$checker" "$1" > /dev/null 2>&1; then
        echo "FAIL: checker rejected $2" >&2
        failures=1
    fi
}

expect_fail() {
    if "$checker" "$1" > /dev/null 2>&1; then
        echo "FAIL: checker accepted a workflow with $2" >&2
        failures=1
    fi
}

expect_pass "$workflow" "the real workflow"

sabotage() {
    local name="$1"
    local python_expr="$2"
    local copy="$sandbox/$name.yml"
    python3 - "$workflow" "$copy" "$python_expr" <<'PY'
import sys

source, destination, expression = sys.argv[1], sys.argv[2], sys.argv[3]
text = open(source, encoding="utf-8").read()
text = eval(expression, {"text": text})
open(destination, "w", encoding="utf-8").write(text)
PY
    echo "$copy"
}

expect_fail "$(sabotage unpinned \
    "text.replace('reactivecircus/android-emulator-runner@a421e43855164a8197daf9d8d40fe71c6996bb0d', 'reactivecircus/android-emulator-runner@v2', 1)")" \
    "an unpinned action"

expect_fail "$(sabotage saucectl-latest \
    "text.replace('saucectl-version: 0.213.0', 'saucectl-version: latest')")" \
    "saucectl-version: latest"

expect_fail "$(sabotage prt \
    "text.replace('  pull_request:', '  pull_request_target:', 1)")" \
    "pull_request_target"

expect_fail "$(sabotage access-token \
    "text.replace('persist-credentials: false', 'token: \${{ secrets.ACCESS_TOKEN }}', 1)")" \
    "ACCESS_TOKEN in checkout"

expect_fail "$(sabotage ungated-secret \
    "text.replace(\"if: needs.policy.outputs.trusted == 'true'\", 'timeout-minutes: 59')")" \
    "secrets outside a trusted-gated job"

expect_fail "$(sabotage or-true \
    "text.replace('run: scripts/check_no_native_init_apk_zip_scan.sh', 'run: scripts/check_no_native_init_apk_zip_scan.sh || true', 1)")" \
    "|| true on a gate"

expect_fail "$(sabotage no-permissions \
    "text.replace('permissions:\\n  contents: read\\n', '')")" \
    "missing permissions: contents: read"

expect_fail "$(sabotage no-legacy \
    "text.replace('api-level: [21, 22]', 'api-level: [30]')")" \
    "missing API 21/22 lane"

expect_fail "$(sabotage no-16kb \
    "text.replace('google_apis_ps16k', 'google_apis')")" \
    "missing 16 KB runtime lane"

expect_fail "$(sabotage no-fatal \
    "text.replace('RUN_FATAL=1', 'RUN_FATAL=0').replace('run_16kb_native_qualification.sh', 'run_split_install_test.sh')")" \
    "missing fatal-process gate"

if [ "$failures" -ne 0 ]; then
    echo "CI policy self-test FAILED." >&2
    exit 1
fi

echo "CI policy self-test passed: real workflow accepted, 10 sabotaged variants rejected."
