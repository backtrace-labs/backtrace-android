package backtraceio.library.logger;

/**
 * The class is intended to determine the available levels of login messages
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
