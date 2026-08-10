#!/usr/bin/env bash
set -euo pipefail

resolver="backtrace-library/src/main/java/backtraceio/library/models/nativeHandler/CrashHandlerConfiguration.java"
forbidden='java\.util\.zip\.(ZipFile|ZipEntry)|new[[:space:]]+ZipFile|apkContains'

if grep -nE "$forbidden" "$resolver"; then
    echo "Native integration must not open or inspect host APK ZIP files." >&2
    exit 1
fi

echo "Verified: native integration contains no APK ZIP central-directory scan."
