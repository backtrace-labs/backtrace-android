# Backtrace Integration with Android

[![Maven Central](https://img.shields.io/maven-central/v/com.github.backtrace-labs.backtrace-android/backtrace-library.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/com.github.backtrace-labs.backtrace-android/backtrace-library)
![Build Status](https://github.com/backtrace-labs/backtrace-android/actions/workflows/test.yml/badge.svg)
[![javadoc](https://javadoc.io/badge2/com.github.backtrace-labs.backtrace-android/backtrace-library/javadoc.svg)](https://javadoc.io/doc/com.github.backtrace-labs.backtrace-android/backtrace-library)
<img src="http://img.shields.io/badge/license-MIT-lightgrey.svg?style=flat" alt="License: MIT">

[Backtrace](http://backtrace.io/)'s integration with Android applications written in Java or Kotlin allows you to capture and report handled and unhandled java exceptions so you can prioritize and debug software errors. Backtrace also captures and reports native (JNI/NDK) handled and unhandled exceptions if native integration is enabled.

## Installation
### Gradle
```groovy
// provide the latest version of the Backtrace reporting library.
dependencies {
    implementation 'com.github.backtrace-labs.backtrace-android:backtrace-library:<add-latest-version>'
}
```

### Maven
```xml
<!-- provide the latest version of the Android SDK. -->
<dependency>
  <groupId>com.github.backtrace-labs.backtrace-android</groupId>
  <artifactId>backtrace-library</artifactId>
  <version><add-latest-version></version>
  <type>aar</type>
</dependency>
```


## Usage
### Java
```java
// replace with your submission url 
BacktraceCredentials credentials = new BacktraceCredentials("<submissionUrl>");
BacktraceClient backtraceClient = new BacktraceClient(getApplicationContext(), credentials);

// send test report
backtraceClient.send("test");

// Capture uncaught exceptions
BacktraceExceptionHandler.enable(backtraceClient);

// Enable ANR detection
backtraceClient.enableAnr();

// Enable Crash Free metrics
backtraceClient.metrics.enable();
```

### Kotlin
```kotlin
// replace with your submission url
val credentials = BacktraceCredentials("<submissionUrl>")
val backtraceClient = BacktraceClient(applicationContext, credentials)

// send test report
backtraceClient.send("test")

// Capture uncaught exceptions
BacktraceExceptionHandler.enable(backtraceClient)

// Enable ANR detection
backtraceClient.enableAnr()

// Enable Crash Free metrics
backtraceClient.metrics.enable()
```

## Documentation

For more information about the Android SDK, including installation, usage, and configuration options, see the [Android Integration guide](https://docs.saucelabs.com/error-reporting/platform-integrations/android/setup/) in the Sauce Labs documentation.

## Native crash integration startup

Native crash capture requires a writable `BacktraceDatabase`. Configure initial attributes, attachments, and breadcrumbs before enabling the integration, then call `enableNativeIntegration()` as early as practical:

```java
BacktraceDatabase database = new BacktraceDatabase(context, databaseSettings);
BacktraceClient client = new BacktraceClient(context, credentials, database, attributes, attachments);

BacktraceExceptionHandler.enable(client);
client.enableNativeIntegration();
```

`enableNativeIntegration()` is synchronous and remains `void` for compatibility;
a contained setup failure is logged and the call returns normally. To observe the actual setup status, use `tryEnableNativeIntegration()`:

```java
boolean nativeEnabled = client.tryEnableNativeIntegration();
if (!nativeEnabled) {
    // Native crash capture is unavailable. Managed reporting remains available.
}
```

Keeping initialization in the normal startup sequence provides the earliest native-crash coverage.
It can be invoked from one background thread, but native crashes that occur before initialization completes cannot be captured.
Do not invoke enable concurrently or race it with `disableNativeIntegration()`. `dumpWithoutCrash()` is a safe no-op while native integration is unavailable or disabled.

The SDK resolves its native crash-handler library from the path already selected by Android's linker.
Backtrace does not open or parse the host application's base or split APK while resolving the crash-handler library.
See [Native integration startup and threading](docs/native-integration-startup.md) for operational guidance and failure modes.
