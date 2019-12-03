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
}
