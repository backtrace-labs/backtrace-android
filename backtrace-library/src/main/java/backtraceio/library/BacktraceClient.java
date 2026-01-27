package backtraceio.library;

import android.content.Context;
import backtraceio.library.anr.AnrType;
import backtraceio.library.anr.BacktraceANRHandler;
import backtraceio.library.anr.BacktraceANRSettings;
import backtraceio.library.anr.BacktraceAppExitInfoSenderHandler;
import backtraceio.library.base.BacktraceBase;
import backtraceio.library.events.OnServerResponseEventListener;
import backtraceio.library.interfaces.Database;
import backtraceio.library.models.database.BacktraceDatabaseSettings;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.watchdog.BacktraceANRHandlerWatchdog;
import backtraceio.library.watchdog.OnApplicationNotRespondingEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Backtrace Java Android Client
 */
public class BacktraceClient extends BacktraceBase {

    /**
     * Backtrace ANR watchdog instance
     */
    private BacktraceANRHandler anrHandler;

    /**
     * Initializing Backtrace client instance with BacktraceCredentials
     *
     * @param context     application context
     * @param credentials credentials to Backtrace API server
     */
    public BacktraceClient(Context context, BacktraceCredentials credentials) {
        this(context, credentials, (BacktraceDatabase) null);
    }

    /**
     * Initializing Backtrace client instance with BacktraceCredentials
     *
     * @param context     application context
     * @param credentials credentials to Backtrace API server
     * @param attachments File attachment paths to consider for reports
     * @note Attachments for native crashes must be specified here, and cannot be changed during runtime
     */
    public BacktraceClient(Context context, BacktraceCredentials credentials, List<String> attachments) {
        this(context, credentials, (BacktraceDatabase) null, attachments);
    }

    /**
     * Initializing Backtrace client instance with BacktraceCredentials
     *
     * @param context     application context
     * @param credentials credentials to Backtrace API server
     * @param attributes  additional information about current application
     */
    public BacktraceClient(Context context, BacktraceCredentials credentials, Map<String, Object> attributes) {
        this(context, credentials, (BacktraceDatabase) null, attributes);
    }

    /**
     * Initializing Backtrace client instance with BacktraceCredentials
     *
     * @param context     application context
     * @param credentials credentials to Backtrace API server
     * @param attributes  additional information about current application
     * @param attachments File attachment paths to consider for reports
     * @note Attachments for native crashes must be specified here, and cannot be changed during runtime
     */
    public BacktraceClient(
            Context context,
            BacktraceCredentials credentials,
            Map<String, Object> attributes,
            List<String> attachments) {
        this(context, credentials, (BacktraceDatabase) null, attributes, attachments);
    }

    /**
     * Initialize new client instance with BacktraceCredentials
     *
     * @param context          context of current state of the application
     * @param credentials      Backtrace credentials to access Backtrace API
     * @param databaseSettings Backtrace database settings
     */
    public BacktraceClient(
            Context context, BacktraceCredentials credentials, BacktraceDatabaseSettings databaseSettings) {
        this(context, credentials, new BacktraceDatabase(context, databaseSettings));
    }

    /**
     * Initialize new client instance with BacktraceCredentials
     *
     * @param context          context of current state of the application
     * @param credentials      Backtrace credentials to access Backtrace API
     * @param databaseSettings Backtrace database settings
     * @note Attachments for native crashes must be specified here, and cannot be changed during runtime
     */
    public BacktraceClient(
            Context context,
            BacktraceCredentials credentials,
            BacktraceDatabaseSettings databaseSettings,
            List<String> attachments) {
        this(context, credentials, new BacktraceDatabase(context, databaseSettings), attachments);
    }

    /**
     * Initialize new client instance with BacktraceCredentials
     *
     * @param context          context of current state of the application
     * @param credentials      Backtrace credentials to access Backtrace API
     * @param databaseSettings Backtrace database settings
     * @param attributes       additional information about current application
     */
    public BacktraceClient(
            Context context,
            BacktraceCredentials credentials,
            BacktraceDatabaseSettings databaseSettings,
            Map<String, Object> attributes) {
        this(context, credentials, new BacktraceDatabase(context, databaseSettings), attributes);
    }

    /**
     * Initialize new client instance with BacktraceCredentials
     *
     * @param context          context of current state of the application
     * @param credentials      Backtrace credentials to access Backtrace API
     * @param databaseSettings Backtrace database settings
     * @param attributes       additional information about current application
     * @note Attachments for native crashes must be specified here, and cannot be changed during runtime
     */
    public BacktraceClient(
            Context context,
            BacktraceCredentials credentials,
            BacktraceDatabaseSettings databaseSettings,
            Map<String, Object> attributes,
            List<String> attachments) {
        this(context, credentials, new BacktraceDatabase(context, databaseSettings), attributes, attachments);
    }

