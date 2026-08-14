#!/usr/bin/env bash
set -euo pipefail

# API 21/22 verifier, resolver, and clean-process safety lane. Secretless: none of these tests
# require backend credentials. Run from the repository root with one connected device/emulator.

./gradlew \
    :example-app:assembleDebug \
    :example-app:assembleDebugAndroidTest

./gradlew :example-app:connectedDebugAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.class=backtraceio.backtraceio.LegacyNativeIntegrationCompatibilityTest,backtraceio.backtraceio.NativeFreshProcessSafetyTest
