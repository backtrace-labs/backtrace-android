#!/usr/bin/env bash
set -euo pipefail

# Play-style split-install qualification against the connected device/emulator, in explicit
# hard-gated phases:
#   1. build/install a device-specific APK set from the debug AAB (signed; certificates aligned)
#   2. assert an ABI configuration split is installed
#   3. resolver test (production dladdr()/metadata path resolution)
#   4. fresh-process failure-safety test (secretless)
#   5. REQUIRE_INGESTION=1: nonfatal, fatal+recovery (RUN_FATAL=1), and lifecycle ingestion gates
#   6. no selected instrumentation test may be skipped
#   7. optional device-fact assertions (EXPECTED_PAGE_SIZE, EXPECTED_PROCESS_ABI,
#      EXPECTED_DEVICE_SUPPORTS_64_BIT)
# Only diagnostic collection may use `|| true`; no test or ingestion gate does.
#
# Environment:
#   REQUIRE_INGESTION=0|1                 (default 1) gate backend ingestion phases
#   RUN_FATAL=0|1                         (default 1) include the fatal+recovery gate
#   EXPECTED_PAGE_SIZE=<int>              optional getconf PAGE_SIZE assertion
#   EXPECTED_PROCESS_ABI=<abi>            optional primary-ABI assertion
#   EXPECTED_DEVICE_SUPPORTS_64_BIT=0|1   optional 64-bit ABI list assertion
#   BUNDLETOOL_JAR, APKS_OUTPUT, DEBUG_KEYSTORE overrides as before

bundletool_jar="${BUNDLETOOL_JAR:-bundletool.jar}"
bundle="example-app/build/outputs/bundle/debug/example-app-debug.aab"
test_apk="example-app/build/outputs/apk/androidTest/debug/example-app-debug-androidTest.apk"
apks_output="${APKS_OUTPUT:-example-app-debug.apks}"
debug_keystore="${DEBUG_KEYSTORE:-$HOME/.android/debug.keystore}"
require_ingestion="${REQUIRE_INGESTION:-1}"
run_fatal="${RUN_FATAL:-1}"

runner="backtraceio.backtraceio.test/androidx.test.runner.AndroidJUnitRunner"

for artifact in "$bundletool_jar" "$bundle" "$test_apk"; do
    if [ ! -f "$artifact" ]; then
        echo "Missing required artifact: $artifact" >&2
        exit 1
    fi
done

# --- diagnostics (the only || true zone) --------------------------------------------------------
collect_logcat() {
    # DEBUG (native tombstones), libc (fatal signals), and ActivityManager (process deaths) are
    # required to tell a native crash from a system kill in a remote test process.
    adb logcat -d -s \
        BacktraceCrashHandlerRunner:V \
        Backtrace-Android:V \
        NativeQualEvidence:I \
        TestRunner:I \
        AndroidRuntime:E \
        DEBUG:V \
        libc:F \
        ActivityManager:I 2>/dev/null \
        | sed -E \
            -e 's#(token=)[A-Za-z0-9._-]+#\1[REDACTED]#g' \
            -e 's#https?://[^[:space:]"]+#[REDACTED_URL]#g' \
            -e 's#--annotation=[^[:space:]"]+#--annotation=[REDACTED]#g' \
            -e 's#--attachment=[^[:space:]"]+#--attachment=[REDACTED]#g' \
        > split-install-logcat.txt || true
}
trap collect_logcat EXIT

# --- signing ------------------------------------------------------------------------------------
if [ ! -f "$debug_keystore" ]; then
    mkdir -p "$(dirname "$debug_keystore")"
    keytool -genkeypair -keystore "$debug_keystore" -storepass android -alias androiddebugkey \
        -keypass android -keyalg RSA -keysize 2048 -validity 10000 \
        -dname "CN=Android Debug,O=Android,C=US"
fi

# --- phase 1: install ---------------------------------------------------------------------------
java -jar "$bundletool_jar" build-apks \
    --bundle="$bundle" \
    --output="$apks_output" \
    --connected-device \
    --overwrite \
    --ks="$debug_keystore" \
    --ks-pass=pass:android \
    --ks-key-alias=androiddebugkey \
    --key-pass=pass:android

java -jar "$bundletool_jar" install-apks --apks="$apks_output"

# --- phase 2: split present + device facts ------------------------------------------------------
adb shell pm path backtraceio.backtraceio | tee installed-package-paths.txt
grep -E 'split_config\.(x86_64|arm64_v8a|armeabi_v7a)\.apk' installed-package-paths.txt

