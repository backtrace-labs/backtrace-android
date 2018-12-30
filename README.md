# Backtrace
<!--
[![Backtrace@release](https://img.shields.io/badge/Backtrace%40master-2.0.5-blue.svg)](https://www.nuget.org/packages/Backtrace)
 [![Build status](https://ci.appveyor.com/api/projects/status/o0n9sp0ydgxb3ktu?svg=true)](https://ci.appveyor.com/project/konraddysput/backtrace-csharp) 
 -->

<!--
[![Backtrace@pre-release](https://img.shields.io/badge/Backtrace%40dev-2.0.6-blue.svg)](https://www.nuget.org/packages/Backtrace)
[![Build status](https://ci.appveyor.com/api/projects/status/o0n9sp0ydgxb3ktu/branch/dev?svg=true)](https://ci.appveyor.com/project/konraddysput/backtrace-csharp/branch/dev) 
-->



[Backtrace](http://backtrace.io/)'s integration with Android applications written in Java allows customers to capture and report handled and unhandled java exceptions to their Backtrace instance, instantly offering the ability to prioritize and debug software errors.


## Usage
```java
// replace with your endpoint url and token
BacktraceCredentials credentials = new BacktraceCredentials("https://myserver.sp.backtrace.io:6097/", "4dca18e8769d0f5d10db0d1b665e64b3d716f76bf182fbcdad5d1d8070c12db0");
BacktraceClient backtraceClient = new BacktraceClient(getApplicationContext(), credentials);

try {
    //throw exception here
} catch (Exception exception) {
    backtraceClient.send(new BacktraceReport(e));
}
```

# Table of contents
1. [Features Summary](#features-summary)
2. [Supported SKDs](#supported-sdks)
3. [Differences and limitations of the SDKs version](#limitations)
4. [Installation](#installation)
5. [Running sample application](#sample-app)
6. [Documentation](#documentation)
7. [Architecture](#architecture)


# Features Summary <a name="features-summary"></a>
* Light-weight Java client library that quickly submits exceptions and crashes to your Backtrace dashboard. Can include callstack, system metadata, custom metadata.<!--, and file attachments if needed.-->
* Supports a wide range of Android SDKs.
* Supports asynchronous Tasks.
* Fully customizable and extendable event handlers and base classes for custom implementations.

# Differences and limitations of the SDKs version <a name="limitations"></a>

# Supported SDKs <a name="supported-sdks"></a>
* Minimal SDK version 19 (Android 4.4)
* Target SDK version 28 (Android 9.0)

# Installation <a name="installation"></a>
TODO

# Running sample application
## Android Studio <a name="sample-app-android-studio"></a>

- Open `MainActivity.java` class in **app\src\main\java\backtraceio\backtraceio** and replace `BacktraceCredential` constructor parameters with with your `Backtrace endpoint URL` (e.g. https://xxx.sp.backtrace.io:6098) and `submission token`:

```java
BacktraceCredentials credentials = new BacktraceCredentials("https://myserver.sp.backtrace.io:6097/", "4dca18e8769d0f5d10db0d1b665e64b3d716f76bf182fbcdad5d1d8070c12db0");
```

First start:
- Press `Run` and `Run..` or type keys combination `Alt+Shift+F10`.
- As module select `app` other options leave default.
- Select `Run` and then select your emulator or connected device.
- You should see new errors in your Backtrace instance. Refresh the Project page or Query Builder to see new details in real-time.

# Documentation  <a name="documentation"></a>
## Initialize a new BacktraceClient <a name="documentation-initialization"></a>

First create a `BacktraceCredential` instance with your `Backtrace endpoint URL` (e.g. https://xxx.sp.backtrace.io:6098) and `submission token`, and supply it as a parameter in the `BacktraceClient` constructor:

```java
BacktraceCredentials credentials = new BacktraceCredentials("https://myserver.sp.backtrace.io:6097/", "4dca18e8769d0f5d10db0d1b665e64b3d716f76bf182fbcdad5d1d8070c12db0");
BacktraceClient backtraceClient = new BacktraceClient(getApplicationContext(), credentials);
```

## Sending an error report <a name="documentation-sending-report"></a>

Methods `BacktraceClient.send` and `BacktraceClient.sendAsync` will send an error report to the Backtrace endpoint specified. There `send` method is overloaded, see examples below:


### Using BacktraceReport

The `BacktraceReport` class represents a single error report. (Optional) You can also submit custom attributes using the `attributes` parameter. <!--, or attach files by supplying an array of file paths in the `attachmentPaths` parameter.-->

```java
try {
    //throw exception here
} catch (Exception exception) {
    BacktraceReport report = new BacktraceReport(e, 
    new HashMap<String, Object>() {{
        put("key", "value");
    }}, new ArrayList<String>() {{
        add("file_path_1");
        add("file_path2");
    }});
    backtraceClient.send(report);
}
```

#### Asynchronous Send Support

Method `send` behind the mask use `AsyncTask` and wait until method `doInBackground` is not completed. Library gives you the option of not blocking code execution by using method `sendAsync` which returning the `AsyncTask<Void, Void, BacktraceResult>` object. Additionally, it is possible to specify the method that should be performed after completion `AsyncTask` by using events described in [events](#events). 


```java
AsyncTask<Void, Void, BacktraceResult> sendAsyncTask = backtraceClient.sendAsync(report);
// another code
BacktraceResult result = sendAsyncTask.get();
```

### Other BacktraceReport Overloads

`BacktraceClient` can also automatically create `BacktraceReport` given an exception or a custom message using the following overloads of the `BacktraceClient.send` or `BacktraceClient.sendAsync` methods:

```java
try {
  //throw exception here
} catch (Exception exception) {

  backtraceClient.Send(report);
  
  //pass exception to Send method
  backtraceClient.Send(exception);
  
  //pass your custom message to Send method
  backtraceClient.SendAsync("Message");
}
```

## Attaching custom event handlers <a name="documentation-events"></a>

All events are written in *listener* pattern. `BacktraceClient` allows you to attach your custom event handlers. For example, you can trigger actions before the `send` method:
 
```java
backtraceClient.setOnBeforeSendEventListener(new OnBeforeSendEventListener() {
            @Override
            public BacktraceData onEvent(BacktraceData data) {
                // another code
                return data;
            }
        });
```

`BacktraceClient` currently supports the following events:
- `BeforeSend`
- `AfterSend`
- `RequestHandler`
- `OnServerResponse`
- `OnServerError`


## Reporting unhandled application exceptions
`BacktraceClient` also supports reporting of unhandled application exceptions not captured by your try-catch blocks. To enable reporting of unhandled exceptions:
```java
new BacktraceExceptionHandler(backtraceClient);
``` 

## Custom client and report classes <a name="documentation-customization"></a>

You can extend `BacktraceBase` to create your own Backtrace client and error report implementation. You can refer to `BacktraceClient` for implementation inspirations. 

# Architecture  <a name="architecture"></a>

## BacktraceReport  <a name="architecture-BacktraceReport"></a>
**`BacktraceReport`** is a class that describe a single error report.

## BacktraceClient  <a name="architecture-BacktraceClient"></a>
**`BacktraceClient`** is a class that allows you to instantiate a client instance that interacts with `BacktraceApi`. This class sets up connection to the Backtrace endpoint and manages error reporting behavior. `BacktraceClient` extends `BacktraceBase` class.

## BacktraceData  <a name="architecture-BacktraceData"></a>
**`BacktraceData`** is a serializable class that holds the data to create a diagnostic JSON to be sent to the Backtrace endpoint via `BacktraceApi`. You can add additional pre-processors for `BacktraceData` by attaching an event handler to the `BacktraceClient.setOnBeforeSendEventListener(event)` event. `BacktraceData` require `BacktraceReport` and `BacktraceClient` client attributes.

## BacktraceApi  <a name="architecture-BacktraceApi"></a>
**`BacktraceApi`** is a class that sends diagnostic JSON to the Backtrace endpoint. `BacktraceApi` is instantiated when the `BacktraceClient` constructor is called. You use the following event handlers in `BacktraceApi` to customize how you want to handle JSON data:
- `RequestHandler` - attach an event handler to this event to override the default `BacktraceApi.send` method. <!--A `RequestHandler`  TODO -->
- `OnServerError` - attach an event handler to be invoked when the server returns with a `400 bad request`, `401 unauthorized` or other HTTP error codes. <!-- TODO -->
- `OnServerResponse` - attach an event handler to be invoked when the server returns with a valid response.


## BacktraceResult  <a name="architecture-BacktraceResult"></a>
**`BacktraceResult`** is a class that holds response and result from a `send` or `sendAsync` call. The class contains a `Status` property that indicates whether the call was completed (`OK`), the call returned with an error (`ServerError`), . Additionally, the class has a `Message` property that contains details about the status.




