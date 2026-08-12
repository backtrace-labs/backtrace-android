#!/usr/bin/env bash
set -euo pipefail

# Hardware release gate for allocated devices (modern arm64 split installs, real 32-bit processes
# on 64-bit hardware, physical 16 KB devices). Fails clearly when no device is attached; never
# silently passes. Run from the repository root.
#
# Usage:
#   scripts/run_native_release_device_gate.sh \
#     --abi arm64-v8a|armeabi-v7a \
#     --require-device-64-bit true|false \
#     --require-process-64-bit true|false \
#     --require-page-size 4096|16384 \
#     --require-ingestion true

abi=""
require_device_64=""
require_process_64=""
require_page_size=""
require_ingestion="true"

while [ $# -gt 0 ]; do
    case "$1" in
        --abi) abi="$2"; shift 2 ;;
        --require-device-64-bit) require_device_64="$2"; shift 2 ;;
        --require-process-64-bit) require_process_64="$2"; shift 2 ;;
        --require-page-size) require_page_size="$2"; shift 2 ;;
        --require-ingestion) require_ingestion="$2"; shift 2 ;;
        *) echo "Unknown argument: $1" >&2; exit 2 ;;
    esac
done

if [ -z "$abi" ]; then
    echo "--abi is required" >&2
    exit 2
fi

# A missing device must be a loud failure, not a silent pass.
device_count="$(adb devices | awk 'NR>1 && $2=="device"' | wc -l | tr -d ' ')"
if [ "$device_count" -lt 1 ]; then
    echo "Hardware release gate requires an attached device; none found." >&2
    exit 1
fi

# --- device facts -------------------------------------------------------------------------------
abilist="$(adb shell getprop ro.product.cpu.abilist | tr -d '\r')"
abilist32="$(adb shell getprop ro.product.cpu.abilist32 | tr -d '\r')"
abilist64="$(adb shell getprop ro.product.cpu.abilist64 | tr -d '\r')"
page_size="$(adb shell getconf PAGE_SIZE | tr -d '\r')"
sdk="$(adb shell getprop ro.build.version.sdk | tr -d '\r')"
model="$(adb shell getprop ro.product.model | tr -d '\r')"
{
    echo "sdk=$sdk"
    echo "model=$model"
    echo "abilist=$abilist"
    echo "abilist32=$abilist32"
    echo "abilist64=$abilist64"
    echo "page_size=$page_size"
} | tee device-facts.txt

if [ "$require_device_64" = "true" ] && [ -z "$abilist64" ]; then
    echo "Device does not advertise a 64-bit ABI list" >&2
    exit 1
fi
if [ -n "$require_page_size" ] && [ "$page_size" != "$require_page_size" ]; then
    echo "PAGE_SIZE mismatch: expected $require_page_size, found $page_size" >&2
    exit 1
fi

# --- ABI-restricted qualification build ---------------------------------------------------------
./gradlew -PqualificationAbi="$abi" \
    :example-app:bundleDebug \
    :example-app:assembleDebug \
    :example-app:assembleDebugAndroidTest

# The split gate performs install, resolver, safety, and (when required) the ingestion phases.
# Process ABI and bitness are read from the service READY facts inside the instrumentation tests;
# the expected process ABI is additionally asserted here from the qualification build restriction.
expected_ingestion=0
run_fatal=0
if [ "$require_ingestion" = "true" ]; then
    expected_ingestion=1
    run_fatal=1
fi

REQUIRE_INGESTION="$expected_ingestion" \
RUN_FATAL="$run_fatal" \
EXPECTED_PAGE_SIZE="${require_page_size:-}" \
./scripts/run_split_install_test.sh

# --- process bitness verification from the evidence ---------------------------------------------
python3 - "$abi" "$require_process_64" <<'PY'
import json
import sys

abi, require_64 = sys.argv[1], sys.argv[2]
with open("native-report-evidence.json") as evidence_file:
    evidence = json.load(evidence_file)

device = evidence.get("device", {})
process_abi = device.get("process_abi")
if process_abi is None:
    raise SystemExit(
        "Evidence carries no service-reported process ABI; run with --require-ingestion true"
    )
if abi == "armeabi-v7a" and process_abi not in (None, "armeabi-v7a"):
    raise SystemExit(f"Expected an armeabi-v7a process, found {process_abi}")
if require_64 == "true" and process_abi and "64" not in process_abi:
    raise SystemExit(f"Expected a 64-bit process ABI, found {process_abi}")
if require_64 == "false" and process_abi and "64" in process_abi:
    raise SystemExit(f"Expected a 32-bit process ABI, found {process_abi}")
print("Process ABI/bitness verified:", process_abi)
PY

echo "Hardware release gate passed for --abi $abi."
