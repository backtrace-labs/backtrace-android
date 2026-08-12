package backtraceio.library;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import backtraceio.library.base.BacktraceBase;
import backtraceio.library.breadcrumbs.BacktraceBreadcrumbs;
import backtraceio.library.common.FileHelper;
import backtraceio.library.common.TypeHelper;
import backtraceio.library.common.serialization.DebugHelper;
import backtraceio.library.enums.UnwindingMode;
import backtraceio.library.enums.database.RetryBehavior;
import backtraceio.library.events.OnServerResponseEventListener;
import backtraceio.library.interfaces.Api;
import backtraceio.library.interfaces.Breadcrumbs;
import backtraceio.library.interfaces.Database;
import backtraceio.library.interfaces.DatabaseContext;
import backtraceio.library.interfaces.DatabaseFileContext;
import backtraceio.library.interfaces.NativeCommunication;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.models.BacktraceAttributeConsts;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.database.BacktraceDatabaseRecord;
import backtraceio.library.models.database.BacktraceDatabaseSettings;
import backtraceio.library.models.json.BacktraceAttributes;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.models.nativeHandler.CrashHandlerConfiguration;
import backtraceio.library.models.types.BacktraceResultStatus;
import backtraceio.library.nativeCalls.BacktraceCrashHandlerWrapper;
import backtraceio.library.services.BacktraceDatabaseContext;
import backtraceio.library.services.BacktraceDatabaseFileContext;
import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

/**
 * Backtrace Database
 */
public class BacktraceDatabase implements Database {

    private static Timer _timer;
    private final transient String LOG_TAG = BacktraceDatabase.class.getSimpleName();
    private Api BacktraceApi;
    private Context _applicationContext;
    private DatabaseContext backtraceDatabaseContext;
    private DatabaseFileContext backtraceDatabaseFileContext;
    private BacktraceDatabaseSettings databaseSettings;
    private boolean _enable = false;
    private Breadcrumbs breadcrumbs;
    private CrashHandlerConfiguration crashHandlerConfiguration;
    private boolean _enabledNativeIntegration = false;
    private NativeCommunication nativeCommunication = new BacktraceCrashHandlerWrapper();

    /**
     * Add attributes to native reports
     *
     * @param name  attribute name
     * @param value attribute value
     */
    public native void addAttribute(String name, String value);

    /**
     * Add a file attachment to native reports.
     *
     * @param attachmentPath the file path to attach to native reports
     */
    public native void addAttachment(String attachmentPath);

    /**
     * Disable Backtrace-native integration
     */
    private native void disable();

    /**
     * Create disabled instance of BacktraceDatabase
     */
    public BacktraceDatabase() {
        BacktraceLogger.w(LOG_TAG, "Disabled instance of BacktraceDatabase created, native crashes won't be captured");
    }

    /**
     * Create new Backtrace database instance
     *
     * @param path Path to database directory
     */
    public BacktraceDatabase(Context context, String path) {
        this(context, new BacktraceDatabaseSettings(path));
    }

    /**
     * Create Backtrace database instance
     *
     * @param databaseSettings Backtrace database settings
     */
    public BacktraceDatabase(Context context, BacktraceDatabaseSettings databaseSettings) {
        if (databaseSettings == null || context == null) {
            throw new IllegalArgumentException("Database settings or application context is null");
        }

        if (databaseSettings.getDatabasePath() == null
                || databaseSettings.getDatabasePath().isEmpty()) {
            throw new IllegalArgumentException("Database path is null or empty");
        }

        if (!FileHelper.isFileExists(databaseSettings.getDatabasePath())) {
            boolean createDirs = new File(databaseSettings.getDatabasePath()).mkdirs();
            if (!createDirs || !FileHelper.isFileExists(databaseSettings.getDatabasePath())) {
                throw new IllegalArgumentException(
                        "Incorrect database path or application " + "doesn't have permission to write to this path");
            }
        }

        this._applicationContext = context;
        this.databaseSettings = databaseSettings;
        this.backtraceDatabaseContext = new BacktraceDatabaseContext(databaseSettings);
        this.backtraceDatabaseFileContext = new BacktraceDatabaseFileContext(
                this.getDatabasePath(),
                this.databaseSettings.getMaxDatabaseSize(),
                this.databaseSettings.getMaxRecordCount());
        this.breadcrumbs = new BacktraceBreadcrumbs(getDatabasePath());
        this.crashHandlerConfiguration = new CrashHandlerConfiguration();
    }

