# Backtrace Integration with Android

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.backtrace-labs.backtrace-android/backtrace-library/badge.svg)](https://search.maven.org/artifact/com.github.backtrace-labs.backtrace-android/backtrace-library)
![Build Status](https://github.com/backtrace-labs/backtrace-android/actions/workflows/test.yml/badge.svg)


[Backtrace](http://backtrace.io/)'s integration with Android applications written in Java or Kotlin allows you to capture and report handled and unhandled java exceptions so you can prioritize and debug software errors. Backtrace also captures and reports native (JNI/NDK) handled and unhandled exceptions if native integration is enabled.

## Installation
### Gradle
```
// provide the latest version of the Backtrace reporting library.
dependencies {
    implementation 'com.github.backtrace-labs.backtrace-android:backtrace-library:<add-latest-version>'
}
```

### Maven
```
// provide the latest version of the Android SDK.
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

// Enable handling of native crashes
backtraceClient.database.setupNativeIntegration(backtraceClient, credentials, false);

// Enable ANR detection
backtraceClient.enableAnr();

// Enable Crash Free metrics
backtraceClient.metrics.enable(new BacktraceMetricsSettings(credentials));
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

// Enable handling of native crashes
backtraceClient.database.setupNativeIntegration(backtraceClient, credentials, false)

// Enable ANR detection
backtraceClient.enableAnr()

// Enable Crash Free metrics
backtraceClient.metrics.enable(BacktraceMetricsSettings(credentials))
```

## Documentation

For more information about the Android SDK, including installation, usage, and configuration options, see the [Android Integration guide](https://docs.saucelabs.com/error-reporting/platform-integrations/android/setup/) in the Sauce Labs documentation.