# Backtrace Integration with Android

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.backtrace-labs.backtrace-android/backtrace-library/badge.svg)](https://search.maven.org/artifact/com.github.backtrace-labs.backtrace-android/backtrace-library)
![Build Status](https://github.com/backtrace-labs/backtrace-android/actions/workflows/test.yml/badge.svg)


[Backtrace](http://backtrace.io/)'s integration with Android applications written in Java or Kotlin allows you to capture and report handled and unhandled java exceptions so you can prioritize and debug software errors. Backtrace also captures and reports native (JNI/NDK) handled and unhandled exceptions if native integration is enabled.

## Installation
### Gradle
```
dependencies {
    implementation 'com.github.backtrace-labs.backtrace-android:backtrace-library:3.7.1'
}
```

### Maven
```
<dependency>
  <groupId>com.github.backtrace-labs.backtrace-android</groupId>
  <artifactId>backtrace-library</artifactId>
  <version>3.7.1</version>
  <type>aar</type>
</dependency>
```

<!-- ## Installation pre-release version <a name="prerelease-version"></a>
### Pre-release version of `v.3.1.0` is available in the following repository: https://oss.sonatype.org/content/repositories/comgithubbacktrace-labs-1018/
Add the above url in `build.gradle` file to `repositories` section as below to allow downloading the library from our staging repository:
```
maven {
    url "https://oss.sonatype.org/content/repositories/comgithubbacktrace-labs-1018/"
}
```
Then you can download this library by adding to the dependencies in `build.gradle` file to `dependencies` section:

```
implementation 'com.github.backtrace-labs.backtrace-android:backtrace-library:3.1.0'
```-->

## Permissions
### Internet permission
* To send errors to the server instance you need to add permissions for Internet connection into `AndroidManifest.xml` file in your application.

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

### File access
* To send file attachments from external storage to the server instance you need to add permissions for read external storage into `AndroidManifest.xml` file in your application.

```xml
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

## Usage
### Java
```java
// replace with your endpoint url and token
BacktraceCredentials credentials = new BacktraceCredentials("<endpoint-url>", "<token>");
BacktraceClient backtraceClient = new BacktraceClient(getApplicationContext(), credentials);

try {
    // throw exception here
} catch (Exception exception) {
    backtraceClient.send(new BacktraceReport(e));
}
```

### Kotlin
```kotlin
// replace with your endpoint url and token
val backtraceCredentials = BacktraceCredentials("<endpoint-url>", "<token>")
val backtraceClient = BacktraceClient(applicationContext, backtraceCredentials)

try {
    // throw exception here
}
catch (e: Exception) {
    backtraceClient.send(BacktraceReport(e))
}
```

## Documentation

For more information about the Android SDK, including installation, usage, and configuration options, see the [Android Integration guide](https://docs.saucelabs.com/error-reporting/platform-integrations/android/setup/) in the Sauce Labs documentation.