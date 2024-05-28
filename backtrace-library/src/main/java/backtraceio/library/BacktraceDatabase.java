package backtraceio.library;

import android.content.Context;
import android.content.pm.ApplicationInfo;

import java.io.File;
import java.util.Calendar;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

import backtraceio.library.base.BacktraceBase;
import backtraceio.library.breadcrumbs.BacktraceBreadcrumbs;
import backtraceio.library.common.FileHelper;
import backtraceio.library.common.TypeHelper;
import backtraceio.library.enums.UnwindingMode;
import backtraceio.library.enums.database.RetryBehavior;
import backtraceio.library.events.OnServerResponseEventListener;
import backtraceio.library.interfaces.Api;
import backtraceio.library.interfaces.Breadcrumbs;
import backtraceio.library.interfaces.Database;
import backtraceio.library.interfaces.DatabaseContext;
import backtraceio.library.interfaces.DatabaseFileContext;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.models.BacktraceAttributeConsts;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.database.BacktraceDatabaseRecord;
import backtraceio.library.models.database.BacktraceDatabaseSettings;
import backtraceio.library.models.json.BacktraceAttributes;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.models.types.BacktraceResultStatus;
import backtraceio.library.services.BacktraceCrashHandler;
import backtraceio.library.services.BacktraceDatabaseContext;
import backtraceio.library.services.BacktraceDatabaseFileContext;

/**
 * Backtrace Database
 */
public class BacktraceDatabase implements Database {

    private final String _crashpadHandlerName = "/libcrashpad_handler.so";
    private final String _crashpadDatabasePathPrefix = "/crashpad";
    private static boolean _timerBackgroundWork = false;
    private static Timer _timer;
    private transient final String LOG_TAG = BacktraceDatabase.class.getSimpleName();
    private Api BacktraceApi;
    private Context _applicationContext;
    private DatabaseContext backtraceDatabaseContext;
    private DatabaseFileContext backtraceDatabaseFileContext;
    private BacktraceDatabaseSettings databaseSettings;
    private boolean _enable = false;
    private Breadcrumbs breadcrumbs;

    private BacktraceCrashHandler crashHandler;

    private boolean _enabledNativeIntegration = false;

    /**
     * Add attributes to native reports
     *
     * @param name  attribute name
     * @param value attribute value
     */
    public native void addAttribute(String name, String value);

    /**
     * Initialize Backtrace-native integration
     *
     * @param url                       url to Backtrace
     * @param databasePath              path to Backtrace-native database
     * @param handlerPath               path to error handler
     * @param attributeKeys             array of attribute keys
     * @param attributeValues           array of attribute values
     * @param attachmentPaths           array of paths to file attachments
     * @param enableClientSideUnwinding enable client side unwinding
     * @param unwindingMode             unwinding mode for client side unwinding to use
     * @return true - if backtrace-native was able to initialize correctly, otherwise false.
     */
    private native boolean initialize(String url, String databasePath, String handlerPath,
                                      String[] attributeKeys, String[] attributeValues,
                                      String[] attachmentPaths, boolean enableClientSideUnwinding,
                                      UnwindingMode unwindingMode, String[] environmentVariables);

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

        if (databaseSettings.getDatabasePath() == null || databaseSettings.getDatabasePath()
                .isEmpty()) {
            throw new IllegalArgumentException("Database path is null or empty");
        }

        if (!FileHelper.isFileExists(databaseSettings.getDatabasePath())) {
            boolean createDirs = new File(databaseSettings.getDatabasePath()).mkdirs();
            if (!createDirs || !FileHelper.isFileExists(databaseSettings.getDatabasePath())) {
                throw new IllegalArgumentException("Incorrect database path or application " +
                        "doesn't have permission to write to this path");
            }
        }

