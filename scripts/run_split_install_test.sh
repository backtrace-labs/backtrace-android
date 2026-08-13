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

case "$require_ingestion" in
    0|1) ;;
    *) echo "REQUIRE_INGESTION must be 0 or 1" >&2; exit 2 ;;
esac
case "$run_fatal" in
    0|1) ;;
    *) echo "RUN_FATAL must be 0 or 1" >&2; exit 2 ;;
esac

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
adb logcat -c
native_evidence_after_clear="$(adb logcat -d -s NativeQualEvidence:I)"
if printf '%s\n' "$native_evidence_after_clear" | grep -q "NativeQualEvidence"; then
    echo "NativeQualEvidence log entries remain after clearing Logcat; refusing stale evidence." >&2
    exit 1
fi

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
raw_logcat="$(adb logcat -d)"
if printf '%s\n' "$raw_logcat" \
        | grep -qE "SECRET_URL_TOKEN_SENTINEL|SENSITIVE_ATTRIBUTE_SENTINEL"; then
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
# Provenance: a pull_request checkout can be a synthetic merge commit, so HEAD is not necessarily
# the source head. Record the source and exact tested checkout separately, and include the merge
# SHA only when the tested checkout is a merge whose second parent is the source head.
tested_checkout_sha="$(git rev-parse HEAD)"
pull_request_merge_sha=""
if [ -n "${PR_HEAD_SHA:-}" ]; then
    source_head_sha="$PR_HEAD_SHA"
    if ! printf '%s\n' "$source_head_sha" | grep -Eq '^([0-9a-f]{40}|[0-9a-f]{64})$'; then
        echo "PR_HEAD_SHA is not a full lowercase object ID: $PR_HEAD_SHA" >&2
        exit 1
    fi
    if [ "$source_head_sha" != "$tested_checkout_sha" ]; then
        # Read the merge object's raw parent hashes instead of resolving HEAD^2. GitHub's default
        # depth-one PR checkout has the merge object but not its parent objects.
        merge_parent_count="$(git cat-file -p HEAD | awk '$1 == "parent" { count++ } END { print count + 0 }')"
        merge_source_sha="$(git cat-file -p HEAD | awk '$1 == "parent" { count++; if (count == 2) print $2 }')"
        if [ "$merge_parent_count" != "2" ] || [ -z "$merge_source_sha" ]; then
            echo "Tested checkout differs from PR_HEAD_SHA but is not a two-parent merge commit." >&2
            exit 1
        fi
        if [ "$merge_source_sha" != "$source_head_sha" ]; then
            echo "Tested merge second parent does not match PR_HEAD_SHA." >&2
            exit 1
        fi
        pull_request_merge_sha="$tested_checkout_sha"
    fi
elif [ "${GITHUB_ACTIONS:-}" = "true" ]; then
    merge_parent_count="$(git cat-file -p HEAD | awk '$1 == "parent" { count++ } END { print count + 0 }')"
    if [ "$merge_parent_count" = "2" ]; then
        source_head_sha="$(git cat-file -p HEAD | awk '$1 == "parent" { count++; if (count == 2) print $2 }')"
        pull_request_merge_sha="$tested_checkout_sha"
    else
        source_head_sha="$tested_checkout_sha"
    fi
else
    source_head_sha="$tested_checkout_sha"
fi
adb logcat -d -s NativeQualEvidence:I > native-qual-evidence-lines.txt
python3 - \
    "$source_head_sha" \
    "$tested_checkout_sha" \
    "$pull_request_merge_sha" \
    "$require_ingestion" \
    "$run_fatal" <<'PY'
import json
import re
import sys

source_head_sha, tested_checkout_sha, pull_request_merge_sha = sys.argv[1:4]
require_ingestion = sys.argv[4] == "1"
run_fatal = sys.argv[5] == "1"

evidence = {
    "source_head_sha": source_head_sha,
    "tested_checkout_sha": tested_checkout_sha,
    "device": {},
    "install": {"package_paths": []},
}
if pull_request_merge_sha:
    evidence["pull_request_merge_sha"] = pull_request_merge_sha

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
with open("installed-package-paths.txt") as paths:
    evidence["install"]["package_paths"] = [
        line.strip().replace("package:", "") for line in paths if line.strip()
    ]

