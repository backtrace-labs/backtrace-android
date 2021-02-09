package backtraceio.library.base;

import android.content.Context;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import backtraceio.library.BacktraceCredentials;
import backtraceio.library.BacktraceDatabase;
import backtraceio.library.breadcrumbs.BacktraceBreadcrumbs;
import backtraceio.library.events.OnBeforeSendEventListener;
import backtraceio.library.events.OnServerErrorEventListener;
import backtraceio.library.events.OnServerResponseEventListener;
import backtraceio.library.events.RequestHandler;
import backtraceio.library.interfaces.Api;
import backtraceio.library.interfaces.Client;
import backtraceio.library.interfaces.Database;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.database.BacktraceDatabaseRecord;
import backtraceio.library.models.database.BacktraceDatabaseSettings;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.enums.BacktraceBreadcrumbLevel;
import backtraceio.library.enums.BacktraceBreadcrumbType;
import backtraceio.library.models.types.BacktraceResultStatus;
import backtraceio.library.services.BacktraceApi;

/**
 * Base Backtrace Android client
 */
public class BacktraceBase implements Client {

    static {
        System.loadLibrary("backtrace-native");
    }

    private static transient String LOG_TAG = BacktraceBase.class.getSimpleName();


    public native void crash();

    /**
     * Instance of BacktraceApi that allows to send data to Backtrace API
     */
    private Api backtraceApi;

    private void setBacktraceApi(Api backtraceApi) {
        this.backtraceApi = backtraceApi;
        if (this.database != null) {
            this.database.setApi(this.backtraceApi);
        }
    }

    private final BacktraceCredentials credentials;
    /**
     * Application context
     */
    protected Context context;

    /**
     * Backtrace database instance
     */
    public final Database database;

    /**
     * Get custom client attributes. Every argument stored in dictionary will be send to Backtrace API
     */
    public final Map<String, Object> attributes;

    /**
     * Event which will be executed before sending data to Backtrace API
     */
    private OnBeforeSendEventListener beforeSendEventListener = null;

    /**
     * Is Proguard symbolication enabled? We have to inform the Backtrace API
     */
    private boolean isProguardEnabled = false;

