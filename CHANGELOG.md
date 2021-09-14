# Backtrace Android Release Notes

## Version 3.5.0 - 10.09.2021
- Added support for native crash reporting in NDK 16b
- Bug fixes and expanded supported NDK versions for client side unwinding

## Version 3.4.0 - 07.09.2021
- Added support for NDK 22

## Version 3.3.0 - 15.07.2021
- Added support for client side unwinding of native crashes

## Version 3.2.2 - 10.03.2021
- Hotfix for crash when user enables native integration without file attachments

## Version 3.2.1 - 09.03.2021
- Native crashes now support custom attributes
- Improved file attachment support for managed and native crashes
- Fixed file attachments bug in BacktraceDatabaseRecord

## Version 3.2.0 - 02.03.2021
- Added Breadcrumbs feature to give Backtrace users context leading up to a `BacktraceReport`
- Improved support for Proguard

## Version 3.1.0 - 29.09.2020
- Backtrace Android allows to capture native crashes from Android NDK code. To enable NDK crashes exception handler use `setupNativeIntegration` method and pass backtraceClient with credentials.

```java
        database.setupNativeIntegration(backtraceClient, credentials);
```

## Version 3.0.2 - 23.01.2020
- Fixed checking internal path during filtering attachments

## Version 3.0.1 - 15.01.2020
- Fixed setting custom global attributes on BacktraceClient
- Added support for custom attributes to BacktraceExceptionHandler

## Version 3.0.0 - 03.12.2019
- Added support for `submit.backtrace.io` urls
- Moved generating a server url from BacktraceApi to BacktraceCredentials
- Refactored name of below libraries interfaces:
    * IBacktraceApi -> Api
    * IBacktraceClient -> Client
    * IBacktraceDatabase -> Database
    * IBacktraceDatabaseRecordWriter -> DatabaseRecordWriter
    * IBacktraceDatabaseFileContext -> DatabaseFileContext
    * IBacktraceDatabaseContext -> DatabaseContext

## Version 2.1.0 - 16.06.2019
- Added support for detecting ANR (Application Not Responding)
- Added methods and structures to detecting blocked custom threads

## Version 2.0.0 - 06.05.2019
- Removed `sendAsync` method
- Removed event `OnAfterSendEventListener`
- Moved event `OnServerResponseEventListener` to parameter of `send` method
- Created dedicated thread to sending HTTP requests to server what caused the removal of AsyncTasks, speeding up the library and fixing errors related to creating threads when closing the application
- Added `BacktraceLogger` to debug the flow of library code execution
- Removed deprecated and unused code

## Version 1.2.1 - 12.04.2019
- Added check is temperature file is empty and remove print stacktrace
- Changed attribute name `app.version_name` to `version`

## Version 1.2.0 - 07.04.2019
- `BacktraceDatabase` - offline error report storage and auto re-submission support in the event of network outage and server unavailability,

## Version 1.1.2 - 07.03.2019
- Added class name to function name in exception StackFrame
- Added exception message to annotations

## Version 1.1.1 - 26.02.2019
- Fixed exception on filter out Backtrace files from StackTraceElements when file name is null

## Version 1.1.0 - 25.02.2019
- Added support for file attachments and annotations
- Added battery level and status attributes
- Added screen brightness attribute
- Fixed Android version attribute
- Fixed bug with negative number of lines in stacktrace
- Filtered out the frames from the Backtrace library
- Simplified BacktraceClient class, removed 'send' and 'async' methods overwrites
- Replaced spaces with underscore in all enums strings

## Version 1.0 - 27.01.2019
- First release.
