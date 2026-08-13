#!/usr/bin/env bash
set -euo pipefail

# Hardware release gate for allocated devices (modern arm64 split installs, real 32-bit processes
# on 64-bit hardware, physical 16 KB devices). Fails clearly when no device is attached or a
# prerequisite is missing; never silently passes. Run from the repository root.
#
# Process bitness is derived from the requested ABI — arm64-v8a is a 64-bit process and
# armeabi-v7a is a 32-bit process — so the two advertised scenarios cannot be misconfigured:
#
#   # 64-bit qualification on modern arm64 hardware
#   scripts/run_native_release_device_gate.sh --abi arm64-v8a --require-device-64-bit true
#
#   # 32-bit process on 64-bit-capable hardware (mixed-bitness qualification)
#   scripts/run_native_release_device_gate.sh --abi armeabi-v7a --require-device-64-bit true
#
# Optional: --require-page-size 4096|16384 (empty skips). Ingestion is mandatory: the process-ABI
# verification reads service-reported facts out of the ingestion evidence, so a secretless run
# cannot pass this gate — use REQUIRE_INGESTION=0 scripts/run_split_install_test.sh for that.
# BUNDLETOOL_JAR must point at the pinned bundletool (default: ./bundletool.jar).

readonly BUNDLETOOL_1_17_2_SHA256="2d4ad908faea64047c1cc9cb747e6aa667c6ab192e09607bd16b67246a8cd6ae"

abi=""
require_device_64=""
require_page_size=""
require_ingestion="true"

while [ $# -gt 0 ]; do
    case "$1" in
        --abi) abi="$2"; shift 2 ;;
        --require-device-64-bit) require_device_64="$2"; shift 2 ;;
        --require-page-size) require_page_size="$2"; shift 2 ;;
        --require-ingestion) require_ingestion="$2"; shift 2 ;;
        *) echo "Unknown argument: $1" >&2; exit 2 ;;
    esac
done

if [ "$require_ingestion" != "true" ]; then
    echo "The hardware release gate requires ingestion (--require-ingestion true):" >&2
    echo "process-ABI verification reads service-reported facts from the ingestion evidence." >&2
    echo "For secretless checks use REQUIRE_INGESTION=0 scripts/run_split_install_test.sh." >&2
    exit 2
fi

case "$abi" in
    arm64-v8a) expect_64_bit_process=true ;;
    armeabi-v7a) expect_64_bit_process=false ;;
    "") echo "--abi is required (arm64-v8a or armeabi-v7a)" >&2; exit 2 ;;
    *) echo "Unsupported --abi '$abi' (arm64-v8a or armeabi-v7a)" >&2; exit 2 ;;
esac

# --- prerequisites: self-hosted runners carry no guarantees --------------------------------------
missing=0
require_tool() {
    if ! command -v "$1" > /dev/null 2>&1; then
        echo "Missing required tool: $1" >&2
        missing=1
    fi
}
require_tool java
require_tool adb
require_tool git
require_tool python3
require_tool keytool
require_tool sha256sum
if [ -z "${ANDROID_HOME:-}" ] || [ ! -d "$ANDROID_HOME" ]; then
    echo "ANDROID_HOME must point at an Android SDK" >&2
    missing=1
else
    build_tools_version=""
    if [ -d "$ANDROID_HOME/build-tools" ]; then
        build_tools_version="$(ls "$ANDROID_HOME/build-tools" | sort -V | tail -1)"
    fi
    if [ -z "$build_tools_version" ]; then
        echo "No build-tools installed under $ANDROID_HOME/build-tools" >&2
        missing=1
    else
        for tool in apksigner zipalign; do
            if [ ! -x "$ANDROID_HOME/build-tools/$build_tools_version/$tool" ]; then
                echo "Missing $tool in build-tools $build_tools_version" >&2
                missing=1
            fi
        done
    fi
fi
bundletool_jar="${BUNDLETOOL_JAR:-bundletool.jar}"
if [ ! -f "$bundletool_jar" ]; then
    echo "Missing bundletool jar: $bundletool_jar (set BUNDLETOOL_JAR or download the pinned release)" >&2
    missing=1
fi
if [ "$missing" -ne 0 ]; then
    exit 2
fi
actual_bundletool_sha256="$(sha256sum -- "$bundletool_jar")"
actual_bundletool_sha256="${actual_bundletool_sha256%% *}"
if [ "$actual_bundletool_sha256" != "$BUNDLETOOL_1_17_2_SHA256" ]; then
    echo "bundletool checksum mismatch: the hardware gate requires bundletool 1.17.2" >&2
    echo "Expected $BUNDLETOOL_1_17_2_SHA256, found $actual_bundletool_sha256" >&2
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
case ",$abilist," in
    *",$abi,"*) ;;
    *)
        echo "Device does not advertise requested ABI $abi in ro.product.cpu.abilist" >&2
        exit 1
        ;;
esac
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
# Process ABI and bitness are read from the service READY facts inside the instrumentation tests
# and land in the evidence, where they are asserted against the requested ABI below.
expected_ingestion=0
run_fatal=0
if [ "$require_ingestion" = "true" ]; then
    expected_ingestion=1
    run_fatal=1
fi

BUNDLETOOL_JAR="$bundletool_jar" \
REQUIRE_INGESTION="$expected_ingestion" \
RUN_FATAL="$run_fatal" \
EXPECTED_PAGE_SIZE="${require_page_size:-}" \
./scripts/run_split_install_test.sh

# --- process ABI/bitness verification from the evidence -----------------------------------------
python3 - "$abi" "$expect_64_bit_process" <<'PY'
import json
import sys

abi, expect_64 = sys.argv[1], sys.argv[2] == "true"
with open("native-report-evidence.json") as evidence_file:
    evidence = json.load(evidence_file)

device = evidence.get("device", {})
process_abi = device.get("process_abi")
if process_abi is None:
    raise SystemExit(
        "Evidence carries no service-reported process ABI; run with --require-ingestion true"
    )
if process_abi != abi:
    raise SystemExit(f"Expected a {abi} process, found {process_abi}")
is_64 = device.get("is_64_bit")
if is_64 is not expect_64:
    raise SystemExit(
        f"Expected process 64-bit={expect_64} for ABI {abi}, evidence says {is_64}"
    )
print(f"Process ABI/bitness verified: {process_abi} (64-bit={is_64})")
PY

echo "Hardware release gate passed for --abi $abi."
