#!/usr/bin/env bash
set -euo pipefail

# Cold-start Perfetto capture around the Backtrace native-initialization trace section
# (BacktraceQualification#tryEnableNativeIntegration). Captures a baseline package and, when
# --with-fixture is set, a large-central-directory build of the example app, so the analyzer can
# prove initialization cost does not scale with APK ZIP entry count.
#
# Fail-closed: every iteration must produce (1) an app result JSON proving
# tryEnableNativeIntegration() ran and returned true, and (2) a fresh, non-empty trace. A failed
# Perfetto capture fails the run. The analyzer requires exactly one trace-section sample per
# captured iteration.
#
# Usage:
#   scripts/capture_native_startup_perfetto.sh \
#     --package <package> --activity <activity> --iterations 10 --output <dir> \
#     [--with-fixture] [--capture-only]
#
# Qualification requires a pinned trace_processor_shell via TRACE_PROCESSOR_SHELL (never
# downloaded as "latest"). Without one, the run must be explicitly requested as --capture-only:
# raw traces are retained but the run is NOT a completed qualification.

package=""
activity=".NativeStartupQualificationActivity"
iterations=10
output=""
with_fixture=0
capture_only=0
result_file="files/native-startup-qualification.json"
remote_trace="/data/misc/perfetto-traces/qualification.perfetto-trace"

while [ $# -gt 0 ]; do
    case "$1" in
        --package) package="$2"; shift 2 ;;
        --activity) activity="$2"; shift 2 ;;
        --iterations) iterations="$2"; shift 2 ;;
        --output) output="$2"; shift 2 ;;
        --with-fixture) with_fixture=1; shift ;;
        --capture-only) capture_only=1; shift ;;
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
if [ "$capture_only" = "0" ] && { [ -z "${TRACE_PROCESSOR_SHELL:-}" ] || [ ! -x "${TRACE_PROCESSOR_SHELL:-}" ]; }; then
    echo "TRACE_PROCESSOR_SHELL is not an executable pinned trace processor." >&2
    echo "Provide one for qualification, or pass --capture-only to record raw traces only." >&2
    exit 2
fi

mkdir -p "$output"
# Stale traces from a previous run must never pass as this run's evidence: the analyzer globs the
# whole directory, so a reused directory could satisfy --expected-samples with old captures.
if ls "$output"/*.perfetto-trace > /dev/null 2>&1; then
    echo "Output directory $output already contains traces; use a fresh directory per run." >&2
    exit 2
fi

# Pin the baseline APK identity: baseline iterations must measure a known, freshly installed
# build, not whatever happens to be on the device (for example a leftover fixture install).
baseline_apk="example-app/build/outputs/apk/debug/example-app-debug.apk"
test -f "$baseline_apk"
baseline_apk_copy="$output/baseline.apk"
cp "$baseline_apk" "$baseline_apk_copy"
python3 - "$baseline_apk_copy" "$output" <<'PY'
import sys
import zipfile

apk, output = sys.argv[1], sys.argv[2]
count = len(zipfile.ZipFile(apk).namelist())
with open(output + "/baseline-apk-entry-count.txt", "w") as record:
    record.write(str(count) + "\n")
print("baseline APK entry count:", count)
PY
adb install -r "$baseline_apk_copy" > /dev/null

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

        # Fresh state per iteration: a stale result or trace must never satisfy this iteration.
        adb shell am force-stop "$package"
        adb shell run-as "$package" rm -f "$result_file"
        adb shell rm -f "$remote_trace"
        sleep 1

        adb shell perfetto -o "$remote_trace" \
            -t 20s --app "$package" sched freq idle am wm view binder_driver &
        local perfetto_pid=$!
        sleep 2

        adb shell am start -n "$package/$activity" -W > /dev/null

        # A failed Perfetto capture is a hard failure, not a skipped iteration.
        if ! wait "$perfetto_pid"; then
            echo "$variant iteration $iteration: perfetto capture failed" >&2
            exit 1
        fi

        # The activity result must prove the production init path ran and enabled native capture;
        # a missing submission URL or a false return must fail the iteration.
        local result_json
        if ! result_json="$(adb shell run-as "$package" cat "$result_file" 2>/dev/null)" \
                || [ -z "$result_json" ]; then
            echo "$variant iteration $iteration: app result JSON is missing" >&2
            exit 1
        fi
        if ! echo "$result_json" | grep -q '"native_enabled":true'; then
            echo "$variant iteration $iteration: native integration did not enable: $result_json" >&2
            exit 1
        fi

        adb pull "$remote_trace" "$trace_file" > /dev/null
        if [ ! -s "$trace_file" ]; then
            echo "$variant iteration $iteration: pulled trace is missing or empty" >&2
            exit 1
        fi
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
    python3 - "$apk" "$output" <<'PY'
import sys

apk, output = sys.argv[1], sys.argv[2]
with open(output + "/baseline-apk-entry-count.txt") as record:
    baseline_count = int(record.read().strip())
with open(output + "/fixture-apk-entry-count.txt") as record:
    fixture_count = int(record.read().strip())
if fixture_count <= baseline_count:
    raise SystemExit(
        f"Fixture APK ({fixture_count} entries) is not larger than baseline ({baseline_count});"
        " the large-central-directory build did not take effect"
    )
PY
    adb install -r "$apk" > /dev/null
    capture_variant fixture
    # Leave the device in the baseline state so a later run cannot capture its baseline from the
    # leftover 10k-entry fixture install.
    adb install -r "$baseline_apk_copy" > /dev/null
fi

if [ "$capture_only" = "1" ]; then
    echo "CAPTURE-ONLY MODE: raw traces retained in $output."
    echo "This run is NOT a completed startup qualification; analyze with a pinned trace processor."
    exit 0
fi

python3 scripts/analyze_native_startup_trace.py \
    --trace-processor "$TRACE_PROCESSOR_SHELL" \
    --traces "$output" \
    --expected-samples "$iterations" \
    --output "$output/native-startup-summary.json"

echo "Perfetto startup qualification complete: $output"