    /**
     * Initialize new client instance with BacktraceCredentials
     *
     * @param context     context of current state of the application
     * @param credentials Backtrace credentials to access Backtrace API
     * @param database    Backtrace database
     */
    public BacktraceClient(Context context, BacktraceCredentials credentials, Database database) {
        this(context, credentials, database, new HashMap<String, Object>());
    }

    /**
     * Initialize new client instance with BacktraceCredentials
     *
     * @param context     context of current state of the application
     * @param credentials Backtrace credentials to access Backtrace API
     * @param database    Backtrace database
     * @note Attachments for native crashes must be specified here, and cannot be changed during runtime
     */
    public BacktraceClient(
            Context context, BacktraceCredentials credentials, Database database, List<String> attachments) {
        this(context, credentials, database, null, attachments);
    }

    /**
     * Initialize new client instance with BacktraceCredentials
     *
     * @param context     context of current state of the application
     * @param credentials Backtrace credentials to access Backtrace API
     * @param database    Backtrace database
     * @param attributes  additional information about current application
     */
    public BacktraceClient(
            Context context, BacktraceCredentials credentials, Database database, Map<String, Object> attributes) {
        this(context, credentials, database, attributes, null);
    }

    /**
     * Initialize new client instance with BacktraceCredentials
     *
     * @param context     context of current state of the application
     * @param credentials Backtrace credentials to access Backtrace API
     * @param database    Backtrace database
     * @param attributes  additional information about current application
     * @param attachments File attachment paths to consider for reports
     * @note Attachments for native crashes must be specified here, and cannot be changed during runtime
     */
    public BacktraceClient(
            Context context,
            BacktraceCredentials credentials,
            Database database,
            Map<String, Object> attributes,
            List<String> attachments) {
        super(context, credentials, database, attributes, attachments);
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
     * Sending an throwable to Backtrace API
     *
     * @param throwable current throwable
     */
    public void send(Throwable throwable) {
        this.send(throwable, null);
    }

    /**
     * Sending an throwable to Backtrace API
     *
     * @param throwable                   current throwable
     * @param serverResponseEventListener event callback that will be executed after receiving a response from the server
     */
    public void send(Throwable throwable, OnServerResponseEventListener serverResponseEventListener) {
        this.send(throwable, null, serverResponseEventListener);
    }

    /**
     * Sending an throwable to Backtrace API
     *
     * @param throwable                   current throwable
     * @param attributes                  throwable attributes
     * @param serverResponseEventListener event callback that will be executed after receiving a response from the server
     */
    public void send(
            Throwable throwable,
            Map<String, Object> attributes,
            OnServerResponseEventListener serverResponseEventListener) {
        super.send(new BacktraceReport(throwable, attributes), serverResponseEventListener);
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
    public void send(BacktraceReport report, OnServerResponseEventListener serverResponseEventListener) {
        super.send(report, serverResponseEventListener);
    }

    /**
     * Start monitoring if the main thread has been blocked
     */
    public void enableAnr() {
        this.enableAnr(AnrType.Threshold);
    }

    public void enableAnr(AnrType anrType) {
        this.enableAnr(anrType, new BacktraceANRSettings());
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
        this.enableAnr(new BacktraceANRSettings(timeout, onApplicationNotRespondingEvent, debug));
    }

    public void enableAnr(BacktraceANRSettings anrSettings) {
        this.enableAnr(AnrType.Threshold, anrSettings);
    }

    public void enableAnr(AnrType anrType, BacktraceANRSettings anrSettings) {
        this.anrHandler = initAnrHandler(anrType, anrSettings);
    }

    /**
     * Stop monitoring if the main thread has been blocked
     */
    public void disableAnr() {
        if (this.anrHandler != null) {
            this.anrHandler.stopMonitoringAnr();
        }
    }

    public BacktraceANRHandler initAnrHandler(AnrType anrType, BacktraceANRSettings backtraceANRSettings) {
        BacktraceANRHandler handler = createBacktraceAnrHandler(anrType, backtraceANRSettings);
        if (backtraceANRSettings.getOnApplicationNotRespondingEvent() != null) {
            handler.setOnApplicationNotRespondingEvent(backtraceANRSettings.getOnApplicationNotRespondingEvent());
        }
        return handler;
    }

    public BacktraceANRHandler createBacktraceAnrHandler(AnrType anrType, BacktraceANRSettings settings) {
        if (anrType == AnrType.ApplicationExit) {
            return new BacktraceAppExitInfoSenderHandler(this, context);
        } else if (anrType == AnrType.Threshold) {
            return new BacktraceANRHandlerWatchdog(this, settings.getTimeout(), settings.isDebug());
        }
        throw new IllegalArgumentException("Unsupported type of ANR: " + anrType.name());
    }
}
