# Backtrace Android Release Notes


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