    private String getDatabasePath() {
        return databaseSettings.getDatabasePath();
    }

    /**
     * Sanitized failure description for optional-native-integration diagnostics: exception
     * messages can carry submission URLs, tokens, or filesystem paths (for example from a custom
     * credential implementation or an UnsatisfiedLinkError), so these paths log only the class.
     */
    private static String failureType(Throwable failure) {
        if (failure == null) {
            return "unknown";
        }
        String type = failure.getClass().getName();
        return type == null || type.trim().isEmpty() ? "unknown" : type;
    }

    /**
     * Setup native crash handler
     *
     * @param client      Backtrace client
     * @param credentials Backtrace credentials
     */
    public Boolean setupNativeIntegration(BacktraceBase client, BacktraceCredentials credentials) {
        return setupNativeIntegration(client, credentials, false);
    }

    /**
     * Setup native crash handler
     *
     * @param client                    Backtrace client
     * @param credentials               Backtrace credentials
     * @param enableClientSideUnwinding Enable client side unwinding
     */
    public Boolean setupNativeIntegration(
            BacktraceBase client, BacktraceCredentials credentials, boolean enableClientSideUnwinding) {
        return setupNativeIntegration(
                client, credentials, enableClientSideUnwinding, UnwindingMode.REMOTE_DUMPWITHOUTCRASH);
    }

    /**
     * Overrides default native communication bridge
     */
    public void useNativeCommunication(NativeCommunication nativeCommunication) {
        if (nativeCommunication == null) {
            throw new IllegalArgumentException("NativeCommunication cannot be null");
        }
        this.nativeCommunication = nativeCommunication;
    }

    /**
     * Test seam: overrides the crash-handler configuration used by
     * {@link #setupNativeIntegration(BacktraceBase, BacktraceCredentials, boolean, UnwindingMode)}.
     */
    void useCrashHandlerConfiguration(CrashHandlerConfiguration crashHandlerConfiguration) {
        if (crashHandlerConfiguration == null) {
            throw new IllegalArgumentException("CrashHandlerConfiguration cannot be null");
        }
        this.crashHandlerConfiguration = crashHandlerConfiguration;
    }

