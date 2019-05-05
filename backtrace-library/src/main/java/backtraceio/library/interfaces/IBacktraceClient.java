package backtraceio.library.interfaces;

import backtraceio.library.models.json.BacktraceReport;

/**
 * Backtrace client interface.
 */
public interface IBacktraceClient {
    /**
     * Send new report to a Backtrace API
     *
     * @param report data which should be send to Backtrace API
     */
    void send(BacktraceReport report);
}
