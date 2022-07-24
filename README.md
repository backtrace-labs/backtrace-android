# Backtrace

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.backtrace-labs.backtrace-android/backtrace-library/badge.svg)](https://search.maven.org/artifact/com.github.backtrace-labs.backtrace-android/backtrace-library)
![Build Status](https://github.com/backtrace-labs/backtrace-android/actions/workflows/test.yml/badge.svg)


[Backtrace](http://backtrace.io/)'s integration with Android applications written in Java or Kotlin which allows customers to capture and report handled and unhandled java exceptions to their Backtrace instance, instantly offering the ability to prioritize and debug software errors. Backtrace also captures and reports native (JNI/NDK) handled and unhandled exceptions if [native integration is enabled](#working_with_ndk).


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
7. [File attachments](#file-attachments)
8. [Breadcrumbs](#breadcrumbs)
9. [Error-Free Metrics](#metrics)
10. [Working with NDK applications](#working_with_ndk)
11. [Working with Proguard](#working_with_proguard)
12. [Documentation](#documentation)


# Features Summary <a name="features-summary"></a>
* Light-weight Java client library that quickly submits exceptions and crashes to your Backtrace dashboard. Can include callstack, system metadata, custom metadata and file attachments if needed.
* Supports a wide range of Android SDKs.
* Supports offline database for error report storage and re-submission in case of network outage.
* Fully customizable and extendable event handlers and base classes for custom implementations.
* Supports detection of blocking the application's main thread (Application Not Responding).
* Supports monitoring the blocking of manually created threads by providing watchdog.
* Supports native (JNI/NDK) exceptions and crashes.
* Supports Proguard obfuscated crashes.
* Supports Breadcrumbs.

# Supported SDKs <a name="supported-sdks"></a>
* Minimum SDK version 16 (Android 4.1.x)
* Target SDK version 30 (Android 11.0)
* Minimum NDK version 16b
* Maximum NDK version 22

# Supported platforms
* arm32/arm64
* x86_64 emulator

# Differences and limitations of the SDKs version <a name="limitations"></a>
* Getting the status that the device is in power saving mode is available from API 21.

# Installation <a name="installation"></a>
## Download library via Gradle or Maven
* Gradle
```
dependencies {
    implementation 'com.github.backtrace-labs.backtrace-android:backtrace-library:3.7.1'
}
```

* Maven
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
        add("absolute_file_path_1");
        add("absolute_file_path_2");
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

```java
BacktraceWatchdog watchdog = BacktraceWatchdog(backtraceClient); // Initialize BacktraceWatchdog
watchdog.registerThread(customThread, timeout, delay); // Register custom thread

watchdog.checkIsAnyThreadIsBlocked(); // check if any thread has exceeded the time, by default an error will be sent to the Backtrace console


// The following code should be executed inside the thread you want to monitor
watchdog.tick(this); // In your custom thread class make incrementation to inform that the thread is not blocked
```
# File Attachments <a name="file-attachments"></a>
You can enable default file attachments which will be sent with all Backtrace reports both managed and native.

```Java
final String fileName = context.getFilesDir() + "/" + "myCustomFile.txt";
List<String> attachments = new ArrayList<String>(){{
    add(fileName);
}};

backtraceClient = new BacktraceClient(context, credentials, database, attributes, attachments);
```

Backtrace crash file attachment paths can only be specified on initialization. If you have rotating file logs or another situation where the exact filename won't be known when you initialize your Backtrace client, you can use symlinks:

```Java
// The file simlink path to pass to Backtrace
final String fileName = context.getFilesDir() + "/" + "myCustomFile.txt";
List<String> attachments = new ArrayList<String>(){{
    add(fileName);
}};

backtraceClient = new BacktraceClient(context, credentials, database, attributes, attachments);

// The actual filename of the desired log, not known to the BacktraceClient on initialization
final String fileNameDateString = context.getFilesDir() + "/" + "myCustomFile06_11_2021.txt";
// Create symlink
Os.symlink(fileNameDateString, fileName);
```

Note: If you create any new files in the same directory as your `BacktraceDatabase` directory, they will be deleted when you create a new `BacktraceClient`.

# Breadcrumbs <a name="breadcrumbs"></a>
Breadcrumbs help you track events leading up to your crash, error, or other submitted object.

When breadcrumbs are enabled, any captured breadcrumbs will automatically be attached as a file to your crash, error, or other submitted object (including native crashes) and displayed in the UI in the `Breadcrumbs` tab.

## Enabling Breadcrumbs
```java
backtraceClient.enableBreadcrumbs(view.getContext().getApplicationContext());
```
Pass the Application Context to get automatic breadcrumbs for [ActivityLifecycleCallbacks](https://developer.android.com/reference/android/app/Application.ActivityLifecycleCallbacks)

## Adding Breadcrumbs
```java
backtraceClient.addBreadcrumb("About to send Backtrace report", BacktraceBreadcrumbType.LOG);
```

## Automatic Breadcrumbs
By default if you enable breadcrumbs we will register handlers to capture Android Broadcasts and other common system events, such as low memory warnings, battery warnings, screen orientation changes, ActivityLifecycleCallbacks, etc.

You can limit the types of automatic events we capture for you by specifying which automatic breadcrumb types you want to enable, such as:
```java
EnumSet<BacktraceBreadcrumbType> breadcrumbTypesToEnable = EnumSet.of(BacktraceBreadcrumbType.USER);
backtraceClient.enableBreadcrumbs(view.getContext().getApplicationContext(), breadcrumbTypesToEnable);
```

To disable all automatic breadcrumbs:
```java
EnumSet<BacktraceBreadcrumbType> breadcrumbTypesToEnable = EnumSet.of(BacktraceBreadcrumbType.MANUAL);
backtraceClient.enableBreadcrumbs(view.getContext().getApplicationContext(), breadcrumbTypesToEnable);
```

NOTE: Breadcrumbs that you add using `addBreadcrumb` calls in your own code are always logged, regardless of their `BacktraceBreadcrumbType`, as long as breadcrumbs are enabled. The enabled breadcrumb types do not affect your own `addBreadcrumb` calls.

## Adding Breadcrumbs from NDK/C++
To add breadcrumbs from NDK, first you must register your `BacktraceClient` Java class with the NDK.

You can do this by creating a JNI function which passes your active `BacktraceClient` to the `Backtrace::InitializeNativeBreadcrumbs` function from the Backtrace header, `backtrace-android.h`. `backtrace-android.h` is included in the `example-app` in this repo.

JNI
```cpp
#include <jni.h>
#include "backtrace-android.h"

JNIEXPORT jboolean JNICALL
Java_backtraceio_backtraceio_MainActivity_registerNativeBreadcrumbs(JNIEnv *env, jobject thiz,
        jobject backtrace_base) {
    return Backtrace::InitializeNativeBreadcrumbs(env, backtrace_base);
}
```

`backtrace-android.h`
```cpp
bool Backtrace::InitializeNativeBreadcrumbs(JNIEnv *env, jobject backtrace_base);
```
Once you have registered your `BacktraceClient` by passing it to `Backtrace::InitializeNativeBreadcrumbs`, you can add breadcrumbs from your NDK/C++ code by directly calling the below function from `backtrace-android.h`

```cpp
#include <jni.h>
#include "backtrace-android.h"

std::unordered_map<std::string, std::string> attributes;
attributes["My Attribute"] = "Attribute Value";
bool success = Backtrace::AddBreadcrumb(env,
                                    "My Native Breadcrumb",
                                    &attributes,
                                    Backtrace::BreadcrumbType::USER,
                                    Backtrace::BreadcrumbLevel::ERROR);
```

## Breadcrumbs Best Practices
- Don't make calls to `addBreadcrumb` from performance-critical code paths.

# Error-Free Metrics <a name="metrics"></a>
Error free metrics can be used to answer the following questions:
- How many of your unique users (i.e: unique device IDs) using your app are experiencing errors/crashes?
- How many application sessions (i.e: individual application sessions from startup till shutdown/exit) of your app are experiencing errors/crashes?

The web UI allows you to track those metrics at-a-glance as well as in detail (what kinds of errors/crashes are most common?, etc.).

## Enabling Error-Free Metrics
```java
// Enable metrics
BacktraceMetricsSettings metricsSettings = new BacktraceMetricsSettings(backtraceCredentials);
backtraceClient.metrics.enable(metricsSettings);
```

**NOTE:** Please enable metrics BEFORE enabling native integration

# Working with NDK applications <a name="working_with_ndk"></a>

## Enabling native integration

If you would like to capture NDK Crashes you can use the `BacktraceClient` `enableNativeIntegration` method. In general this should be the final step in setting up your Backtrace client to ensure all attributes and file attachment paths are captured properly by the native crash handler.

```java
backtraceClient.enableNativeIntegration();
```

In addition, you may need to add the [extractNativeLibs](https://developer.android.com/guide/topics/manifest/application-element#extractNativeLibs) option to your AndroidManifest.xml:
```xml
<application
        android:extractNativeLibs="true">
        ...
</application>
```
More details about [extractNativeLibs](https://developer.android.com/guide/topics/manifest/application-element#extractNativeLibs) are available from the Android documentation

You can also disable (and re-enable) native integration:
```java
backtraceClient.disableNativeIntegration();
```

**NOTE:** If your native app is built with NDK 16b, the Breakpad native crash client will be used instead of our recommended Crashpad crash client. To avoid this please use NDK 17c+ to build your native app.

**NOTE:** Breakpad crash reports are submitted on the next app startup, instead of at crash time like Crashpad crash reports

**NOTE:** Breakpad does not currently support `disableNativeIntegration`

## Uploading symbols to Backtrace
For an NDK application, debugging symbols are not available to Backtrace by default. You will need to upload the application symbols for your native code to Backtrace. You can do this by uploading the native libraries themselves, which are usually found in the .apk bundle. [Click here to learn more about symbolification](https://support.backtrace.io/hc/en-us/articles/360040517071-Symbolication-Overview)

## Client side unwinding
For an NDK application, debugging symbols for system functions (for instance in `libc.so`) and other opaque libraries can be difficult to obtain. In these cases, it is better to unwind the callstack on the crashing application (i.e: the client). This may not provide the same callstack quality as with debugging symbols, but will give you debugging information you would otherwise not have if you don't have debugging symbols available.

To enable client side unwinding, you can call the `setupNativeIntegration` method with an additional boolean value.
```java
database.setupNativeIntegration(backtraceClient, credentials, true);
```

**NOTE:** When viewing a crash in the Backtrace Debugger, it may still show warning messages that symbols are missing from certain frames after client-side unwinding is performed. This warning is expected if these symbols are not available on the Backtrace server, and should have no impact to the end-user's ability to read the call stack.

**NOTE:** Client side unwinding is only available for fatal crashes. Non-fatal Crashpad dumps you generate via `DumpWithoutCrash` for instance will not use client side unwinding.

**NOTE:** Client side unwinding is only available in NDK level 17+ (i.e: Only with the Crashpad crash reporting backend)

**NOTE:** Client side unwinding is only available in SDK level 21+ (i.e: If minSDKVersion < 21, client-side unwinding will be disabled for 32-bit arm platforms)

### Unwinding Modes and Options

You can optionally specify the unwinding mode (`REMOTE_DUMPWITHOUTCRASH` is the default)
```java
database.setupNativeIntegration(backtraceClient, credentials, true, UnwindingMode.REMOTE_DUMPWITHOUTCRASH);
```

- **LOCAL** - Unwinding is done within the same process that has the crash. This is less robust than remote unwinding, but avoids the complexity of creating a child process and IPC. Local unwinding is executed from a signal handler and needs to be signal-safe.
- **REMOTE** - Unwinding is done by a child process. This means that the unwinding is correct even in case of severe malfunctions in the crashing parent process, and signal-safety is not a concern.
- **LOCAL_DUMPWITHOUTCRASH** - The same as `LOCAL` unwinding, but instead of using the regular Crashpad signal hander to call the unwinder and regular Crashpad reporting mechanism, Backtrace's custom signal handler will be used to call the unwinder before we send the report using Crashpad's `DumpWithoutCrash()` method.
- **REMOTE_DUMPWITHOUTCRASH** - This is the default and recommended option. Same as `REMOTE` unwinding, but instead of using the regular Crashpad signal hander to call the unwinder and regular Crashpad reporting mechanism, Backtrace's custom signal handler will be used to call the unwinder before we send the report using Crashpad's `DumpWithoutCrash()` method.
- **LOCAL_CONTEXT** -  The same as `LOCAL` unwinding, but use `ucontext_t *` from the signal handler to reconstruct the callstack.

# Working with Proguard <a name="working_with_proguard"></a>

##### 1. Add the following to the `proguard_rules.pro` for your app
These are needed since Proguard breaks some Backtrace libraries
```
-keep class com.google.gson.**.* { *; }
-keep class backtraceio.library.**.* { *; }
```
##### 2. Enable Proguard mode in the [BacktraceClient](#documentation-BacktraceClient)
```java
backtraceClient.enableProguard();
```

##### 3. Create a UUID of your choice and set it as the value for the attribute `symbolication_id`, you will upload your Proguard mapping file with this same UUID later
```java
final UUID proguardMappingUUID = UUID.fromString("f6c3e8d4-8626-4051-94ec-53e6daccce25");
final Map<String, Object> attributes = new HashMap<String, Object>() {{
    put("symbolication_id", proguardMappingUUID.toString());
}};
```

##### 4. Upload your Proguard mapping file with the UUID from the above step to Backtrace

Currently we don't have a way to upload the Proguard mapping file from the UI. You will need to use a tool such as `curl` or Postman to upload the Proguard mapping file to Backtrace.
To do so, please construct an HTTP POST request with the following parameters, and submit the mapping file as the request body:
`https://<Universe Name>.sp.backtrace.io:6098/post?format=proguard&token=<Symbol Upload Token>&universe=<Universe Name>&project=<Project Name>&symbolication_id=<symbolication_id from above>`

##### 5. Start sending Proguard obfuscated crashes!
If the symbolication_id from the submitted crash matches a symbolication_id of a submitted Proguard mapping file, it will attempt to use that mapping file to deobfuscate the symbols from the submitted crash.

#### Important Note for Windows users:
Please ensure your Proguard mapping file has Unix line endings before submitting to Backtrace!

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
