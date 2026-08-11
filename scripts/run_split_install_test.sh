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

adb install -r "$test_apk"

adb shell am instrument -w -r \
    -e class backtraceio.backtraceio.SplitInstallNativeResolutionTest \
    backtraceio.backtraceio.test/androidx.test.runner.AndroidJUnitRunner | tee instrument-output.txt

# The test must PASS (status code 0), not be skipped by its split-install assumptions (-4) or
# fail (-1/-2); am instrument itself always exits 0, so gate on the raw status stream.
grep -q "INSTRUMENTATION_STATUS_CODE: 0" instrument-output.txt
if grep -qE "INSTRUMENTATION_STATUS_CODE: -[0-9]" instrument-output.txt; then
    echo "Split-install resolver test failed or was skipped; see instrument-output.txt" >&2
    exit 1
fi

echo "Split-install qualification passed: resolver returned the installed ABI configuration split."
