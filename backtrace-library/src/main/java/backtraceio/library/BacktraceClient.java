package backtraceio.library;

import android.content.Context;

import backtraceio.library.base.BacktraceBase;
import backtraceio.library.events.OnServerResponseEventListener;
import backtraceio.library.interfaces.IBacktraceDatabase;
import backtraceio.library.models.database.BacktraceDatabaseSettings;
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
     * Initialize new client instance with BacktraceCredentials
     *
     * @param context          context of current state of the application
     * @param credentials      Backtrace credentials to access Backtrace API
     * @param databaseSettings Backtrace database settings
     */
    public BacktraceClient(Context context, BacktraceCredentials credentials,
                           BacktraceDatabaseSettings databaseSettings) {
        super(context, credentials, databaseSettings);
    }

    /**
     * Initialize new client instance with BacktraceCredentials
     *
     * @param context     context of current state of the application
     * @param credentials Backtrace credentials to access Backtrace API
     * @param database    Backtrace database
     */
    public BacktraceClient(Context context, BacktraceCredentials credentials, IBacktraceDatabase
            database) {
        super(context, credentials, database);
    }

    /**
     * Sending a message to Backtrace API
     *
     * @param message custom client message
     */
    public void send(String message) {
        this.send(message, null);
    }

    /**
     * Sending a message to Backtrace API
     *
     * @param message                     custom client message
     * @param serverResponseEventListener
     */
    public void send(String message, OnServerResponseEventListener serverResponseEventListener) {
        super.send(new BacktraceReport(message), serverResponseEventListener);
    }

    /**
     * Sending an exception to Backtrace API
     *
     * @param exception current exception
     */
    public void send(Exception exception) {
        this.send(exception, null);
    }

    /**
     * Sending an exception to Backtrace API
     *
     * @param exception current exception
     * @param serverResponseEventListener
     */
    public void send(Exception exception, OnServerResponseEventListener
            serverResponseEventListener) {
        super.send(new BacktraceReport(exception), serverResponseEventListener);
    }

    /**
     * Sending a Backtrace report to Backtrace API
     *
     * @param report current BacktraceReport
     */
    public void send(BacktraceReport report) {
        send(report, null);
    }

    /**
     * Sending a Backtrace report to Backtrace API
     *
     * @param report                      current BacktraceReport
     * @param serverResponseEventListener
     */
    public void send(BacktraceReport report, OnServerResponseEventListener
            serverResponseEventListener) {
        super.send(report, serverResponseEventListener);
    }
}