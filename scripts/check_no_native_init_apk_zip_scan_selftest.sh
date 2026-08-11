#!/usr/bin/env bash
set -euo pipefail

# Self-test for check_no_native_init_apk_zip_scan.sh. Copies the guard and every guarded root into
# a sandbox, then proves the guard passes on the clean tree, fails on every forbidden archive-reader
# form, and fails closed when a guarded root disappears. Run from the repository root.

guard="scripts/check_no_native_init_apk_zip_scan.sh"

if [ ! -f "$guard" ]; then
    echo "Self-test must run from the repository root; missing $guard" >&2
    exit 1
fi

# Read the guarded roots from the guard script itself so the two cannot drift apart.
roots=()
while IFS= read -r line; do
    roots+=("$line")
done < <(sed -n '/^roots=(/,/^)/p' "$guard" | sed -n 's/^[[:space:]]*"\(.*\)"$/\1/p')

if [ "${#roots[@]}" -eq 0 ]; then
    echo "Self-test could not parse any guarded roots from $guard" >&2
    exit 1
fi

sandbox="$(mktemp -d)"
trap 'rm -rf "$sandbox"' EXIT

mkdir -p "$sandbox/scripts"
cp "$guard" "$sandbox/$guard"
for root in "${roots[@]}"; do
    mkdir -p "$sandbox/$(dirname "$root")"
    cp -R "$root" "$sandbox/$root"
done

run_guard() {
    (cd "$sandbox" && "./$guard" > /dev/null 2>&1)
}

failures=0

if ! run_guard; then
    echo "FAIL: guard rejected a clean tree" >&2
    failures=1
fi

# One representative file inside a guarded directory root receives each probe.
probe_target="$sandbox/backtrace-library/src/main/java/backtraceio/library/models/nativeHandler/CrashHandlerConfiguration.java"
probes=(
    "import java.util.zip.ZipFile;"
    "import java.util.zip.ZipEntry;"
    "import java.util.zip.ZipInputStream;"
    "import java.util.jar.JarFile;"
    "import java.util.jar.JarInputStream;"
    "ZipFile archive = new ZipFile(apkPath);"
    "Object archive = new java.util.zip.ZipFile(apkPath);"
    "ZipInputStream stream = new ZipInputStream(input);"
    "JarFile archive = new JarFile(apkPath);"
    "Object archive = new java.util.jar.JarFile(apkPath);"
    "JarInputStream stream = new JarInputStream(input);"
    "boolean found = apkContains(apkPath, entry);"
)

for probe in "${probes[@]}"; do
    printf '\n// %s\n' "$probe" >> "$probe_target"
    if run_guard; then
        echo "FAIL: guard did not reject: $probe" >&2
        failures=1
    fi
    # Strip the two appended lines.
    tail_lines=$(($(wc -l < "$probe_target") - 2))
    head -n "$tail_lines" "$probe_target" > "$probe_target.tmp" && mv "$probe_target.tmp" "$probe_target"
done

if ! run_guard; then
    echo "FAIL: guard rejected the tree after probes were removed" >&2
    failures=1
fi

# Fail-closed: a missing guarded root must fail, not silently pass.
mv "$sandbox/${roots[0]}" "$sandbox/${roots[0]}.moved"
if run_guard; then
    echo "FAIL: guard passed with a missing guarded root: ${roots[0]}" >&2
    failures=1
fi
mv "$sandbox/${roots[0]}.moved" "$sandbox/${roots[0]}"

if [ "$failures" -ne 0 ]; then
    echo "Guard self-test FAILED." >&2
    exit 1
fi

echo "Guard self-test passed: clean tree accepted, ${#probes[@]} forbidden forms rejected, missing-root fails closed."