    /**
     * Setup native crash handler
     *
     * @param client                    Backtrace client
     * @param credentials               Backtrace credentials
     * @param enableClientSideUnwinding Enable client side unwinding
     * @param unwindingMode             Unwinding mode to use for client side unwinding
     */
    public Boolean setupNativeIntegration(
            BacktraceBase client,
            BacktraceCredentials credentials,
            boolean enableClientSideUnwinding,
            UnwindingMode unwindingMode) {
        // avoid initialization when database doesn't exist
        if (_enable == false || getSettings() == null) {
            return false;
        }

        // Validate public inputs before performing any side effects; enabling the optional native
        // integration must never terminate the host application.
        if (client == null) {
            BacktraceLogger.e(LOG_TAG, "Native integration requires a Backtrace client.");
            this._enabledNativeIntegration = false;
            return false;
        }
        if (credentials == null) {
            BacktraceLogger.e(LOG_TAG, "Native integration requires Backtrace credentials.");
            this._enabledNativeIntegration = false;
            return false;
        }
        if (nativeCommunication == null || crashHandlerConfiguration == null) {
            BacktraceLogger.e(LOG_TAG, "Native integration dependencies are unavailable.");
            this._enabledNativeIntegration = false;
            return false;
        }

        final long startSetupNativeIntegrationTime = DebugHelper.getCurrentTimeMillis();

        // Preparation failures (credential resolution, ABI policy, crashpad directory, attributes,
        // attachments, path resolution) disable native crash capture only; managed crash reporting
        // stays operational. The minidump URL getter runs exactly once, inside containment, because
        // credential implementations can carry null values or throw while building the URL.
        final String minidumpSubmissionUrl;
        final String crashpadDatabaseDirectory;
        final String classPath;
        final String[] keys;
        final String[] values;
        final String[] attachmentPaths;
        final String[] environmentVariables;
        try {
            Uri minidumpUri = credentials.getMinidumpSubmissionUrl();
            if (minidumpUri == null || minidumpUri.toString().trim().isEmpty()) {
                BacktraceLogger.e(LOG_TAG, "Native integration requires a minidump submission URL.");
                this._enabledNativeIntegration = false;
                return false;
            }
            minidumpSubmissionUrl = minidumpUri.toString();

            if (!this.crashHandlerConfiguration.isSupportedAbi()) {
                this._enabledNativeIntegration = false;
                return false;
            }

            crashpadDatabaseDirectory = this.crashHandlerConfiguration.useCrashpadDirectory(
                    getSettings().getDatabasePath());

            // setup default native attributes
            BacktraceAttributes crashpadAttributes = new BacktraceAttributes(_applicationContext, client.attributes);
            crashpadAttributes.attributes.put(
                    BacktraceAttributeConsts.ErrorType, BacktraceAttributeConsts.CrashAttributeType);
            keys = crashpadAttributes.attributes.keySet().toArray(new String[0]);
            values = crashpadAttributes.attributes.values().toArray(new String[0]);

            // Leave room for breadcrumbs attachment path too
            List<String> attachmentList = new ArrayList<>(client.getAttachments());
            attachmentList.add(this.breadcrumbs.getBreadcrumbLogPath());
            attachmentPaths = attachmentList.toArray(new String[0]);

            ApplicationInfo applicationInfo = _applicationContext.getApplicationInfo();
            environmentVariables = this.crashHandlerConfiguration
                    .getCrashHandlerEnvironmentVariables(applicationInfo)
                    .toArray(new String[0]);
            classPath = this.crashHandlerConfiguration.getClassPath();
        } catch (RuntimeException | LinkageError failure) {
            this._enabledNativeIntegration = false;
            BacktraceLogger.e(
                    LOG_TAG,
                    "BT_NATIVE_PREPARE_FAILURE: Native integration configuration could not be"
                            + " prepared. Failure type: "
                            + failureType(failure));
            return false;
        }

        try {
            _enabledNativeIntegration = nativeCommunication.initializeJavaCrashHandler(
                    minidumpSubmissionUrl,
                    crashpadDatabaseDirectory,
                    classPath,
                    keys,
                    values,
                    attachmentPaths,
                    environmentVariables);
        } catch (RuntimeException | LinkageError failure) {
            this._enabledNativeIntegration = false;
            BacktraceLogger.e(
                    LOG_TAG,
                    "BT_NATIVE_BRIDGE_FAILURE: Native integration was not enabled because the"
                            + " native initialization bridge failed. Failure type: "
                            + failureType(failure));
            return false;
        }

        if (!_enabledNativeIntegration) {
            BacktraceLogger.e(LOG_TAG, "Native integration was not enabled by the native initialization bridge.");
        }

        if (_enabledNativeIntegration && this.breadcrumbs.isEnabled()) {
            // Nonfatal: a failure installing the breadcrumb hook must not misreport an already
            // successfully initialized native handler as disabled.
            try {
                this.breadcrumbs.setOnSuccessfulBreadcrumbAddEventListener(breadcrumbId -> {
                    this.addAttribute("breadcrumbs.lastId", Long.toString((breadcrumbId)));
                });
            } catch (RuntimeException failure) {
                BacktraceLogger.e(
                        LOG_TAG,
                        "BT_NATIVE_BREADCRUMB_HOOK_FAILURE: Native integration is enabled, but the"
                                + " breadcrumb synchronization hook could not be installed."
                                + " Failure type: "
                                + failureType(failure));
            }
        }

        final long endSetupNativeIntegrationTime = DebugHelper.getCurrentTimeMillis();
        BacktraceLogger.d(
                LOG_TAG,
                "Setup native integration took " + (endSetupNativeIntegrationTime - startSetupNativeIntegrationTime)
                        + " milliseconds");

        return _enabledNativeIntegration;
    }

    private Runnable nativeDisableAction = this::disable;

    /**
     * Test seam: overrides the native disable action invoked by {@link #disableNativeIntegration()}.
     */
    void useNativeDisableAction(Runnable nativeDisableAction) {
        if (nativeDisableAction == null) {
            throw new IllegalArgumentException("Native disable action cannot be null");
        }
        this.nativeDisableAction = nativeDisableAction;
    }

    /**
     * Disable native integration. Fail-safe: a native bridge failure is logged and the Java-side
     * state is cleared regardless, so a later enable starts from a consistent state.
     */
    @Override
    public void disableNativeIntegration() {
        try {
            nativeDisableAction.run();
        } catch (RuntimeException | LinkageError failure) {
            BacktraceLogger.e(
                    LOG_TAG,
                    "BT_NATIVE_DISABLE_FAILURE: Native integration could not be disabled through"
                            + " the native bridge. Failure type: "
                            + failureType(failure));
        } finally {
            this._enabledNativeIntegration = false;
        }
    }

