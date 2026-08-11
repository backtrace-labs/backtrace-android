#!/usr/bin/env bash
set -euo pipefail

# Production sources involved in native crash-handler initialization.
# Opening or parsing the host APK on this path parses the ZIP central directory on the calling thread and reintroduces the cold-start ANR (ZipFile$Source.initCEN) this guard exists to prevent.
roots=(
    "backtrace-library/src/main/java/backtraceio/library/BacktraceDatabase.java"
    "backtrace-library/src/main/java/backtraceio/library/base"
    "backtrace-library/src/main/java/backtraceio/library/common/AbiHelper.java"
    "backtrace-library/src/main/java/backtraceio/library/models/nativeHandler"
    "backtrace-library/src/main/java/backtraceio/library/services/BacktraceCrashHandlerRunner.java"
)

missing=0
for root in "${roots[@]}"; do
    if [ ! -e "$root" ]; then
        echo "Guard target is missing, update scripts/$(basename "$0"): $root" >&2
        missing=1
    fi
done
if [ "$missing" -ne 0 ]; then
    exit 1
fi

forbidden='java\.util\.zip\.(ZipFile|ZipEntry|ZipInputStream)|java\.util\.jar\.(JarFile|JarInputStream)|new[[:space:]]+(java\.util\.zip\.)?(ZipFile|ZipInputStream)|new[[:space:]]+(java\.util\.jar\.)?(JarFile|JarInputStream)|apkContains'

set +e
matches=$(grep -RInE --include='*.java' "$forbidden" "${roots[@]}")
status=$?
set -e

# grep exits 0 on match, 1 on no match, >1 on error. Only 1 is a pass.
if [ "$status" -gt 1 ]; then
    echo "Guard failed to scan native initialization sources (grep exit $status)." >&2
    exit 1
fi

if [ "$status" -eq 0 ]; then
    echo "$matches"
    echo "Native integration must not open or inspect host APK ZIP files." >&2
    exit 1
fi

echo "Verified: native initialization contains no APK ZIP central-directory scan."