device_sdk="$(adb shell getprop ro.build.version.sdk | tr -d '\r')"
device_abi="$(adb shell getprop ro.product.cpu.abi | tr -d '\r')"
device_abilist="$(adb shell getprop ro.product.cpu.abilist | tr -d '\r')"
device_abilist64="$(adb shell getprop ro.product.cpu.abilist64 | tr -d '\r')"
device_page_size="$(adb shell getconf PAGE_SIZE | tr -d '\r')"
{
    echo "sdk=$device_sdk"
    echo "abi=$device_abi"
    echo "abilist=$device_abilist"
    echo "abilist64=$device_abilist64"
    echo "page_size=$device_page_size"
    echo "model=$(adb shell getprop ro.product.model | tr -d '\r')"
} | tee device-facts.txt

# --- phase 7/8 style device assertions (early, they are cheap) ----------------------------------
if [ -n "${EXPECTED_PAGE_SIZE:-}" ] && [ "$device_page_size" != "$EXPECTED_PAGE_SIZE" ]; then
    echo "PAGE_SIZE mismatch: expected $EXPECTED_PAGE_SIZE, found $device_page_size" >&2
    exit 1
fi
if [ -n "${EXPECTED_PROCESS_ABI:-}" ] && [ "$device_abi" != "$EXPECTED_PROCESS_ABI" ]; then
    echo "Primary ABI mismatch: expected $EXPECTED_PROCESS_ABI, found $device_abi" >&2
    exit 1
fi
if [ "${EXPECTED_DEVICE_SUPPORTS_64_BIT:-}" = "1" ] && [ -z "$device_abilist64" ]; then
    echo "Device does not advertise a 64-bit ABI list" >&2
    exit 1
fi
if [ "${EXPECTED_DEVICE_SUPPORTS_64_BIT:-}" = "0" ] && [ -n "$device_abilist64" ]; then
    echo "Device unexpectedly advertises a 64-bit ABI list" >&2
    exit 1
fi

# A persistent device may carry NativeQualEvidence lines from a previous run; stale evidence
# must never satisfy this run's gates or contaminate its provenance.
adb logcat -c 2>/dev/null || true

# --- instrumentation APK, certificate-aligned ---------------------------------------------------
build_tools_version="$(ls "$ANDROID_HOME/build-tools" | sort -V | tail -1)"
apksigner="$ANDROID_HOME/build-tools/$build_tools_version/apksigner"
signed_test_apk="${TMPDIR:-/tmp}/example-app-debug-androidTest-resigned.apk"
cp "$test_apk" "$signed_test_apk"
"$apksigner" sign --ks "$debug_keystore" --ks-pass pass:android \
    --ks-key-alias androiddebugkey --key-pass pass:android "$signed_test_apk"

adb install -r "$signed_test_apk"

# Each selected test must PASS (status code 0), not be skipped by its assumptions (-4) or fail
# (-1/-2); am instrument itself always exits 0, so gate on the raw status stream and test count.
run_instrumentation_test() {
    local selector="$1"
    local output="$2"
    local expected_tests="$3"
    local timeout_arg="${4:-}"

    adb shell am instrument -w -r \
        -e class "$selector" \
        $timeout_arg \
        "$runner" | tee "$output"

    grep -q "INSTRUMENTATION_STATUS_CODE: 0" "$output"
    if grep -qE "INSTRUMENTATION_STATUS_CODE: -[0-9]" "$output"; then
        echo "Instrumentation failed or was skipped: $selector (see $output)" >&2
        return 1
    fi
    grep -q "OK (${expected_tests} test" "$output"
}

# --- phase 3: resolver --------------------------------------------------------------------------
run_instrumentation_test \
    "backtraceio.backtraceio.SplitInstallNativeResolutionTest" \
    "split-resolver-output.txt" \
    1

# --- phase 4: fresh-process failure safety (secretless) -----------------------------------------
run_instrumentation_test \
    "backtraceio.backtraceio.NativeFreshProcessSafetyTest" \
    "fresh-process-safety-output.txt" \
    1

# --- phase 4b: assertion-policy sabotage suite (secretless) -------------------------------------
# The duplicate/collapsed-group/message/error-type policies guard every ingestion gate; they must
# execute on fork and Dependabot changes too, so they run here rather than only in trusted lanes.
# The expected count is hard-gated: silently dropped sabotage tests must fail this phase.
run_instrumentation_test \
    "backtraceio.backtraceio.CoronerNativeReportAssertionsTest" \
    "assertion-sabotage-output.txt" \
    13

