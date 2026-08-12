#!/usr/bin/env bash
set -euo pipefail

# Cold-start Perfetto capture around the Backtrace native-initialization trace section
# (BacktraceQualification#tryEnableNativeIntegration). Captures a baseline package and, when
# --with-fixture is set, a large-central-directory build of the example app, so the analyzer can
# prove initialization cost does not scale with APK ZIP entry count.
#
# Usage:
#   scripts/capture_native_startup_perfetto.sh \
#     --package <package> --activity <activity> --iterations 10 --output <dir> [--with-fixture]
#
# The raw .perfetto-trace files are retained; the analyzer runs only when a pinned
# trace_processor_shell is provided through TRACE_PROCESSOR_SHELL (never downloaded as "latest").

package=""
activity=".NativeStartupQualificationActivity"
iterations=10
output=""
with_fixture=0

while [ $# -gt 0 ]; do
    case "$1" in
        --package) package="$2"; shift 2 ;;
        --activity) activity="$2"; shift 2 ;;
        --iterations) iterations="$2"; shift 2 ;;
        --output) output="$2"; shift 2 ;;
        --with-fixture) with_fixture=1; shift ;;
        *) echo "Unknown argument: $1" >&2; exit 2 ;;
    esac
done

if [ -z "$package" ] || [ -z "$output" ]; then
    echo "--package and --output are required" >&2
    exit 2
fi
if [ "$iterations" -lt 10 ]; then
    echo "At least 10 iterations are required for stable percentiles" >&2
    exit 2
fi

mkdir -p "$output"
fixture_dir="example-app/build/native-startup-fixture/assets"

cleanup_fixture() {
    rm -rf "example-app/build/native-startup-fixture"
}
trap cleanup_fixture EXIT

{
    echo "model=$(adb shell getprop ro.product.model | tr -d '\r')"
    echo "sdk=$(adb shell getprop ro.build.version.sdk | tr -d '\r')"
    echo "abi=$(adb shell getprop ro.product.cpu.abi | tr -d '\r')"
    echo "page_size=$(adb shell getconf PAGE_SIZE | tr -d '\r')"
} | tee "$output/device-facts.txt"

capture_variant() {
    local variant="$1"
    local iteration
    for iteration in $(seq 1 "$iterations"); do
        local trace_file="$output/$variant-$iteration.perfetto-trace"
        adb shell am force-stop "$package"
        sleep 1

        adb shell perfetto -o /data/misc/perfetto-traces/qualification.perfetto-trace \
            -t 20s --app "$package" sched freq idle am wm view binder_driver &
        local perfetto_pid=$!
        sleep 2

        adb shell am start -n "$package/$activity" -W > /dev/null
        wait "$perfetto_pid" || true
        adb pull /data/misc/perfetto-traces/qualification.perfetto-trace "$trace_file" > /dev/null
    done
}

echo "== baseline capture =="
capture_variant baseline

if [ "$with_fixture" = "1" ]; then
    echo "== large-central-directory fixture capture =="
    python3 scripts/generate_native_startup_fixture.py \
        --entries 10000 --bytes-per-entry 1 --output "$fixture_dir"
    ./gradlew :example-app:assembleDebug -PnativeStartupFixtureAssets="$PWD/$fixture_dir"
    apk="example-app/build/outputs/apk/debug/example-app-debug.apk"
    python3 - "$apk" "$output" <<'PY'
import sys
import zipfile

apk, output = sys.argv[1], sys.argv[2]
count = len(zipfile.ZipFile(apk).namelist())
with open(output + "/fixture-apk-entry-count.txt", "w") as record:
    record.write(str(count) + "\n")
print("fixture APK entry count:", count)
PY
    adb install -r "$apk" > /dev/null
    capture_variant fixture
fi

if [ -n "${TRACE_PROCESSOR_SHELL:-}" ] && [ -x "${TRACE_PROCESSOR_SHELL}" ]; then
    python3 scripts/analyze_native_startup_trace.py \
        --trace-processor "$TRACE_PROCESSOR_SHELL" \
        --traces "$output" \
        --output "$output/native-startup-summary.json"
else
    echo "TRACE_PROCESSOR_SHELL not set; raw traces retained in $output, analyzer skipped."
fi

echo "Perfetto capture complete: $output"
