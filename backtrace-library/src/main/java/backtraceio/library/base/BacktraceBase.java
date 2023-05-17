package backtraceio.library.base;

import android.content.Context;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import backtraceio.library.BacktraceCredentials;
import backtraceio.library.BacktraceDatabase;
import backtraceio.library.enums.BacktraceBreadcrumbLevel;
import backtraceio.library.enums.BacktraceBreadcrumbType;
import backtraceio.library.enums.UnwindingMode;
import backtraceio.library.events.OnBeforeSendEventListener;
import backtraceio.library.events.OnServerErrorEventListener;
import backtraceio.library.events.OnServerResponseEventListener;
import backtraceio.library.events.RequestHandler;
import backtraceio.library.interfaces.Api;
import backtraceio.library.interfaces.Breadcrumbs;
import backtraceio.library.interfaces.Client;
import backtraceio.library.interfaces.Database;
import backtraceio.library.interfaces.Metrics;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.database.BacktraceDatabaseRecord;
import backtraceio.library.models.database.BacktraceDatabaseSettings;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.models.types.BacktraceResultStatus;
import backtraceio.library.services.BacktraceApi;
import backtraceio.library.services.BacktraceMetrics;

/**
 * Base Backtrace Android client
 */
public class BacktraceBase implements Client {

    private static final transient String LOG_TAG = BacktraceBase.class.getSimpleName();

    static {
        System.loadLibrary("backtrace-native");
    }

    /**
     * Backtrace database instance
     */
    public final Database database;

    /**
     * Backtrace client version
     */
    public static String version = backtraceio.library.BuildConfig.VERSION_NAME;

    /**
     * Get custom client attributes. Every argument stored in dictionary will be send to Backtrace API
     */
    public final Map<String, Object> attributes;

    /**
     * File attachments to attach to crashes and reports.
     */
    public final List<String> attachments;
    private final BacktraceCredentials credentials;

    /**
     * Backtrace metrics instance
     */
    public Metrics metrics = null;

    /**
     * Application context
     */
    protected Context context;

    /**
     * Instance of BacktraceApi that allows to send data to Backtrace API
     */
    private Api backtraceApi;

    /**
     * Event which will be executed before sending data to Backtrace API
     */
    private OnBeforeSendEventListener beforeSendEventListener = null;

    /**
     * Is Proguard symbolication enabled? We have to inform the Backtrace API
     */
    private boolean isProguardEnabled = false;

    /**
     * Initialize new client instance with BacktraceCredentials
     *
     * @param context     context of current state of the application
     * @param credentials Backtrace credentials to access Backtrace API
     */
    public BacktraceBase(Context context, BacktraceCredentials credentials) {
        this(context, credentials, (Database) null);
    }

    /**
     * Initialize new client instance with BacktraceCredentials
     *
     * @param context     context of current state of the application
     * @param credentials Backtrace credentials to access Backtrace API
     * @param attachments File attachment paths to consider for reports
     * @note Attachments for native crashes must be specified here, and cannot be changed during runtime
     */
    public BacktraceBase(Context context, BacktraceCredentials credentials, List<String> attachments) {
        this(context, credentials, (Database) null, attachments);
    }

    /**
     * Initialize new client instance with BacktraceCredentials
     *
     * @param context     context of current state of the application
     * @param credentials Backtrace credentials to access Backtrace API
     * @param attributes  additional information about current application
     */
    public BacktraceBase(Context context, BacktraceCredentials credentials, Map<String, Object> attributes) {
        this(context, credentials, (Database) null, attributes);
    }

    /**
     * Initialize new client instance with BacktraceCredentials
     *
     * @param context     context of current state of the application
     * @param credentials Backtrace credentials to access Backtrace API
     * @param attributes  additional information about current application
     * @param attachments File attachment paths to consider for reports
     * @note Attachments for native crashes must be specified here, and cannot be changed during runtime
     */
    public BacktraceBase(Context context, BacktraceCredentials credentials, Map<String, Object> attributes, List<String> attachments) {
        this(context, credentials, (Database) null, attributes, attachments);
    }