# Sanitization gate: the safety scenarios deliberately throw sentinel-bearing failures in a real
# process; a raw sentinel in UNfiltered logcat means the sanitized-diagnostics contract regressed.
if adb logcat -d 2>/dev/null | grep -qE "SECRET_URL_TOKEN_SENTINEL|PRIVATE_CUSTOMER_SENTINEL"; then
    echo "Sanitized-diagnostics regression: a sensitive sentinel reached Logcat" >&2
    exit 1
fi

# --- phase 5: ingestion gates -------------------------------------------------------------------
if [ "$require_ingestion" = "1" ]; then
    run_instrumentation_test \
        "backtraceio.backtraceio.NativeSplitProcessIntegrationTest#nonfatalDumpFromDedicatedProcessIsIngestedExactlyOnce" \
        "split-nonfatal-output.txt" \
        1

    if [ "$run_fatal" = "1" ]; then
        run_instrumentation_test \
            "backtraceio.backtraceio.NativeSplitProcessIntegrationTest#fatalCrashIsRecoveredAndIngestedExactlyOnce" \
            "split-fatal-output.txt" \
            1
    fi

    run_instrumentation_test \
        "backtraceio.backtraceio.NativeSplitProcessIntegrationTest#disableAndReEnableRestartsUploads" \
        "split-lifecycle-output.txt" \
        1
else
    echo "Ingestion gates skipped by policy (REQUIRE_INGESTION=0); resolver and safety gates were mandatory."
fi

# --- evidence -----------------------------------------------------------------------------------
# Provenance: a pull_request checkout is a synthetic merge commit, so HEAD is NOT the PR head.
# Record both SHAs; PR_HEAD_SHA is provided by CI, and HEAD^2 of a merge checkout is the PR head.
tested_merge_sha="$(git rev-parse HEAD 2>/dev/null || echo unknown)"
if [ -n "${PR_HEAD_SHA:-}" ]; then
    pr_head_sha="$PR_HEAD_SHA"
elif [ "${GITHUB_ACTIONS:-}" = "true" ] && git rev-parse -q --verify HEAD^2 > /dev/null 2>&1; then
    pr_head_sha="$(git rev-parse HEAD^2)"
else
    pr_head_sha="$tested_merge_sha"
fi
adb logcat -d -s NativeQualEvidence:I 2>/dev/null > native-qual-evidence-lines.txt || true
python3 - "$pr_head_sha" "$tested_merge_sha" <<'PY'
import json
import re
import sys

evidence = {
    "pr_head_sha": sys.argv[1],
    "tested_merge_sha": sys.argv[2],
    "device": {},
    "install": {"package_paths": []},
}
try:
    with open("device-facts.txt") as facts:
        for line in facts:
            key, _, value = line.strip().partition("=")
            if key == "sdk":
                evidence["device"]["sdk"] = int(value)
            elif key == "page_size":
                evidence["device"]["page_size"] = int(value)
            elif key == "abilist":
                evidence["device"]["supported_abis"] = [a for a in value.split(",") if a]
            elif key == "abi":
                evidence["device"]["primary_abi"] = value
            elif key == "abilist64":
                evidence["device"]["is_64_bit"] = bool(value)
except OSError:
    pass
try:
    with open("installed-package-paths.txt") as paths:
        evidence["install"]["package_paths"] = [
            line.strip().replace("package:", "") for line in paths if line.strip()
        ]
except OSError:
    pass
try:
    with open("native-qual-evidence-lines.txt") as lines:
        for line in lines:
            match = re.search(r"\{.*\}", line)
            if not match:
                continue
            entry = json.loads(match.group(0))
            phase = entry.pop("phase", None)
            if phase:
                evidence[phase] = entry
except OSError:
    pass

# The service READY facts carry the real process ABI/bitness and the resolved handler path;
# prefer them over device props and surface the handler path at the top level.
nonfatal = evidence.get("nonfatal", {})
if isinstance(nonfatal, dict) and nonfatal.get("process_abi"):
    evidence["device"]["process_abi"] = nonfatal.pop("process_abi")
    if "process_is_64_bit" in nonfatal:
        evidence["device"]["is_64_bit"] = nonfatal.pop("process_is_64_bit")
if isinstance(nonfatal, dict) and nonfatal.get("handler_path"):
    evidence["handler_path"] = nonfatal.pop("handler_path")

with open("native-report-evidence.json", "w") as output:
    json.dump(evidence, output, indent=2)
print("wrote native-report-evidence.json")
PY
rm -f native-qual-evidence-lines.txt

echo "Split-install qualification passed for the selected phases."