        this._applicationContext = context;
        this.databaseSettings = databaseSettings;
        this.backtraceDatabaseContext = new BacktraceDatabaseContext(this._applicationContext,
                databaseSettings);
        this.backtraceDatabaseFileContext = new BacktraceDatabaseFileContext(this.getDatabasePath(),
                this.databaseSettings.getMaxDatabaseSize(), this.databaseSettings
                .getMaxRecordCount());
        this.breadcrumbs = new BacktraceBreadcrumbs(getDatabasePath());
        this.crashHandler = new BacktraceCrashHandler();
    }

    private String getDatabasePath() {
        return databaseSettings.getDatabasePath();
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
    public Boolean setupNativeIntegration(BacktraceBase client, BacktraceCredentials credentials,
                                          boolean enableClientSideUnwinding) {
        return setupNativeIntegration(client, credentials, enableClientSideUnwinding, UnwindingMode.REMOTE_DUMPWITHOUTCRASH);
    }

    /**
     * Setup native crash handler
     *
     * @param client                    Backtrace client
     * @param credentials               Backtrace credentials
     * @param enableClientSideUnwinding Enable client side unwinding
     * @param unwindingMode             Unwinding mode to use for client side unwinding
     */
    public Boolean setupNativeIntegration(BacktraceBase client, BacktraceCredentials credentials,
                                          boolean enableClientSideUnwinding, UnwindingMode unwindingMode) {
        // avoid initialization when database doesn't exist
        if (_enable == false || getSettings() == null) {
            return false;
        }
        String minidumpSubmissionUrl = credentials.getMinidumpSubmissionUrl().toString();
        if (minidumpSubmissionUrl == null) {
            return false;
        }

        // Create the crashpad directory if it doesn't exist
        String databasePath = getSettings().getDatabasePath() + _crashpadDatabasePathPrefix;
        File crashHandlerDir = new File(databasePath);
        crashHandlerDir.mkdir();

        String classPath = this.crashHandler.getClassPath();
        ApplicationInfo applicationInfo = _applicationContext.getApplicationInfo();
        String[] environmentVariables = this.crashHandler.setCrashHandlerEnvironmentVariables(applicationInfo);

        // setup default native attributes
        BacktraceAttributes crashpadAttributes = new BacktraceAttributes(_applicationContext, client.attributes);
        crashpadAttributes.attributes.put(BacktraceAttributeConsts.ErrorType, BacktraceAttributeConsts.CrashAttributeType);
        String[] keys = crashpadAttributes.attributes.keySet().toArray(new String[0]);
        String[] values = crashpadAttributes.attributes.values().toArray(new String[0]);

        // Leave room for breadcrumbs attachment path too
        String[] attachmentPaths = new String[client.attachments.size() + 1];

        // Paths to Crashpad attachments
        if (client.attachments != null) {
            for (int i = 0; i < client.attachments.size(); i++) {
                attachmentPaths[i] = client.attachments.get(i);
            }
        }
        attachmentPaths[attachmentPaths.length - 1] = this.breadcrumbs.getBreadcrumbLogPath();

        _enabledNativeIntegration = initialize(
                minidumpSubmissionUrl,
                databasePath,
                classPath,
                keys,
                values,
                attachmentPaths,
                enableClientSideUnwinding,
                unwindingMode,
                environmentVariables
        );

        if (_enabledNativeIntegration && this.breadcrumbs.isEnabled()) {
            this.breadcrumbs.setOnSuccessfulBreadcrumbAddEventListener(breadcrumbId -> {
                this.addAttribute("breadcrumbs.lastId", Long.toString((breadcrumbId)));
            });
        }
        return _enabledNativeIntegration;
    }

    /**
     * Disable native integration
     */
    @Override
    public void disableNativeIntegration() {
        disable();
        this._enabledNativeIntegration = false;
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

    public void start() {
        if (databaseSettings == null) {
            return;
        }

        if (backtraceDatabaseContext != null && !backtraceDatabaseContext.isEmpty()) {
            this._enable = true;
            return;
        }

        this.loadReports(); // load reports from internal storage

        this.removeOrphaned();

        if (databaseSettings.getRetryBehavior() == RetryBehavior.ByInterval || databaseSettings
                .isAutoSendMode()) {
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
        _timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                String dateTimeNow = Calendar.getInstance().getTime().toString();
                BacktraceLogger.d(LOG_TAG, "Timer - " + dateTimeNow);
                if (backtraceDatabaseContext == null) {
                    BacktraceLogger.w(LOG_TAG, "Timer - database context is null: " +
                            dateTimeNow);
                    return;
                }

                if (backtraceDatabaseContext.isEmpty()) {
                    BacktraceLogger.d(LOG_TAG, "Timer - database is empty (no records): " +
                            dateTimeNow);
                    return;
                }

                if (_timerBackgroundWork) {
                    BacktraceLogger.d(LOG_TAG, "Timer - another timer works now: " + dateTimeNow);
                    return;
                }

                BacktraceLogger.d(LOG_TAG, "Timer - continue working: " + dateTimeNow);
                _timerBackgroundWork = true;
                _timer.cancel();
                _timer.purge();
                _timer = null;

                BacktraceDatabaseRecord record = backtraceDatabaseContext.first();
                while (record != null) {
                    final CountDownLatch threadWaiter = new CountDownLatch(1);
                    BacktraceData backtraceData = record.getBacktraceData(_applicationContext);
                    if (backtraceData == null || backtraceData.report == null) {
                        BacktraceLogger.d(LOG_TAG, "Timer - backtrace data or report is null - " +
                                "deleting record");
                        delete(record);
                    } else {
                        final BacktraceDatabaseRecord currentRecord = record;
                        BacktraceApi.send(backtraceData, new OnServerResponseEventListener() {
                            @Override
                            public void onEvent(BacktraceResult backtraceResult) {
                                if (backtraceResult.status == BacktraceResultStatus.Ok) {
                                    BacktraceLogger.d(LOG_TAG, "Timer - deleting record");
                                    delete(currentRecord);
                                } else {
                                    BacktraceLogger.d(LOG_TAG, "Timer - closing record");
                                    currentRecord.close();
                                    // backtraceDatabaseContext.incrementBatchRetry(); TODO: consider another way to remove some records after few retries
                                }
                                threadWaiter.countDown();
                            }
                        });
                        try {
                            threadWaiter.await();
                        } catch (Exception ex) {
                            BacktraceLogger.e(LOG_TAG,
                                    "Error during waiting for result in Timer", ex
                            );
                        }
                        if (currentRecord.valid() && !currentRecord.locked) {
                            BacktraceLogger.d(LOG_TAG, "Timer - record is valid and unlocked");
                            break;
                        }
                    }
                    record = backtraceDatabaseContext.first();
                }
                BacktraceLogger.d(LOG_TAG, "Setup new timer");
                _timerBackgroundWork = false;
                setupTimer();
            }
        }, databaseSettings.getRetryInterval() * 1000, databaseSettings.getRetryInterval() * 1000);
    }

    public void flush() {
        if (this.BacktraceApi == null) {
            throw new IllegalArgumentException("BacktraceApi is required " +
                    "if you want to use Flush method");
        }

        BacktraceDatabaseRecord record = backtraceDatabaseContext.first();
        while (record != null) {
            BacktraceData backtraceData = record.getBacktraceData(this._applicationContext);
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

    public BacktraceDatabaseRecord add(BacktraceReport backtraceReport, Map<String, Object>
            attributes) {
        return add(backtraceReport, attributes, false);
    }

    public BacktraceDatabaseRecord add(BacktraceReport backtraceReport, Map<String, Object>
            attributes, boolean isProguardEnabled) {
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

        if (record == null){
            return;
        }
        this.backtraceDatabaseContext.delete(record);
    }

    public int count() {
        return backtraceDatabaseContext.count();
    }

    private void loadReports() {
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
        if (backtraceDatabaseContext.count() + 1 > databaseSettings.getMaxRecordCount() &&
                databaseSettings.getMaxRecordCount() != 0) {
            if (!backtraceDatabaseContext.removeOldestRecord()) {
                BacktraceLogger.e(LOG_TAG, "Can't remove last record. Database size is invalid");
                return false;
            }
        }

        if (databaseSettings.getMaxDatabaseSize() != 0 && backtraceDatabaseContext
                .getDatabaseSize() > databaseSettings.getMaxDatabaseSize()) {
            int deletePolicyRetry = 5;
            while (backtraceDatabaseContext.getDatabaseSize() > databaseSettings
                    .getMaxDatabaseSize()) {
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