    @Override
    public Breadcrumbs getBreadcrumbs() {
        return this.breadcrumbs;
    }

    public Boolean addNativeAttribute(String key, Object value) {
        if (!_enabledNativeIntegration) {
            return false;
        }

        if (key == null || value == null) {
            return false;
        }
        Class type = value.getClass();
        if (!TypeHelper.isPrimitiveOrPrimitiveWrapperOrString(type)) {
            return false;
        }
        addAttribute(key, value.toString());
        return true;
    }

    public Boolean addNativeAttachment(String attachmentPath) {
        if (!_enabledNativeIntegration || attachmentPath == null) {
            return false;
        }
        addAttachment(attachmentPath);
        return true;
    }

    public void start() {
        if (databaseSettings == null) {
            return;
        }

        if (backtraceDatabaseContext != null && !backtraceDatabaseContext.isEmpty()) {
            this._enable = true;
            return;
        }

        this.loadReports();

        this.removeOrphaned();

        if (databaseSettings.getRetryBehavior() == RetryBehavior.ByInterval || databaseSettings.isAutoSendMode()) {
            setupTimer();
        }

        this._enable = true;
    }

    /**
     * Get settings
     *
     * @return current database settings
     */
    public BacktraceDatabaseSettings getSettings() {
        return databaseSettings;
    }

