package backtraceio.library.logger;

/**
 * Defines the logging levels for the Backtrace library.
 * <p>
 * Controls the severity of log messages:
 * <ul>
 *     <li>{@link #DEBUG} - Logs detailed debug messages.</li>
 *     <li>{@link #WARN} - Logs warnings indicating potential issues.</li>
 *     <li>{@link #ERROR} - Logs error messages for failures.</li>
 *     <li>{@link #OFF} - Disables all logging.</li>
 * </ul>
 */
public enum LogLevel {
    /**
     * logging level designed for logging debug messages
     */
    DEBUG,
    /**
     * logging level designed for logging warning messages
     */
    WARN,
    /**
     * logging level designed for logging errors messages
     */
    ERROR,
    /**
     * No messages will be logged
     */
    OFF
}
