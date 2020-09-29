# Backtrace

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.backtrace-labs.backtrace-android/backtrace-library/badge.svg)](https://search.maven.org/artifact/com.github.backtrace-labs.backtrace-android/backtrace-library)
[![Build Status](https://travis-ci.org/backtrace-labs/backtrace-android.png?branch=master)](https://travis-ci.org/backtrace-labs/backtrace-android)
<!--
[![Backtrace@release](https://img.shields.io/badge/Backtrace%40master-2.0.5-blue.svg)](https://www.nuget.org/packages/Backtrace)
 [![Build status](https://ci.appveyor.com/api/projects/status/o0n9sp0ydgxb3ktu?svg=true)](https://ci.appveyor.com/project/konraddysput/backtrace-csharp) 
 -->

<!--
[![Backtrace@pre-release](https://img.shields.io/badge/Backtrace%40dev-2.0.6-blue.svg)](https://www.nuget.org/packages/Backtrace)
[![Build status](https://ci.appveyor.com/api/projects/status/o0n9sp0ydgxb3ktu/branch/dev?svg=true)](https://ci.appveyor.com/project/konraddysput/backtrace-csharp/branch/dev) 
-->



[Backtrace](http://backtrace.io/)'s integration with Android applications written in Java or Kotlin which allows customers to capture and report handled and unhandled java exceptions to their Backtrace instance, instantly offering the ability to prioritize and debug software errors.


## Usage
Java
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

Kotlin
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

# Table of contents
1. [Features Summary](#features-summary)
2. [Supported SDKs](#supported-sdks)
3. [Differences and limitations of the SDKs version](#limitations)
4. [Installation](#installation)
5. [Running sample application](#sample-app)
6. [Using Backtrace library](#using-backtrace)
7. [Documentation](#documentation)


# Features Summary <a name="features-summary"></a>
* Light-weight Java client library that quickly submits exceptions and crashes to your Backtrace dashboard. Can include callstack, system metadata, custom metadata and file attachments if needed.
* Supports a wide range of Android SDKs.
* Supports offline database for error report storage and re-submission in case of network outage.
* Fully customizable and extendable event handlers and base classes for custom implementations.
* Supports detection of blocking the application's main thread (Application Not Responding).
* Supports monitoring the blocking of manually created threads by providing watchdog.

# Supported SDKs <a name="supported-sdks"></a>
* Minimal SDK version 21 (Android 5.0)
* Target SDK version 28 (Android 9.0)

# Differences and limitations of the SDKs version <a name="limitations"></a>
* Getting the status that the device is in power saving mode is available from API 21.

# Installation <a name="installation"></a>
## Download library via Gradle or Maven
* Gradle
```
dependencies {
    implementation 'com.github.backtrace-labs.backtrace-android:backtrace-library:3.0.2'
}
```

* Maven
```
<dependency>
  <groupId>com.github.backtrace-labs.backtrace-android</groupId>
  <artifactId>backtrace-library</artifactId>
  <version>3.0.2</version>
  <type>aar</type>
</dependency>
```

<!-- ## Installation pre-release version <a name="prerelease-version"></a>
### Pre-release version of `v.3.0.2` is available in the following repository: https://oss.sonatype.org/content/repositories/comgithubbacktrace-labs-1018/
Add the above url in `build.gradle` file to `repositories` section as below to allow downloading the library from our staging repository:
```
maven {
    url "https://oss.sonatype.org/content/repositories/comgithubbacktrace-labs-1018/"
}
```
Then you can download this library by adding to the dependencies in `build.gradle` file to `dependencies` section:

```
implementation 'com.github.backtrace-labs.backtrace-android:backtrace-library:3.0.2'
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


# Running sample application
## Android Studio <a name="sample-app-android-studio"></a>

- Open `MainActivity.java` class in **app\src\main\java\backtraceio\backtraceio** and replace `BacktraceCredential` constructor parameters with your `Backtrace endpoint URL` (e.g. https://xxx.sp.backtrace.io:6098) and `submission token`:

Java
```java
BacktraceCredentials credentials = new BacktraceCredentials("https://<yourInstance>.sp.backtrace.io:6098/", "<submissionToken>");
```

Kotlin
```kotlin
val backtraceCredentials = BacktraceCredentials("https://<yourInstance>.sp.backtrace.io:6098/", "<submissionToken>")
```

First start:
- Press `Run` and `Run..` or type keys combination `Alt+Shift+F10`.
- As module select `app` other options leave default.
- Select `Run` and then select your emulator or connected device.
- You should see new errors in your Backtrace instance. Refresh the Project page or Query Builder to see new details in real-time.

# Using Backtrace library  <a name="using-backtrace"></a>
## Initialize a new BacktraceClient <a name="using-backtrace-initialization"></a>

First create a `BacktraceCredential` instance with your `Backtrace endpoint URL` (e.g. https://xxx.sp.backtrace.io:6098) and `submission token`, and supply it as a parameter in the `BacktraceClient` constructor:

Java
```java
BacktraceCredentials credentials = new BacktraceCredentials("https://<yourInstance>.sp.backtrace.io:6098/", "<submissionToken>");
BacktraceClient backtraceClient = new BacktraceClient(getApplicationContext(), credentials);
```

Kotlin
```kotlin
val backtraceCredentials = BacktraceCredentials("https://<yourInstance>.sp.backtrace.io:6098/", "<submissionToken>")
val backtraceClient = BacktraceClient(applicationContext, backtraceCredentials)
```

Another option for creating a BacktraceCredentials object is using the URL to which the report is to be sent, pass URL string as parameter to `BacktraceCredentials` constructor:

Java
```java
BacktraceCredentials credentials = new BacktraceCredentials("https://submit.backtrace.io/{universe}/{token}/json");
```

Kotlin
```kotlin
val backtraceCredentials = BacktraceCredentials("https://submit.backtrace.io/{universe}/{token}/json")
```

## Setting global custom attributes

It is possible to add your global custom attributes to BacktraceClient and send them with each of report. To do it you should pass map with custom attributes to BacktraceClient constructor method.

Java
```java
Map<String, Object> attributes = new HashMap<String, Object>(){{
    put("custom-attribute-key", "custom-attribute-value");
}};
BacktraceClient backtraceClient = new BacktraceClient(context, credentials, attributes);
```

Kotlin
```kotlin
val attributes: HashMap<String, Any> = hashMapOf("custom-attribute-key" to "custom-attribute-value")
val backtraceClient = BacktraceClient(context, credentials, attributes)
```

## Enabling ANR detection
Backtrace client allows you to detect that main thread is blocked, you can pass `timeout` as argument and `event` which should be executed instead of sending the error information to the Backtrace console by default. You can also provide information that the application is working in the debug mode by providing `debug` parameter, then if the debugger is connected errors will not be reported. Default value of `timeout` is 5 seconds.

```
backtraceClient.enableAnr(timeout, event, debug);
```
## Database initialization <a name="using-backtrace-initialization"></a>

BacktraceClient allows you to customize the initialization of BacktraceDatabase for local storage of error reports by supplying a BacktraceDatabaseSettings parameter, as follows:

Java
```java
BacktraceCredentials credentials = new BacktraceCredentials("https://myserver.sp.backtrace.io:6097/", "4dca18e8769d0f5d10db0d1b665e64b3d716f76bf182fbcdad5d1d8070c12db0");

Context context = getApplicationContext();
String dbPath = context.getFilesDir().getAbsolutePath(); // any path, eg. absolute path to the internal storage

BacktraceDatabaseSettings settings = new BacktraceDatabaseSettings(dbPath);
settings.setMaxRecordCount(100);
settings.setMaxDatabaseSize(100);
settings.setRetryBehavior(RetryBehavior.ByInterval);
settings.setAutoSendMode(true);
settings.setRetryOrder(RetryOrder.Queue);

BacktraceDatabase database = new BacktraceDatabase(context, settings);
BacktraceClient backtraceClient = new BacktraceClient(context, credentials, database);
// start capturing NDK crashes
database.setupNativeIntegration(backtraceClient, credentials);
```

## Sending an error report <a name="using-backtrace-sending-report"></a>

Method `BacktraceClient.send` will send an error report to the Backtrace endpoint specified. There `send` method is overloaded, see examples below:

### Using BacktraceReport

The `BacktraceReport` class represents a single error report. (Optional) You can also submit custom attributes using the `attributes` parameter. <!--, or attach files by supplying an array of file paths in the `attachmentPaths` parameter.-->

Java
```java
try {
    // throw exception here
} catch (Exception e) {
    BacktraceReport report = new BacktraceReport(e, 
    new HashMap<String, Object>() {{
        put("key", "value");
    }}, new ArrayList<String>() {{
        add("absoulte_file_path_1");
        add("absoulte_file_path_2");
    }});
    backtraceClient.send(report);
}
```

Kotlin
```kotlin
try {
    // throw exception here
}
catch (e: Exception) {
    val report = BacktraceReport(e, mapOf("key" to "value"), listOf("absolute_file_path_1", "absolute_file_path_2"))
    backtraceClient.send(report)
}
```

### Asynchronous Send support

Method `send` behind the mask use dedicated thread which sending report to server. You can specify the method that should be performed after completion.


Java
```java
client.send(report, new OnServerResponseEventListener() {
    @Override
    public void onEvent(BacktraceResult backtraceResult) {
        // process result here
    }
});
```

Kotlin
```kotlin
client.send(report) { backtraceResult ->
    // process result here
}
```

### Other BacktraceReport overloads

`BacktraceClient` can also automatically create `BacktraceReport` given an exception or a custom message using the following overloads of the `BacktraceClient.send` method:

Java
```java
try {
  // throw exception here
} catch (Exception exception) {

  backtraceClient.send(new BacktraceReport(exception));
  
  // pass exception to send method
  backtraceClient.send(exception);
  
  // pass your custom message to send method
  backtraceClient.send("Message");
}
```
Kotlin
```kotlin
try {
    // throw exception here
} catch (exception: Exception) {
  backtraceClient.send(BacktraceReport(exception));
  
  // pass exception to send method
  backtraceClient.send(exception);
  
  // pass your custom message to send method
  backtraceClient.send("Message");
}
```


## Attaching custom event handlers <a name="documentation-events"></a>

All events are written in *listener* pattern. `BacktraceClient` allows you to attach your custom event handlers. For example, you can trigger actions before the `send` method:
 
 Java
```java
backtraceClient.setOnBeforeSendEventListener(new OnBeforeSendEventListener() {
    @Override
    public BacktraceData onEvent(BacktraceData data) {
        // another code
        return data;
    }
});
```

Kotlin
```kotlin
backtraceClient.setOnBeforeSendEventListener { data ->
    // another code
    data
}
```

`BacktraceClient` currently supports the following events:
- `BeforeSend`
- `RequestHandler`
- `OnServerError`


## Reporting unhandled application exceptions
`BacktraceClient` also supports reporting of unhandled application exceptions not captured by your try-catch blocks. To enable reporting of unhandled exceptions:
```java
BacktraceExceptionHandler.enable(backtraceClient);
``` 

You can add custom map of attributes to `BacktraceExceptionHandler` which will be sent with each unhandled exception:


```java
BacktraceExceptionHandler.setCustomAttributes(customAttributes);
```

If you would like to capture NDK Crashes you can use `BacktraceDatabase` `setupNativeIntegration` method.

```java
        database.setupNativeIntegration(backtraceClient, credentials);
```


## Enable library logger - debug mode
`BacktraceLogger` is a class which helps with debugging and analysis code flow execution inside the library. Logger is a wrapper on Android `Log` class. `BacktraceLogger` supports 4 logging levels:
- `DEBUG`
- `WARN`
- `ERROR`
- `OFF`

In order to enable displaying logs from inside the library, one should set the level from which information should be logged:

```java
BacktraceLogger.setLevel(LogLevel.DEBUG);
```

## Custom client and report classes <a name="documentation-customization"></a>

You can extend `BacktraceBase` to create your own Backtrace client and error report implementation. You can refer to `BacktraceClient` for implementation inspirations. 

## Monitoring custom threads
Library provides structures and methods to monitor the blocking of your own threads. It is the responsibility of the library user to check whether the thread is blocked and the user's thread should increment the counter.

Java
```
BacktraceWatchdog watchdog = BacktraceWatchdog(backtraceClient); // Initialize BacktraceWatchdog
watchdog.registerThread(customThread, timeout, delay); // Register custom thread

watchdog.checkIsAnyThreadIsBlocked(); // check if any thread has exceeded the time, by default an error will be sent to the Backtrace console


// The following code should be executed inside the thread you want to monitor
watchdog.tick(this); // In your custom thread class make incrementation to inform that the thread is not blocked
```

# Documentation  <a name="documentation"></a>

## BacktraceReport  <a name="documentation-BacktraceReport"></a>
**`BacktraceReport`** is a class that describe a single error report.

## BacktraceClient  <a name="documentation-BacktraceClient"></a>
**`BacktraceClient`** is a class that allows you to instantiate a client instance that interacts with `BacktraceApi`. This class sets up connection to the Backtrace endpoint and manages error reporting behavior. `BacktraceClient` extends `BacktraceBase` class.

## BacktraceData  <a name="documentation-BacktraceData"></a>
**`BacktraceData`** is a serializable class that holds the data to create a diagnostic JSON to be sent to the Backtrace endpoint via `BacktraceApi`. You can add additional pre-processors for `BacktraceData` by attaching an event handler to the `BacktraceClient.setOnBeforeSendEventListener(event)` event. `BacktraceData` require `BacktraceReport` and `BacktraceClient` client attributes.

## BacktraceApi  <a name="documentation-BacktraceApi"></a>
**`BacktraceApi`** is a class that sends diagnostic JSON to the Backtrace endpoint. `BacktraceApi` is instantiated when the `BacktraceClient` constructor is called. You use the following event handlers in `BacktraceApi` to customize how you want to handle JSON data:
- `RequestHandler` - attach an event handler to this event to override the default `BacktraceApi.send` method.
- `OnServerError` - attach an event handler to be invoked when the server returns with a `400 bad request`, `401 unauthorized` or other HTTP error codes.


## BacktraceResult  <a name="documentation-BacktraceResult"></a>
**`BacktraceResult`** is a class that holds response and result from a `send` method call. The class contains a `status` property that indicates whether the call was completed (`OK`), the call returned with an error (`ServerError`), . Additionally, the class has a `message` property that contains details about the status.

## BacktraceDatabase  <a name="documentation-BacktraceDatabase"></a>

**`BacktraceDatabase`** is a class that stores error report data in your local hard drive. If `DatabaseSettings` dones't contain a **valid** `DatabasePath` then `BacktraceDatabase` won't store error report data. 

`BacktraceDatabase` stores error reports that were not sent successfully due to network outage or server unavailability. `BacktraceDatabase` periodically tries to resend reports 
cached in the database.  In `BacktraceDatabaseSettings` you can set the maximum number of entries (`MaxRecordCount`) to be stored in the database. The database will retry sending 
stored reports every `RetryInterval` seconds up to `RetryLimit` times, both customizable in the `BacktraceDatabaseSettings`. 

`BacktraceDatabaseSettings` has the following properties:
- `DatabasePath` - the local directory path where `BacktraceDatabase` stores error report data when reports fail to send
- `MaxRecordCount` - Maximum number of stored reports in Database. If value is equal to `0`, then there is no limit.
- `MaxDatabaseSize` - Maximum database size in MB. If value is equal to `0`, there is no limit.
- `AutoSendMode` - if the value is `true`, `BacktraceDatabase` will automatically try to resend stored reports. Default is `false`.
- `RetryBehavior` - 
	- `RetryBehavior.ByInterval` - Default. `BacktraceDatabase` will try to resend the reports every time interval specified by `RetryInterval`.
	- `RetryBehavior.NoRetry` - Will not attempt to resend reports
- `RetryInterval` - the time interval between retries, in seconds.
- `RetryLimit` - the maximum number of times `BacktraceDatabase` will attempt to resend error report before removing it from the database.


If you want to clear your database or remove all reports after send method you can use `clear` or `flush` methods.