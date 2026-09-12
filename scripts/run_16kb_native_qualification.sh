#!/usr/bin/env bash
set -euo pipefail

# 16 KB runtime qualification. ELF alignment is a static property; this proves the complete native
# integration on a device actually running 16 KB pages: the first command asserts the runtime page
# size, then the full split-install gate (resolver, fresh-process safety, nonfatal, fatal+recovery,
# lifecycle) runs with the page-size assertion re-checked inside the gate, plus 16 KB zip alignment
# of the debug APK.

# The emulator runner returns once sys.boot_completed is set, but system services register
# after that flag. On the slow-booting ps16k image PackageManager can still be unpublished when
# bundletool queries the device ("cmd: Can't find service: package"), which fails the run at
# install time. Wait for the services this gate actually depends on before touching the device.
wait_for_device_ready() {
    local deadline=$((SECONDS + 300))
    adb wait-for-device
    while [ "$SECONDS" -lt "$deadline" ]; do
        if [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" = "1" ] \
            && adb shell service check package 2>/dev/null | grep -q "Service package: found" \
            && adb shell service check activity 2>/dev/null | grep -q "Service activity: found" \
            && adb shell pm path android 2>&1 | grep -q "^package:" \
            && adb shell pm list features 2>&1 | grep -q "^feature:"; then
            echo "Device ready: boot completed, PackageManager and ActivityManager registered."
            return 0
        fi
        sleep 5
    done
    echo "Device did not become ready within 300s" >&2
    adb shell service list >&2 || true
    return 1
}
wait_for_device_ready

page_size="$(adb shell getconf PAGE_SIZE | tr -d '\r')"
test "$page_size" = "16384"

# The zip-alignment check is a hard gate: a missing APK means the caller forgot to build it, not
# that the check may be skipped.
apk="example-app/build/outputs/apk/debug/example-app-debug.apk"
test -f "$apk"
build_tools_version="$(ls "$ANDROID_HOME/build-tools" | sort -V | tail -1)"
zipalign="$ANDROID_HOME/build-tools/$build_tools_version/zipalign"
"$zipalign" -c -P 16 -v 4 "$apk" > /dev/null
echo "zipalign -P 16 check passed for $apk"

EXPECTED_PAGE_SIZE=16384 \
REQUIRE_INGESTION="${REQUIRE_INGESTION:-1}" \
RUN_FATAL="${RUN_FATAL:-1}" \
./scripts/run_split_install_test.sh
