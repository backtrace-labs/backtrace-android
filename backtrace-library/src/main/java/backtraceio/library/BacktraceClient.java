package backtraceio.library;

import android.content.Context;
import android.provider.ContactsContract;


import java.util.Map;

import backtraceio.library.watchdog.BacktraceANRWatchdog;
import backtraceio.library.watchdog.OnApplicationNotRespondingEvent;
import backtraceio.library.base.BacktraceBase;
import backtraceio.library.events.OnServerResponseEventListener;
import backtraceio.library.interfaces.Database;
import backtraceio.library.models.database.BacktraceDatabaseSettings;
import backtraceio.library.models.json.BacktraceReport;

/**
 * Backtrace Java Android Client
 */
public class BacktraceClient extends BacktraceBase {

    /**
     *
     */
    private BacktraceANRWatchdog anrWatchdog;


    /**
     * Initializing Backtrace client instance with BacktraceCredentials
     *
     * @param context     application context
     * @param credentials credentials to Backtrace API server
     */
    public BacktraceClient(Context context, BacktraceCredentials credentials) {
        this(context, credentials, (BacktraceDatabase)null, null);
    }

    /**
     * Initializing Backtrace client instance with BacktraceCredentials
     *
     * @param context     application context
     * @param credentials credentials to Backtrace API server
     */
    public BacktraceClient(Context context, BacktraceCredentials credentials, Map<String, Object> attributes) {
        this(context, credentials, (BacktraceDatabase)null, attributes);
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
        this(context, credentials,  new BacktraceDatabase(context, databaseSettings));
    }

    /**
     * Initialize new client instance with BacktraceCredentials
     *
     * @param context          context of current state of the application
     * @param credentials      Backtrace credentials to access Backtrace API
     * @param databaseSettings Backtrace database settings
     */
    public BacktraceClient(Context context, BacktraceCredentials credentials,
                           BacktraceDatabaseSettings databaseSettings, Map<String, Object> attributes) {
        this(context, credentials,  new BacktraceDatabase(context, databaseSettings), attributes);
    }

    /**
     * Initialize new client instance with BacktraceCredentials
     *
     * @param context     context of current state of the application
     * @param credentials Backtrace credentials to access Backtrace API
     * @param database    Backtrace database
     */
    public BacktraceClient(Context context, BacktraceCredentials credentials,
                           Database database) {
        this(context, credentials, database, null);
    }

    /**
     * Initialize new client instance with BacktraceCredentials
     *
     * @param context     context of current state of the application
     * @param credentials Backtrace credentials to access Backtrace API
     * @param database    Backtrace database
     */
    public BacktraceClient(Context context, BacktraceCredentials credentials,
                           Database database, Map<String, Object> attributes) {
        super(context, credentials, database, attributes);
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
     * @param serverResponseEventListener event callback that will be executed after receiving a response from the server
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
     * @param exception                   current exception
     * @param serverResponseEventListener event callback that will be executed after receiving a response from the server
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
     * @param serverResponseEventListener event callback that will be executed after receiving a response from the server
     */
    public void send(BacktraceReport report, OnServerResponseEventListener
            serverResponseEventListener) {
        super.send(report, serverResponseEventListener);
    }


    /**
     * Start monitoring if the main thread has been blocked
     */
    public void enableAnr() {
        this.anrWatchdog = new BacktraceANRWatchdog(this);
    }

    /**
     * Start monitoring if the main thread has been blocked
     *
     * @param timeout maximum time in milliseconds after which should check if the main thread is not hanged
     */
    public void enableAnr(int timeout) {
        this.enableAnr(timeout, null);
    }

    /**
     * Start monitoring if the main thread has been blocked
     *
     * @param timeout                         maximum time in milliseconds after which should check if the main thread is not hanged
     * @param onApplicationNotRespondingEvent event that will be executed instead of the default sending of the error information to the Backtrace console
     */
    public void enableAnr(int timeout, OnApplicationNotRespondingEvent onApplicationNotRespondingEvent) {
        this.enableAnr(timeout, onApplicationNotRespondingEvent, false);
    }

    /**
     * Start monitoring if the main thread has been blocked
     *
     * @param timeout maximum time in milliseconds after which should check if the main thread is not hanged
     * @param debug   enable debug mode - errors will not be sent if the debugger is connected
     */
    public void enableAnr(int timeout, boolean debug) {
        this.enableAnr(timeout, null, debug);
    }

    /**
     * Start monitoring if the main thread has been blocked
     *
     * @param timeout                         maximum time in milliseconds after which should check if the main thread is not hanged
     * @param onApplicationNotRespondingEvent event that will be executed instead of the default sending of the error information to the Backtrace console
     * @param debug                           enable debug mode - errors will not be sent if the debugger is connected
     */
    public void enableAnr(int timeout, OnApplicationNotRespondingEvent onApplicationNotRespondingEvent, boolean debug) {
        this.anrWatchdog = new BacktraceANRWatchdog(this, timeout, debug);
        this.anrWatchdog.setOnApplicationNotRespondingEvent(onApplicationNotRespondingEvent);
    }

    /**
     * Stop monitoring if the main thread has been blocked
     */
    public void disableAnr() {
        if (this.anrWatchdog != null && !this.anrWatchdog.isInterrupted()) {
            this.anrWatchdog.stopMonitoringAnr();
        }
    }
}