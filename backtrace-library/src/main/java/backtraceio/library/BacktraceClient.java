package backtraceio.library;

import android.content.Context;
import android.os.AsyncTask;

import backtraceio.library.base.BacktraceBase;
import backtraceio.library.events.OnServerResponseEventListener;
import backtraceio.library.interfaces.IBacktraceDatabase;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.models.BacktraceResult;
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
    public BacktraceClient(Context context, BacktraceCredentials credentials, BacktraceDatabaseSettings databaseSettings)
    {
        super(context, credentials, databaseSettings);
    }

    /**
     * Initialize new client instance with BacktraceCredentials
     *
     * @param context     context of current state of the application
     * @param credentials Backtrace credentials to access Backtrace API
     * @param database    Backtrace database
     */
    public BacktraceClient(Context context, BacktraceCredentials credentials, IBacktraceDatabase database) {
        super(context, credentials, database);
    }

    /**
     * Sending a message to Backtrace API
     *
     * @param message custom client message
     */
    public void send(String message) {
        super.send(new BacktraceReport(message));
    }

//    public void sendWithThreadHandler(BacktraceReport report, OnServerResponseEventListener serverResponseEventListener) {
//        super.sendThreadHandler(report, serverResponseEventListener);
//    }

    /**
     * Sending an exception to Backtrace API
     *
     * @param exception current exception
     */
    public void send(Exception exception) {
        super.send(new BacktraceReport(exception));
    }

    /**
     * Sending a Backtrace report to Backtrace API
     *
     * @param report current BacktraceReport
     */
    public void send(BacktraceReport report) {
        super.send(report);
    }
}