    private void setupTimer() {
        _timer = new Timer();
        _timer.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        String dateTimeNow = Calendar.getInstance().getTime().toString();
                        BacktraceLogger.d(LOG_TAG, "Backtrace DB Timer - " + dateTimeNow);
                        if (backtraceDatabaseContext == null) {
                            BacktraceLogger.w(LOG_TAG, "Backtrace DB Timer - database context is null: " + dateTimeNow);
                            return;
                        }

                        if (backtraceDatabaseContext.isEmpty()) {
                            BacktraceLogger.d(
                                    LOG_TAG, "Backtrace DB Timer - database is empty (no records): " + dateTimeNow);
                            return;
                        }

                        try {
                            BacktraceDatabaseRecord record = backtraceDatabaseContext.first();
                            while (record != null) {
                                final CountDownLatch threadWaiter = new CountDownLatch(1);
                                BacktraceData backtraceData = record.getBacktraceData();
                                if (backtraceData == null || backtraceData.getReport() == null) {
                                    BacktraceLogger.d(
                                            LOG_TAG,
                                            "Backtrace DB Timer - backtrace data or report is null - "
                                                    + "deleting record");
                                    delete(record);
                                } else {
                                    final BacktraceDatabaseRecord currentRecord = record;
                                    BacktraceApi.send(backtraceData, new OnServerResponseEventListener() {
                                        @Override
                                        public void onEvent(BacktraceResult backtraceResult) {
                                            if (backtraceResult.status == BacktraceResultStatus.Ok) {
                                                BacktraceLogger.d(LOG_TAG, "Backtrace DB Timer - deleting record");
                                                delete(currentRecord);
                                            } else {
                                                BacktraceLogger.d(LOG_TAG, "Backtrace DB Timer - closing record");
                                                currentRecord.close();
                                                backtraceDatabaseContext
                                                        .incrementBatchRetry(); // If we are not able to send single
                                                // record we are moving all reports to
                                                // next batch
                                            }
                                            threadWaiter.countDown();
                                        }
                                    });
                                    try {
                                        threadWaiter.await();
                                    } catch (Exception ex) {
                                        BacktraceLogger.e(
                                                LOG_TAG, "Error during waiting for result in Backtrace DB Timer", ex);
                                    }
                                    if (currentRecord.valid() && !currentRecord.locked) {
                                        BacktraceLogger.d(LOG_TAG, "Backtrace DB Timer - record is valid and unlocked");
                                        break;
                                    }
                                }
                                record = backtraceDatabaseContext.first();
                            }
                        } catch (Exception e) {
                            BacktraceLogger.e(LOG_TAG, "Exception in Backtrace DB timer", e);
                        }
                    }
                },
                databaseSettings.getRetryInterval() * 1000L,
                databaseSettings.getRetryInterval() * 1000L);
    }

    public void flush() {
        if (this.BacktraceApi == null) {
            throw new IllegalArgumentException("BacktraceApi is required " + "if you want to use Flush method");
        }

        BacktraceDatabaseRecord record = backtraceDatabaseContext.first();
        while (record != null) {
            BacktraceData backtraceData = record.getBacktraceData();
            this.delete(record);
            if (backtraceData != null) {
                BacktraceApi.send(backtraceData, null);
            }
            record = backtraceDatabaseContext.first();
        }
    }

    public void setApi(Api backtraceApi) {
        this.BacktraceApi = backtraceApi;
    }

    public void clear() {
        if (backtraceDatabaseContext != null) {
            backtraceDatabaseContext.clear();
        }
        if (backtraceDatabaseFileContext != null) {
            backtraceDatabaseFileContext.clear();
        }
    }

    private void removeOrphaned() {
        Iterable<BacktraceDatabaseRecord> records = backtraceDatabaseContext.get();
        backtraceDatabaseFileContext.removeOrphaned(records);
    }

    public boolean validConsistency() {
        return backtraceDatabaseFileContext.validFileConsistency();
    }

    public BacktraceDatabaseRecord add(BacktraceReport backtraceReport, Map<String, Object> attributes) {
        return add(backtraceReport, attributes, false);
    }

    public BacktraceDatabaseRecord add(
            BacktraceReport backtraceReport, Map<String, Object> attributes, boolean isProguardEnabled) {
        if (!this._enable || backtraceReport == null) {
            return null;
        }

        boolean validationResult = this.validateDatabaseSize();
        if (!validationResult) {
            return null;
        }

        BacktraceData data = backtraceReport.toBacktraceData(this._applicationContext, attributes, isProguardEnabled);
        return backtraceDatabaseContext.add(data);
    }

    public Iterable<BacktraceDatabaseRecord> get() {
        if (backtraceDatabaseContext == null) {
            return null;
        }

        return backtraceDatabaseContext.get();
    }

    public void delete(BacktraceDatabaseRecord record) {
        if (this.backtraceDatabaseContext == null) {
            return;
        }

        if (record == null) {
            return;
        }
        this.backtraceDatabaseContext.delete(record);
    }

    public int count() {
        return backtraceDatabaseContext.count();
    }

    private void loadReports() {
        final long startLoadingReportsTime = System.currentTimeMillis();

        this.loadReportsToDbContext();

        final long endLoadingReportsTime = System.currentTimeMillis();

        BacktraceLogger.d(
                LOG_TAG,
                "Loading " + backtraceDatabaseContext.count() + " reports took "
                        + (endLoadingReportsTime - startLoadingReportsTime) + " milliseconds");
    }

    private void loadReportsToDbContext() {
        Iterable<File> files = backtraceDatabaseFileContext.getRecords();

        for (File file : files) {
            BacktraceDatabaseRecord record = BacktraceDatabaseRecord.readFromFile(file);
            if (record == null) {
                continue;
            }

            if (!record.valid()) {
                record.delete();
                continue;
            }
            backtraceDatabaseContext.add(record);
            validateDatabaseSize();
            record.close();
        }
    }

    /**
     * Validate database size - check how many records are stored
     * in database and how much records need space.
     * If space or number of records are invalid
     * database will remove old reports
     *
     * @return is database size valid
     */
    private boolean validateDatabaseSize() {
        // Check how many records are stored in database
        // Remove in case when we want to store one more than expected number
        // If record count == 0 then we ignore this condition
        if (backtraceDatabaseContext.count() + 1 > databaseSettings.getMaxRecordCount()
                && databaseSettings.getMaxRecordCount() != 0) {
            if (!backtraceDatabaseContext.removeOldestRecord()) {
                BacktraceLogger.e(LOG_TAG, "Can't remove last record. Database size is invalid");
                return false;
            }
        }

        if (databaseSettings.getMaxDatabaseSize() != 0
                && backtraceDatabaseContext.getDatabaseSize() > databaseSettings.getMaxDatabaseSize()) {
            int deletePolicyRetry = 5;
            while (backtraceDatabaseContext.getDatabaseSize() > databaseSettings.getMaxDatabaseSize()) {
                backtraceDatabaseContext.removeOldestRecord();
                deletePolicyRetry--; // avoid infinity loop
                if (deletePolicyRetry == 0) {
                    break;
                }
            }
            return deletePolicyRetry != 0;
        }
        return true;
    }

    public long getDatabaseSize() {
        return backtraceDatabaseContext.getDatabaseSize();
    }
}
