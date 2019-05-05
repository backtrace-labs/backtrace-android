package backtraceio.library.logger;

import android.util.Log;

/**
 * Backtrace Logger class for logging messages from inside library
 */
public class BacktraceLogger {

    /**
     * Level from which all information is logged
     */
    private static LogLevel logLevel = LogLevel.OFF;
    private static final String BASE_TAG = "BacktraceLogger: ";

    /**
     * set logging level from which all messages should be logged to the console
     * @param level login level
     */
    public static void setLevel(LogLevel level) {
        BacktraceLogger.logLevel = level;
    }

    /**
     *
     * @param tag source of logs, usually identifies the class or activity
     * @param message text information which should be logged
     * @return the number of bytes written
     */
    public static int d(String tag, String message){
        if(BacktraceLogger.logLevel.ordinal() <= LogLevel.DEBUG.ordinal()) {
            return Log.d(getTag(tag), message);
        }
        return 0;
    }

    /**
     * Log messages that suggest something unexpected or rare has happened, which isn't an error.
     * @param tag source of logs, usually identifies the class or activity
     * @param message text information which should be logged
     * @return the number of bytes written
     */
    public static int w(String tag, String message){
        if(BacktraceLogger.logLevel.ordinal() <= LogLevel.WARN.ordinal()) {
            return Log.w(getTag(tag), message);
        }
        return 0;
    }

    /**
     * Log messages that suggest error or something that should not happen
     * @param tag source of logs, usually identifies the class or activity
     * @param message text information which should be logged
     * @return the number of bytes written
     */
    public static int e(String tag, String message){
        if(BacktraceLogger.logLevel.ordinal() <= LogLevel.ERROR.ordinal()) {
            return Log.e(getTag(tag), message);
        }
        return 0;
    }

    /**
     * Log messages that suggest error or something that should not happen
     * @param tag source of logs, usually identifies the class or activity
     * @param message text information which should be logged
     * @param tr an exception to log
     * @return the number of bytes written
     */
    public static int e(String tag, String message, Throwable tr){
        if(BacktraceLogger.logLevel.ordinal() <= LogLevel.ERROR.ordinal()) {
            return Log.e(getTag(tag), message, tr);
        }
        return 0;
    }

    private static String getTag(String tag){
        return BacktraceLogger.BASE_TAG + tag;
    }
}