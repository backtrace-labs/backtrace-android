package backtraceio.library.logger;

import org.jetbrains.annotations.NotNull;

/**
 * A global logging utility for the Backtrace library.
 * <p>
 * {@code BacktraceLogger} acts as a wrapper around a {@link Logger} implementation,
 * providing a centralized logging mechanism throughout the library. By default, it uses
 * the {@link BacktraceLogLogger}, which relies on {@code android.util.Log} for logging.
 * However, the logger can be replaced with a custom implementation by using the
 * {@link #setLogger(Logger)} method.
 * </p>
 * <p>
 * This allows for flexibility in logging strategies, enabling developers to integrate
 * their preferred logging framework or customize log handling as needed.
 * </p>
 */
public class BacktraceLogger {

    private static Logger logger = new BacktraceLogLogger();

    public static Logger getLogger() {
        return logger;
    }

    public static void setLogger(@NotNull Logger logger) {
        if (logger == null) {
           throw new IllegalArgumentException("Passed custom logger implementation can`t be null");
        }
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

    /**
     * Set logging level from which all messages should be logged to the console
     * @param level login level (debug, warn, error, off)
     *
     * @deprecated setting the logging level should be done directly in the passed custom logger
     * implementation so as not to depend on the internal implementation of Backtrace internal
     * LogLevel types.
     *
     */
    @Deprecated
    public static void setLevel(@NotNull LogLevel level) {
        logger.setLevel(level);
    }
}