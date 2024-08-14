package backtraceio.library.logger;

/**
 * Interface for logging messages in the Backtrace library.
 * <p>
 * Defines methods for logging at various levels, such as debug, warning, and error.
 * Implementing classes will provide the actual logging functionality, ensuring consistent
 * message logging across the library.
 * </p>
 */
public interface Logger {
    int d(String tag, String message);
    int w(String tag, String message);
    int e(String tag, String message);
    int e(String tag, String message, Throwable tr);
}