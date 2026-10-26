#!/usr/bin/env bash
set -euo pipefail

sdkmanager="${ANDROID_HOME:?ANDROID_HOME is not set}/cmdline-tools/latest/bin/sdkmanager"

for attempt in 1 2 3; do
    if "$sdkmanager" --install "cmake;3.31.6" "ndk;28.2.13676358"; then
        exit 0
    fi
    echo "Android SDK package install failed (attempt $attempt of 3)." >&2
    sleep 15
done
exit 1
