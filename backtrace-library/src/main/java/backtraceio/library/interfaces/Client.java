package backtraceio.library.interfaces;

import backtraceio.library.models.json.BacktraceReport;

/**
 * Client interface.
 */
public interface Client {
    /**
     * Send new report to a Backtrace API
     *
     * @param report data which should be send to Backtrace API
     */
    void send(BacktraceReport report);

    /**
     * Capture unhandled native exceptions (Backtrace database integration is required to enable this feature).
     */
    void enableNativeIntegration();
}
