package backtraceio.library.logger;

import android.util.Log;

import org.jetbrains.annotations.NotNull;

/**
 * Backtrace Logger implementation class for logging messages within the Backtrace library.
 * <p>
 * This class leverages the {@link android.util.Log} package to output log messages.
 * It provides a standardized way to log information, warnings, errors, and debug messages
 * from within the library, making it easier to track and diagnose issues.
 * </p>
 */
public class BacktraceInternalLogger implements Logger {

    private static final String BASE_TAG = "BacktraceLogger: ";

    /**
     * Level from which all information is logged
     */
    private int logLevel;

    public BacktraceInternalLogger() {
        this(LogLevel.OFF);
    }

    public BacktraceInternalLogger(@NotNull LogLevel logLevel) {
        this.logLevel = logLevel.ordinal();
    }

    public int getLogLevel() {
        return logLevel;
    }

    /**
     * set logging level from which all messages should be logged to the console
     *
     * @param level login level
     */
    public void setLevel(@NotNull LogLevel level) {
        this.logLevel = level.ordinal();
    }

    /**
     * @param tag     source of logs, usually identifies the class or activity
     * @param message text information which should be logged
     * @return the number of bytes written
     */
    public int d(String tag, String message) {
        if (this.logLevel <= LogLevel.DEBUG.ordinal()) {
            return Log.d(this.getTag(tag), message);
        }
        return 0;
    }

    /**
     * Log messages that suggest something unexpected or rare has happened, which isn't an error.
     *
     * @param tag     source of logs, usually identifies the class or activity
     * @param message text information which should be logged
     * @return the number of bytes written
     */
    public int w(String tag, String message) {
        if (this.logLevel <= LogLevel.WARN.ordinal()) {
            return Log.w(this.getTag(tag), message);
        }
        return 0;
    }

    /**
     * Log messages that suggest error or something that should not happen
     *
     * @param tag     source of logs, usually identifies the class or activity
     * @param message text information which should be logged
     * @return the number of bytes written
     */
    public int e(String tag, String message) {
        if (this.logLevel <= LogLevel.ERROR.ordinal()) {
            return Log.e(this.getTag(tag), message);
        }
        return 0;
    }

    /**
     * Log messages that suggest error or something that should not happen
     *
     * @param tag     source of logs, usually identifies the class or activity
     * @param message text information which should be logged
     * @param tr      an exception to log
     * @return the number of bytes written
     */
    public int e(String tag, String message, Throwable tr) {
        if (this.logLevel <= LogLevel.ERROR.ordinal()) {
            return Log.e(this.getTag(tag), message, tr);
        }
        return 0;
    }

    private String getTag(String tag) {
        return BacktraceInternalLogger.BASE_TAG + tag;
    }
}
