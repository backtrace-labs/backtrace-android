package backtraceio.library.anr;

import backtraceio.library.watchdog.OnApplicationNotRespondingEvent;

/**
 * Configuration settings for Application Not Responding (ANR) detection.
 * This class allows customization of ANR monitoring behavior, such as the timeout duration
 * and callback events.
 */
public class BacktraceANRSettings {
    /**
     * Default timeout value in milliseconds
     */
    public static final int DEFAULT_ANR_TIMEOUT = 5000;

    /**
     * The timeout in milliseconds after which an ANR is reported if the main thread is blocked.
     */
    private final int timeout;

    /**
     * Flag to enable or disable additional debug logging for ANR detection.
     * When true, more verbose logging related to ANR monitoring might be produced.
     */
    private final boolean debug;

    /**
     * Callback interface to be invoked when an Application Not Responding event is detected.
     * This allows custom handling of ANR events.
     */
    private final OnApplicationNotRespondingEvent onApplicationNotRespondingEvent;

    /**
     * Default constructor.
     * Initializes ANR settings with default values.
     */
    public BacktraceANRSettings() {
        this(DEFAULT_ANR_TIMEOUT, null, false);
    }

    /**
     * Constructs ANR settings with specified parameters.
     *
     * @param timeout                         The timeout in milliseconds for ANR detection.
     * @param onApplicationNotRespondingEvent The callback to be invoked when an ANR is detected.
     *                                        Can be null if no custom callback is needed.
     * @param debug                           True to enable debug logging for ANR detection, false otherwise.
     */
    public BacktraceANRSettings(
            int timeout, OnApplicationNotRespondingEvent onApplicationNotRespondingEvent, boolean debug) {
        this.timeout = timeout;
        this.onApplicationNotRespondingEvent = onApplicationNotRespondingEvent;
        this.debug = debug;
    }

    /**
     * Gets the configured ANR timeout in milliseconds.
     *
     * @return The timeout in milliseconds.
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * Checks if debug logging for ANR detection is enabled.
     *
     * @return True if debug logging is enabled, false otherwise.
     */
    public boolean isDebug() {
        return debug;
    }

    /**
     * Gets the configured callback for ANR events.
     *
     * @return The {@link OnApplicationNotRespondingEvent} callback, or null if none is set.
     */
    public OnApplicationNotRespondingEvent getOnApplicationNotRespondingEvent() {
        return onApplicationNotRespondingEvent;
    }
}