device = evidence["device"]
if not (
    isinstance(device.get("sdk"), int)
    and device["sdk"] > 0
    and isinstance(device.get("page_size"), int)
    and device["page_size"] > 0
    and isinstance(device.get("supported_abis"), list)
    and device["supported_abis"]
    and all(isinstance(abi, str) and abi for abi in device["supported_abis"])
    and isinstance(device.get("primary_abi"), str)
    and device["primary_abi"]
    and isinstance(device.get("is_64_bit"), bool)
):
    raise SystemExit("Device facts are incomplete or malformed")
package_paths = evidence["install"]["package_paths"]
if not package_paths or not all(isinstance(path, str) and path for path in package_paths):
    raise SystemExit("Installed package paths are missing or malformed")

expected_phases = set()
if require_ingestion:
    expected_phases.update(("nonfatal", "lifecycle"))
    if run_fatal:
        expected_phases.add("fatal")

phases = {}
with open("native-qual-evidence-lines.txt", encoding="utf-8") as lines:
    for line in lines:
        if "NativeQualEvidence" not in line:
            continue
        match = re.search(r"\{.*\}", line)
        if not match:
            raise SystemExit("Malformed NativeQualEvidence line without a JSON object")
        try:
            entry = json.loads(match.group(0))
        except json.JSONDecodeError as error:
            raise SystemExit(f"Malformed NativeQualEvidence JSON: {error}") from error
        if not isinstance(entry, dict):
            raise SystemExit("NativeQualEvidence entry must be a JSON object")
        phase = entry.pop("phase", None)
        if phase not in expected_phases:
            raise SystemExit(f"Unexpected NativeQualEvidence phase: {phase!r}")
        if phase in phases:
            raise SystemExit(f"Duplicate NativeQualEvidence phase: {phase}")
        phases[phase] = entry

missing_phases = expected_phases.difference(phases)
if missing_phases:
    raise SystemExit("Missing NativeQualEvidence phase(s): " + ", ".join(sorted(missing_phases)))

def require(condition, message):
    if not condition:
        raise SystemExit(message)

for phase, entry in phases.items():
    require(isinstance(entry.get("guid"), str) and entry["guid"], f"{phase}: missing GUID")
    group_ids = entry.get("group_ids")
    require(
        isinstance(group_ids, list)
        and group_ids
        and all(isinstance(group_id, str) and group_id for group_id in group_ids),
        f"{phase}: invalid group_ids",
    )
    stable_count = entry.get("stable_count")
    require(
        isinstance(stable_count, int) and not isinstance(stable_count, bool),
        f"{phase}: invalid stable_count",
    )
    expected_count = 2 if phase == "lifecycle" else 1
    require(stable_count == expected_count, f"{phase}: expected stable_count={expected_count}")

    if phase in ("nonfatal", "lifecycle"):
        messages = entry.get("messages")
        expected_messages = 2 if phase == "lifecycle" else 1
        require(
            isinstance(messages, dict)
            and len(messages) == expected_messages
            and all(isinstance(message, str) and message for message in messages)
            and all(count == 1 and not isinstance(count, bool) for count in messages.values()),
            f"{phase}: invalid exact-message evidence",
        )

if "nonfatal" in phases:
    nonfatal = phases["nonfatal"]
    require(
        isinstance(nonfatal.get("process_abi"), str) and nonfatal["process_abi"],
        "nonfatal: missing process ABI",
    )
    require(isinstance(nonfatal.get("process_is_64_bit"), bool), "nonfatal: missing process bitness")
    require(
        isinstance(nonfatal.get("handler_path"), str) and nonfatal["handler_path"],
        "nonfatal: missing handler path",
    )
if "fatal" in phases:
    fatal = phases["fatal"]
    require(
        isinstance(fatal.get("crashed_pid"), int)
        and not isinstance(fatal["crashed_pid"], bool)
        and fatal["crashed_pid"] > 0,
        "fatal: invalid crashed PID",
    )
    require(fatal.get("binder_died") is True, "fatal: binder death was not proven")

evidence.update(phases)

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
