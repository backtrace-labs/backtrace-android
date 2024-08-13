package backtraceio.library.logger;

import android.util.Log;

/**
 * TODO: improve Backtrace Logger implementation class for logging messages from inside library
 */
public class BacktraceLogLogger implements Logger {

    private static final String BASE_TAG = "BacktraceLogger: ";
    /**
     * Level from which all information is logged
     */
    private LogLevel logLevel;


    public BacktraceLogLogger() {
        this(LogLevel.OFF);
    }

    public BacktraceLogLogger(LogLevel logLevel) {
        this.logLevel = logLevel;
    }
    /**
     * set logging level from which all messages should be logged to the console
     *
     * @param level login level
     */
    public void setLevel(LogLevel level) {
        this.logLevel = level;
    }

    /**
     * @param tag     source of logs, usually identifies the class or activity
     * @param message text information which should be logged
     * @return the number of bytes written
     */
    public int d(String tag, String message) {
        if (this.logLevel.ordinal() <= LogLevel.DEBUG.ordinal()) {
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
        if (this.logLevel.ordinal() <= LogLevel.WARN.ordinal()) {
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
        if (this.logLevel.ordinal() <= LogLevel.ERROR.ordinal()) {
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
        if (this.logLevel.ordinal() <= LogLevel.ERROR.ordinal()) {
            return Log.e(this.getTag(tag), message, tr);
        }
        return 0;
    }

    private String getTag(String tag) {
        return BacktraceLogLogger.BASE_TAG + tag;
    }
}