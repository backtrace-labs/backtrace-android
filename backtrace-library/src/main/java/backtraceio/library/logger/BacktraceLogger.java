package backtraceio.library.logger;

/**
 * TODO: improve Backtrace Logger class for logging messages from inside library
 */
public class BacktraceLogger {

    private static Logger logger = new BacktraceLogLogger();

    public static void setLogger(Logger logger) {
        BacktraceLogger.logger = logger;
    }

    /**
     * @param tag     source of logs, usually identifies the class or activity
     * @param message text information which should be logged
     * @return the number of bytes written
     */
    public static int d(String tag, String message) {
        return logger.d(tag, message);
    }

    /**
     * Log messages that suggest something unexpected or rare has happened, which isn't an error.
     *
     * @param tag     source of logs, usually identifies the class or activity
     * @param message text information which should be logged
     * @return the number of bytes written
     */
    public static int w(String tag, String message) {
        return logger.w(tag, message);
    }

    /**
     * Log messages that suggest error or something that should not happen
     *
     * @param tag     source of logs, usually identifies the class or activity
     * @param message text information which should be logged
     * @return the number of bytes written
     */
    public static int e(String tag, String message) {
        return logger.e(tag, message);
    }

    /**
     * Log messages that suggest error or something that should not happen
     *
     * @param tag     source of logs, usually identifies the class or activity
     * @param message text information which should be logged
     * @param tr      an exception to log
     * @return the number of bytes written
     */
    public static int e(String tag, String message, Throwable tr) {
        return logger.e(tag, message, tr);
    }
}