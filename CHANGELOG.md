# Backtrace Android Release Notes

## Version 1.2.1 - 12.04.2019
- Check is temperature file is empty and remove print stacktrace
- Change attribute name `app.version_name` to `version`

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