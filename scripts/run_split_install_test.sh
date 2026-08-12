#!/usr/bin/env bash
set -euo pipefail

# Play-style split-install qualification against the connected device/emulator. Installs a
# device-specific APK set generated from the debug AAB, proves an ABI configuration split is
# installed, and runs the resolver qualification test. Requires: repository root as cwd,
# bundletool.jar present (BUNDLETOOL_JAR overrides the path), adb on PATH, one connected device,
# and previously built :example-app:bundleDebug and :example-app:assembleDebugAndroidTest outputs.

bundletool_jar="${BUNDLETOOL_JAR:-bundletool.jar}"
bundle="example-app/build/outputs/bundle/debug/example-app-debug.aab"
test_apk="example-app/build/outputs/apk/androidTest/debug/example-app-debug-androidTest.apk"
apks_output="${APKS_OUTPUT:-example-app-debug.apks}"
debug_keystore="${DEBUG_KEYSTORE:-$HOME/.android/debug.keystore}"

for artifact in "$bundletool_jar" "$bundle" "$test_apk"; do
    if [ ! -f "$artifact" ]; then
        echo "Missing required artifact: $artifact" >&2
        exit 1
    fi
done

# The emulator dies with the CI step, so capture logcat here while it is still reachable; a child
# crash-handler System.load failure is only diagnosable from this log. Capture only the relevant
# tags and redact URL/token-shaped values: handler diagnostics can otherwise carry the submission
# token and customer attributes into an uploaded artifact.
collect_logcat() {
    adb logcat -d -s \
        BacktraceCrashHandlerRunner:V \
        Backtrace-Android:V \
        TestRunner:I \
        AndroidRuntime:E 2>/dev/null \
        | sed -E \
            -e 's#(token=)[A-Za-z0-9._-]+#\1[REDACTED]#g' \
            -e 's#https?://[^[:space:]"]+#[REDACTED_URL]#g' \
        > split-install-logcat.txt || true
}
trap collect_logcat EXIT

# Device-specific APKs must be signed or the install fails with INSTALL_PARSE_FAILED_NO_CERTIFICATES,
# and the certificate must match the instrumentation APK's, so sign with the same Android debug
# keystore Gradle uses, creating it first on fresh CI runners where it does not exist yet.
if [ ! -f "$debug_keystore" ]; then
    mkdir -p "$(dirname "$debug_keystore")"
    keytool -genkeypair -keystore "$debug_keystore" -storepass android -alias androiddebugkey \
        -keypass android -keyalg RSA -keysize 2048 -validity 10000 \
        -dname "CN=Android Debug,O=Android,C=US"
fi

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

adb shell pm path backtraceio.backtraceio | tee installed-package-paths.txt
grep -E 'split_config\.(x86_64|arm64_v8a|armeabi_v7a)\.apk' installed-package-paths.txt

# Instrumentation requires the test package's certificate to match the target's. Which keystore
# Gradle resolved for the debug signing config is environment-dependent, so make the certificates
# identical by construction: re-sign a copy of the instrumentation APK with the exact keystore the
# split APKs were signed with.
build_tools_version="$(ls "$ANDROID_HOME/build-tools" | sort -V | tail -1)"
apksigner="$ANDROID_HOME/build-tools/$build_tools_version/apksigner"
signed_test_apk="${TMPDIR:-/tmp}/example-app-debug-androidTest-resigned.apk"
cp "$test_apk" "$signed_test_apk"
"$apksigner" sign --ks "$debug_keystore" --ks-pass pass:android \
    --ks-key-alias androiddebugkey --key-pass pass:android "$signed_test_apk"

adb install -r "$signed_test_apk"

# Each test must PASS (status code 0), not be skipped by its assumptions (-4) or fail (-1/-2);
# am instrument itself always exits 0, so gate on the raw status stream and the test count.
run_instrumentation_test() {
    local selector="$1"
    local output="$2"
    local expected_tests="$3"

    adb shell am instrument -w -r \
        -e class "$selector" \
        backtraceio.backtraceio.test/androidx.test.runner.AndroidJUnitRunner | tee "$output"

    grep -q "INSTRUMENTATION_STATUS_CODE: 0" "$output"
    if grep -qE "INSTRUMENTATION_STATUS_CODE: -[0-9]" "$output"; then
        echo "Instrumentation failed or was skipped: $selector (see $output)" >&2
        return 1
    fi
    grep -q "OK (${expected_tests} test" "$output"
}

run_instrumentation_test \
    "backtraceio.backtraceio.SplitInstallNativeResolutionTest" \
    "split-resolver-output.txt" \
    1

# The handler-ingestion test needs working Backtrace test credentials in BuildConfig. CI provides
# them and must gate on the result. Local runs may carry stale or absent credentials, so with
# REQUIRE_INGESTION=0 the handler test runs informationally only and the resolver test remains the
# hard gate.
if [ "${REQUIRE_INGESTION:-1}" = "1" ]; then
    run_instrumentation_test \
        "backtraceio.backtraceio.SplitInstallNativeIntegrationTest" \
        "split-native-output.txt" \
        1
    echo "Split-install qualification passed: resolver, handler load, and report ingestion verified."
else
    adb shell am instrument -w -r \
        -e class backtraceio.backtraceio.SplitInstallNativeIntegrationTest \
        backtraceio.backtraceio.test/androidx.test.runner.AndroidJUnitRunner | tee split-native-output.txt || true
    echo "Split-install resolver qualification passed (handler ingestion not gated in this run)."
fi