    /**
     * Backtrace Breadcrumbs instance
     */
    private BacktraceBreadcrumbs backtraceBreadcrumbs;

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
     * @param attributes  additional information about current application
     */
    public BacktraceBase(Context context, BacktraceCredentials credentials, Map<String, Object> attributes) {
        this(context, credentials, (Database) null, attributes);
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
     * @param attributes       additional information about current application
     */
    public BacktraceBase(Context context, BacktraceCredentials credentials, BacktraceDatabaseSettings databaseSettings, Map<String, Object> attributes) {
        this(context, credentials, new BacktraceDatabase(context, databaseSettings), attributes);
    }

    /**
     * Initialize new client instance with BacktraceCredentials
     *
     * @param context     context of current state of the application
     * @param credentials Backtrace credentials to access Backtrace API
     * @param database    Backtrace database
     */
    public BacktraceBase(Context context, BacktraceCredentials credentials, Database database) {
        this(context, credentials, database, null);
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
        this.context = context;
        this.credentials = credentials;
        this.attributes = attributes != null ? attributes : new HashMap<String, Object>();
        this.database = database != null ? database : new BacktraceDatabase();
        this.setBacktraceApi(new BacktraceApi(credentials));
        this.database.start();
    }

    /**
     * Capture unhandled native exceptions (Backtrace database integration is required to enable this feature).
     */
    public void enableNativeIntegration() {
        this.database.setupNativeIntegration(this, this.credentials);
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
     * Custom request handler for call to server
     *
     * @param requestHandler object with method which will be executed
     */
    public void setOnRequestHandler(RequestHandler requestHandler) {
        this.backtraceApi.setRequestHandler(requestHandler);
    }

    /**
     * Enable logging of breadcrumbs and submission with crash reports
     * @param context   context of current state of the application
     * @return true if we successfully enabled breadcrumbs
     */
    public boolean enableBreadcrumbs(Context context) {
        try {
            backtraceBreadcrumbs = new BacktraceBreadcrumbs(context);
            backtraceBreadcrumbs.enableBreadcrumbs();
        } catch (Exception ex) {
            BacktraceLogger.w(LOG_TAG, "Could not enable breadcrumbs due to " + ex.getMessage());
            return false;
        }

        return true;
    }

    /**
     * Disable logging of breadcrumbs and submission with crash reports
     * @param context   context of current state of the application
     */
    public void disableBreadcrumbs(Context context) {
        if (backtraceBreadcrumbs != null) {
            backtraceBreadcrumbs.disableBreadcrumbs();
        }
    }

    /**
     * Is Breadcrumbs logging enabled?
     * @return      true if Breadcrumbs is enabled, false otherwise
     */
    public boolean isBreadcrumbsEnabled() {
        if (backtraceBreadcrumbs != null) {
            return backtraceBreadcrumbs.isBreadcrumbsEnabled();
        }
        return false;
    }

    /**
     * Clear breadcrumb logs
     */
    public void clearBreadcrumbs() {
        if (backtraceBreadcrumbs != null)
            backtraceBreadcrumbs.clearBreadcrumbs();
    }

    /**
     * Add a breadcrumb of type "Manual" and level "Info" with the provided message string
     * @param message       a brief message which describes this breadcrumb
     * @return              true if the breadcrumb was successfully added
     */
    public boolean addBreadcrumb(String message) {
        if (backtraceBreadcrumbs != null) {
            return backtraceBreadcrumbs.addBreadcrumb(message);
        }
        return false;
    }

    /**
     * Add a breadcrumb of type "Manual" and the desired level with the provided message string
     * @param message       a brief message which describes this breadcrumb
     * @param level         the severity level of this breadcrumb
     * @return              true if the breadcrumb was successfully added
     */
    public boolean addBreadcrumb(String message, BacktraceBreadcrumbLevel level) {
        if (backtraceBreadcrumbs != null) {
            return backtraceBreadcrumbs.addBreadcrumb(message, level);
        }
        return false;
    }

    /**
     * Add a breadcrumb of type "Manual" and level "Info" with the provided message string and attributes
     * @param message       a brief message which describes this breadcrumb
     * @param attributes    key-value pairs to provide additional information about this breadcrumb
     * @return              true if the breadcrumb was successfully added
     */
    public boolean addBreadcrumb(String message, Map<String, Object> attributes) {
        if (backtraceBreadcrumbs != null) {
            return backtraceBreadcrumbs.addBreadcrumb(message, attributes);
        }
        return false;
    }

    /**
     * Add a breadcrumb of type "Manual" and the desired level with the provided message string and attributes
     * @param message       a brief message which describes this breadcrumb
     * @param attributes    key-value pairs to provide additional information about this breadcrumb
     * @param level         the severity level of this breadcrumb
     * @return              true if the breadcrumb was successfully added
     */
    public boolean addBreadcrumb(String message, Map<String, Object> attributes, BacktraceBreadcrumbLevel level) {
        if (backtraceBreadcrumbs != null) {
            return backtraceBreadcrumbs.addBreadcrumb(message, attributes, level);
        }
        return false;
    }

    /**
     * Add a breadcrumb of the desired type and level "Info" with the provided message string
     * @param message       a brief message which describes this breadcrumb
     * @param type          broadly describes the category of this breadcrumb
     * @return              true if the breadcrumb was successfully added
     */
    public boolean addBreadcrumb(String message, BacktraceBreadcrumbType type) {
        if (backtraceBreadcrumbs != null) {
            return backtraceBreadcrumbs.addBreadcrumb(message, type);
        }
        return false;
    }

    /**
     * Add a breadcrumb of the desired level and type with the provided message string
     * @param message       a brief message which describes this breadcrumb
     * @param type          broadly describes the category of this breadcrumb
     * @param level         the severity level of this breadcrumb
     * @return              true if the breadcrumb was successfully added
     */
    public boolean addBreadcrumb(String message, BacktraceBreadcrumbType type, BacktraceBreadcrumbLevel level) {
        if (backtraceBreadcrumbs != null) {
            return backtraceBreadcrumbs.addBreadcrumb(message, type, level);
        }
        return false;
    }

    /**
     * Add a breadcrumb of the desired type and level "Info" with the provided message string and attributes
     * @param message       a brief message which describes this breadcrumb
     * @param attributes    key-value pairs to provide additional information about this breadcrumb
     * @param type          broadly describes the category of this breadcrumb
     * @return              true if the breadcrumb was successfully added
     */
    public boolean addBreadcrumb(String message, Map<String, Object> attributes, BacktraceBreadcrumbType type) {
        if (backtraceBreadcrumbs != null) {
            return backtraceBreadcrumbs.addBreadcrumb(message, attributes, type);
        }
        return false;
    }

    /**
     * Add a breadcrumb of the desired level and type with the provided message string and attributes
     * @param message       a brief message which describes this breadcrumb
     * @param attributes    key-value pairs to provide additional information about this breadcrumb
     * @param type          broadly describes the category of this breadcrumb
     * @param level         the severity level of this breadcrumb
     * @return              true if the breadcrumb was successfully added
     */
    public boolean addBreadcrumb(String message, Map<String, Object> attributes, BacktraceBreadcrumbType type, BacktraceBreadcrumbLevel level) {
        if (backtraceBreadcrumbs != null) {
            return backtraceBreadcrumbs.addBreadcrumb(message, attributes, type, level);
        }
        return false;
    }

    public void nativeCrash() {
        crash();
    }

    /**
     * Force a Crashpad report and minidump submission
     * @param message
     */
    public native void dumpWithoutCrash(String message);

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
        if (backtraceBreadcrumbs != null && backtraceBreadcrumbs.isBreadcrumbsEnabled()) {
            File breadcrumbLogFilesDir = new File(backtraceBreadcrumbs.getBreadcrumbLogDirectory());

            File[] breadcrumbLogFiles = breadcrumbLogFilesDir.listFiles();
            for (File breadcrumbLogFile : breadcrumbLogFiles) {
                report.attachmentPaths.add(breadcrumbLogFile.getAbsolutePath());
            }

            long lastBreadcrumbId = backtraceBreadcrumbs.getCurrentBreadcrumbId();
            report.attributes.put("breadcrumbs.lastId", lastBreadcrumbId);
        }

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
}
