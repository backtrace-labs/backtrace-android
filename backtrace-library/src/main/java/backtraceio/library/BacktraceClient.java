package backtraceio.library;

import android.content.Context;
import android.os.AsyncTask;

import backtraceio.library.base.BacktraceBase;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.json.BacktraceReport;

/**
 * Backtrace Java Android Client
 */
public class BacktraceClient extends BacktraceBase {

    /**
     * Initializing Backtrace client instance with BacktraceCredentials
     *
     * @param context     application context
     * @param credentials credentials to Backtrace API server
     */
    public BacktraceClient(Context context, BacktraceCredentials credentials) {
        super(context, credentials);
    }

    /**
     * Sending a message to Backtrace API
     *
     * @param message custom client message
     * @return server response
     */
    public BacktraceResult send(String message) {
        return super.send(message);
    }

    /**
     * Sending an exception to Backtrace API
     *
     * @param exception current exception
     * @return server response
     */
    public BacktraceResult send(Exception exception) {
        return super.send(exception);
    }

    /**
     * Sending a Backtrace report to Backtrace API
     *
     * @param report current BacktraceReport
     * @return server response
     */
    public BacktraceResult send(BacktraceReport report) {
        return super.send(report);
    }

    /**
     * Sending asynchronously a message to Backtrace API
     *
     * @param message custom client message
     * @return async task which send data to Backtrace API and return server response
     */
    public AsyncTask<Void, Void, BacktraceResult> sendAsync(String message) {
        return super.sendAsync(message);
    }

    /**
     * Sending asynchronously an Exception to Backtrace API
     *
     * @param exception current exception
     * @return async task which send data to Backtrace API and return server response
     */
    public AsyncTask<Void, Void, BacktraceResult> sendAsync(Exception exception) {
        return super.sendAsync(exception);
    }

    /**
     * Sending asynchronously a Backtrace report to Backtrace API
     *
     * @param report current BacktraceReport
     * @return async task which send data to Backtrace API and return server response
     */
    public AsyncTask<Void, Void, BacktraceResult> sendAsync(BacktraceReport report) {
        return super.sendAsync(report);
    }
}