    /**
     * Initialize new client instance with BacktraceCredentials
     *
     * @param context          context of current state of the application
     * @param credentials      Backtrace credentials to access Backtrace API
     * @param databaseSettings Backtrace database settings
     */
    public BacktraceBase(Context context, BacktraceCredentials credentials, BacktraceDatabaseSettings databaseSettings) {
        this(context, credentials, new BacktraceDatabase(context, databaseSettings));
    }

    /**
     * Initialize new client instance with BacktraceCredentials
     *
     * @param context          context of current state of the application
     * @param credentials      Backtrace credentials to access Backtrace API
     * @param databaseSettings Backtrace database settings
     * @param attachments      File attachment paths to consider for reports
     * @note Attachments for native crashes must be specified here, and cannot be changed during runtime
     */
    public BacktraceBase(Context context, BacktraceCredentials credentials, BacktraceDatabaseSettings databaseSettings, List<String> attachments) {
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
    public BacktraceBase(Context context, BacktraceCredentials credentials, BacktraceDatabaseSettings databaseSettings, Map<String, Object> attributes) {
        this(context, credentials, new BacktraceDatabase(context, databaseSettings), attributes);
    }

    /**
     * Initialize new client instance with BacktraceCredentials
     *
     * @param context          context of current state of the application
     * @param credentials      Backtrace credentials to access Backtrace API
     * @param databaseSettings Backtrace database settings
     * @param attributes       additional information about current application
     * @param attachments      File attachment paths to consider for reports
     * @note Attachments for native crashes must be specified here, and cannot be changed during runtime
     */
    public BacktraceBase(Context context, BacktraceCredentials credentials, BacktraceDatabaseSettings databaseSettings, Map<String, Object> attributes, List<String> attachments) {
        this(context, credentials, new BacktraceDatabase(context, databaseSettings), attributes, attachments);
    }

    /**
     * Initialize new client instance with BacktraceCredentials
     *
     * @param context     context of current state of the application
     * @param credentials Backtrace credentials to access Backtrace API
     * @param database    Backtrace database
     */
    public BacktraceBase(Context context, BacktraceCredentials credentials, Database database) {
        this(context, credentials, database, (Map<String, Object>) null);
    }

    /**
     * Initialize new client instance with BacktraceCredentials
     *
     * @param context     context of current state of the application
     * @param credentials Backtrace credentials to access Backtrace API
     * @param database    Backtrace database
     * @param attachments File attachment paths to consider for reports
     * @note Attachments for native crashes must be specified here, and cannot be changed during runtime
     */
    public BacktraceBase(Context context, BacktraceCredentials credentials, Database database, List<String> attachments) {
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
    public BacktraceBase(Context context, BacktraceCredentials credentials, Database database, Map<String, Object> attributes) {
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
    public BacktraceBase(Context context, BacktraceCredentials credentials, Database database, Map<String, Object> attributes, List<String> attachments) {
        this.context = context;
        this.credentials = credentials;
        this.attributes = attributes != null ? attributes : new HashMap<String, Object>();
        this.attachments = attachments != null ? attachments : new ArrayList<String>();
        this.database = database != null ? database : new BacktraceDatabase();
        this.setBacktraceApi(new BacktraceApi(credentials));
        this.database.start();
        this.metrics = new BacktraceMetrics(context, attributes, backtraceApi, credentials);
    }

    public native void crash();

    private void setBacktraceApi(Api backtraceApi) {
        this.backtraceApi = backtraceApi;
        if (this.database != null) {
            this.database.setApi(this.backtraceApi);
        }
    }

    /**
     * Capture unhandled native exceptions (Backtrace database integration is required to enable this feature).
     */
    public void enableNativeIntegration() {
        this.database.setupNativeIntegration(this, this.credentials);
    }

    /**
     * Capture unhandled native exceptions (Backtrace database integration is required to enable this feature).
     *
     * @param enableClientSideUnwinding Enable client side unwinding
     */
    public void enableNativeIntegration(boolean enableClientSideUnwinding) {
        this.database.setupNativeIntegration(this, this.credentials, enableClientSideUnwinding);
    }

    /**
     * Capture unhandled native exceptions (Backtrace database integration is required to enable this feature).
     *
     * @param enableClientSideUnwinding Enable client side unwinding
     * @param unwindingMode             Unwinding mode to use for client side unwinding
     */
    public void enableNativeIntegration(boolean enableClientSideUnwinding, UnwindingMode unwindingMode) {
        this.database.setupNativeIntegration(this, this.credentials, enableClientSideUnwinding, unwindingMode);
    }

    public void disableNativeIntegration() {
        this.database.disableNativeIntegration();
    }

    /**
     * Inform Backtrace API that we are using Proguard symbolication
     */
    public void enableProguard() {
        this.isProguardEnabled = true;
    }

    /**
     * Get custom attributes
     *
     * @return map with custom attributes
     */
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /**
     * Set event executed before sending data to Backtrace API
     *
     * @param eventListener object with method which will be executed
     */
    public void setOnBeforeSendEventListener(OnBeforeSendEventListener eventListener) {
        this.beforeSendEventListener = eventListener;
    }

    /**
     * Set an event executed when received bad request, unauthorize request or other information from server
     *
     * @param eventListener object with method which will be executed
     */
    public void setOnServerErrorEventListener(OnServerErrorEventListener eventListener) {
        this.backtraceApi.setOnServerError(eventListener);
    }

    /**
     * Custom request handler for sending Backtrace reports to server
     *
     * @param requestHandler object with method which will be executed
     */
    public void setOnRequestHandler(RequestHandler requestHandler) {
        this.backtraceApi.setRequestHandler(requestHandler);
    }

    /**
     * Enable logging of breadcrumbs and submission with crash reports
     *
     * @param context context of current state of the application
     * @return true if we successfully enabled breadcrumbs
     */
    public boolean enableBreadcrumbs(Context context) {
        if (!isBreadcrumbsAvailable()) {
            return false;
        }
        return database.getBreadcrumbs().enableBreadcrumbs(context);

    }

    /**
     * Enable logging of breadcrumbs and submission with crash reports
     *
     * @param context                   context of current state of the application
     * @param maxBreadcrumbLogSizeBytes breadcrumb log size limit in bytes, should be a power of 2
     * @return true if we successfully enabled breadcrumbs
     * @note breadcrumbTypesToEnable only affects automatic breadcrumb receivers. User created
     * breadcrumbs will always be enabled
     */
    public boolean enableBreadcrumbs(Context context,
                                     int maxBreadcrumbLogSizeBytes) {
        if (!isBreadcrumbsAvailable()) {
            return false;
        }
        return database.getBreadcrumbs().enableBreadcrumbs(context, maxBreadcrumbLogSizeBytes);

    }

    /**
     * Enable logging of breadcrumbs and submission with crash reports
     *
     * @param context                 context of current state of the application
     * @param breadcrumbTypesToEnable a set containing which breadcrumb types to enable
     * @return true if we successfully enabled breadcrumbs
     * @note breadcrumbTypesToEnable only affects automatic breadcrumb receivers. User created
     * breadcrumbs will always be enabled
     */
    public boolean enableBreadcrumbs(Context context,
                                     EnumSet<BacktraceBreadcrumbType> breadcrumbTypesToEnable) {
        if (!isBreadcrumbsAvailable()) {
            return false;
        }
        return database.getBreadcrumbs().enableBreadcrumbs(context, breadcrumbTypesToEnable);
    }

    /**
     * Enable logging of breadcrumbs and submission with crash reports
     *
     * @param context                   context of current state of the application
     * @param breadcrumbTypesToEnable   a set containing which breadcrumb types to enable
     * @param maxBreadcrumbLogSizeBytes breadcrumb log size limit in bytes, should be a power of 2
     * @return true if we successfully enabled breadcrumbs
     * @note breadcrumbTypesToEnable only affects automatic breadcrumb receivers. User created
     * breadcrumbs will always be enabled
     */
    public boolean enableBreadcrumbs(Context context,
                                     EnumSet<BacktraceBreadcrumbType> breadcrumbTypesToEnable,
                                     int maxBreadcrumbLogSizeBytes) {
        if (!isBreadcrumbsAvailable()) {
            return false;
        }
        return database.getBreadcrumbs().enableBreadcrumbs(context, breadcrumbTypesToEnable, maxBreadcrumbLogSizeBytes);
    }

    /**
     * Clear breadcrumb logs
     */
    public boolean clearBreadcrumbs() {
        if (!isBreadcrumbsAvailable()) {
            return false;
        }
        return database.getBreadcrumbs().clearBreadcrumbs();
    }

    /**
     * Add a breadcrumb of type "Manual" and level "Info" with the provided message string
     *
     * @param message a message which describes this breadcrumb (1KB max)
     * @return true if the breadcrumb was successfully added
     */
    public boolean addBreadcrumb(String message) {
        if (!isBreadcrumbsAvailable()) {
            return false;
        }
        return database.getBreadcrumbs().addBreadcrumb(message);
    }

    /**
     * Add a breadcrumb of type "Manual" and the desired level with the provided message string
     *
     * @param message a message which describes this breadcrumb (1KB max)
     * @param level   the severity level of this breadcrumb
     * @return true if the breadcrumb was successfully added
     */
    public boolean addBreadcrumb(String message, BacktraceBreadcrumbLevel level) {
        if (!isBreadcrumbsAvailable()) {
            return false;
        }
        return database.getBreadcrumbs().addBreadcrumb(message, level);
    }

    /**
     * Add a breadcrumb of type "Manual" and level "Info" with the provided message string and attributes
     *
     * @param message    a message which describes this breadcrumb (1KB max)
     * @param attributes key-value pairs to provide additional information about this breadcrumb (1KB max, including some overhead per key-value pair)
     * @return true if the breadcrumb was successfully added
     */
    public boolean addBreadcrumb(String message, Map<String, Object> attributes) {
        if (!isBreadcrumbsAvailable()) {
            return false;
        }
        return database.getBreadcrumbs().addBreadcrumb(message, attributes);
    }

    /**
     * Add a breadcrumb of type "Manual" and the desired level with the provided message string and attributes
     *
     * @param message    a message which describes this breadcrumb (1KB max)
     * @param attributes key-value pairs to provide additional information about this breadcrumb (1KB max, including some overhead per key-value pair)
     * @param level      the severity level of this breadcrumb
     * @return true if the breadcrumb was successfully added
     */
    public boolean addBreadcrumb(String message, Map<String, Object> attributes, BacktraceBreadcrumbLevel level) {
        if (!isBreadcrumbsAvailable()) {
            return false;
        }
        return database.getBreadcrumbs().addBreadcrumb(message, attributes, level);
    }

    /**
     * Add a breadcrumb of the desired type and level "Info" with the provided message string
     *
     * @param message a message which describes this breadcrumb (1KB max)
     * @param type    broadly describes the category of this breadcrumb
     * @return true if the breadcrumb was successfully added
     */
    public boolean addBreadcrumb(String message, BacktraceBreadcrumbType type) {
        if (!isBreadcrumbsAvailable()) {
            return false;
        }
        return database.getBreadcrumbs().addBreadcrumb(message, type);
    }

    /**
     * Add a breadcrumb of the desired level and type with the provided message string
     *
     * @param message a message which describes this breadcrumb (1KB max)
     * @param type    broadly describes the category of this breadcrumb
     * @param level   the severity level of this breadcrumb
     * @return true if the breadcrumb was successfully added
     */
    public boolean addBreadcrumb(String message, BacktraceBreadcrumbType type, BacktraceBreadcrumbLevel level) {
        if (!isBreadcrumbsAvailable()) {
            return false;
        }
        return database.getBreadcrumbs().addBreadcrumb(message, type, level);
    }

    /**
     * Add a breadcrumb of the desired type and level "Info" with the provided message string and attributes
     *
     * @param message    a message which describes this breadcrumb (1KB max)
     * @param attributes key-value pairs to provide additional information about this breadcrumb (1KB max, including some overhead per key-value pair)
     * @param type       broadly describes the category of this breadcrumb
     * @return true if the breadcrumb was successfully added
     */
    public boolean addBreadcrumb(String message, Map<String, Object> attributes, BacktraceBreadcrumbType type) {
        if (isBreadcrumbsAvailable()) {
            return database.getBreadcrumbs().addBreadcrumb(message, attributes, type);
        }
        return false;
    }

    /**
     * Add a breadcrumb of the desired level and type with the provided message string and attributes
     *
     * @param message    a message which describes this breadcrumb (1KB max)
     * @param attributes key-value pairs to provide additional information about this breadcrumb (1KB max, including some overhead per key-value pair)
     * @param type       broadly describes the category of this breadcrumb
     * @param level      the severity level of this breadcrumb
     * @return true if the breadcrumb was successfully added
     */
    public boolean addBreadcrumb(String message, Map<String, Object> attributes, BacktraceBreadcrumbType type, BacktraceBreadcrumbLevel level) {
        if (!isBreadcrumbsAvailable()) {
            return false;
        }
        return database.getBreadcrumbs().addBreadcrumb(message, attributes, type, level);
    }

    public void nativeCrash() {
        crash();
    }

    /**
     * Force a native crash report and minidump submission
     *
     * @param message
     */
    public native void dumpWithoutCrash(String message);

    public native void dumpWithoutCrash(String message, boolean setMainThreadAsFaultingThread);

    /**
     * Sending an exception to Backtrace API
     *
     * @param report current BacktraceReport
     */
    public void send(BacktraceReport report) {
        send(report, null);
    }

    /**
     * Sending an exception to Backtrace API
     *
     * @param report current BacktraceReport
     */
    public void send(BacktraceReport report, final OnServerResponseEventListener callback) {
        Breadcrumbs breadcrumbs = this.database.getBreadcrumbs();
        if (breadcrumbs != null) {
            breadcrumbs.processReportBreadcrumbs(report);
        }
        addReportAttachments(report);

        BacktraceData backtraceData = new BacktraceData(this.context, report, this.attributes);
        backtraceData.symbolication = this.isProguardEnabled ? "proguard" : null;

        final BacktraceDatabaseRecord record = this.database.add(report, this.attributes, this.isProguardEnabled);

        if (this.beforeSendEventListener != null) {
            backtraceData = this.beforeSendEventListener.onEvent(backtraceData);
        }

        this.backtraceApi.send(backtraceData, this.getDatabaseCallback(record, callback));
    }

    private OnServerResponseEventListener getDatabaseCallback(final BacktraceDatabaseRecord record, final OnServerResponseEventListener customCallback) {
        return new OnServerResponseEventListener() {
            @Override
            public void onEvent(BacktraceResult backtraceResult) {
                if (customCallback != null) {
                    customCallback.onEvent(backtraceResult);
                }
                if (record != null) {
                    record.close();
                }
                if (backtraceResult != null && backtraceResult.status == BacktraceResultStatus.Ok) {
                    database.delete(record);
                }
            }
        };
    }

    private void addReportAttachments(BacktraceReport report) {
        if (this.attachments != null) {
            for (String path : this.attachments) {
                report.attachmentPaths.add(path);
            }
        }
    }

    private boolean isBreadcrumbsAvailable() {
        return database != null && database.getBreadcrumbs() != null;
    }

    public boolean usesCustomRequestHandler() {
        return this.backtraceApi.usesCustomRequestHandler();
    }
}
