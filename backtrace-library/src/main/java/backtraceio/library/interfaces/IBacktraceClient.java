package backtraceio.library.interfaces;

import android.os.AsyncTask;

import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.json.BacktraceReport;

/**
 * Backtrace client interface.
 */
public interface IBacktraceClient {
    /**
     * Send new report to a Backtrace API
     * @param report data which should be send to Backtrace API
     * @return server response
     */
    BacktraceResult send(BacktraceReport report);

    /**
     * Send asynchronously new report to a Backtrace API
     * @param report data which should be send to Backtrace API
     * @return async task which send data to Backtrace API and return server response
     */
    AsyncTask<Void, Void, BacktraceResult> sendAsync(BacktraceReport report);
}
