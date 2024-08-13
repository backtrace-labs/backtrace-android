package backtraceio.library.logger;

/**
 * TODO: improve Backtrace Logger class for logging messages from inside library
 */
public interface Logger {

    int d(String tag, String message);

    int w(String tag, String message);

    int e(String tag, String message);

    int e(String tag, String message, Throwable tr);
}