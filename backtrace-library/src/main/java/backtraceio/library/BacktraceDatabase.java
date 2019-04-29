package backtraceio.library;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.sql.Date;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import backtraceio.library.common.FileHelper;
import backtraceio.library.enums.database.RetryBehavior;
import backtraceio.library.interfaces.IBacktraceApi;
import backtraceio.library.interfaces.IBacktraceDatabase;
import backtraceio.library.interfaces.IBacktraceDatabaseContext;
import backtraceio.library.interfaces.IBacktraceDatabaseFileContext;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.database.BacktraceDatabaseRecord;
import backtraceio.library.models.database.BacktraceDatabaseSettings;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.models.types.BacktraceResultStatus;
import backtraceio.library.services.BacktraceApi;
import backtraceio.library.services.BacktraceDatabaseContext;
import backtraceio.library.services.BacktraceDatabaseFileContext;

/**
 * Backtrace Database
 */
public class BacktraceDatabase implements IBacktraceDatabase {

    private transient final String LOG_TAG = BacktraceDatabase.class.getSimpleName();

    private IBacktraceApi BacktraceApi;

    private Context _applicationContext;

    private IBacktraceDatabaseContext backtraceDatabaseContext;

    private IBacktraceDatabaseFileContext backtraceDatabaseFileContext;

    private BacktraceDatabaseSettings databaseSettings;

    private String getDatabasePath() {
        return databaseSettings.getDatabasePath();
    }

    private static boolean _timerBackgroundWork = false;

    private boolean _enable = false;

    private static Timer _timer;

    /**
     * Create disabled instance of BacktraceDatabase
     */
    public BacktraceDatabase() {
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

        if (databaseSettings.getDatabasePath() == null || databaseSettings.getDatabasePath().isEmpty()) {
            throw new IllegalArgumentException("Database path is null or empty");
        }

        if(!FileHelper.isFileExists(databaseSettings.getDatabasePath())){
            boolean createDirs = new File(databaseSettings.getDatabasePath()).mkdirs();
            if(!createDirs || !FileHelper.isFileExists(databaseSettings.getDatabasePath())) {
                throw new IllegalArgumentException("Incorrect database path or application doesn't have permission to write to this path");
            }
        }

        this._applicationContext = context;
        this.databaseSettings = databaseSettings;
        this.backtraceDatabaseContext = new BacktraceDatabaseContext(this._applicationContext, databaseSettings);
        this.backtraceDatabaseFileContext = new BacktraceDatabaseFileContext(this.getDatabasePath(),
                this.databaseSettings.getMaxDatabaseSize(), this.databaseSettings.getMaxRecordCount());
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
                BacktraceLogger.d(LOG_TAG, "Timer:" + new Date(System.currentTimeMillis()).toString());
                if (backtraceDatabaseContext == null || backtraceDatabaseContext.isEmpty() || _timerBackgroundWork) {
                    BacktraceLogger.d(LOG_TAG, "Null or another timer works: " +  new Date(System.currentTimeMillis()).toString());
                    return;
                }
                BacktraceLogger.d(LOG_TAG, "Timer continue working: " +  new Date(System.currentTimeMillis()).toString());
                _timerBackgroundWork = true;
                _timer.cancel();
                _timer.purge();
                _timer = null;

                BacktraceDatabaseRecord record = backtraceDatabaseContext.first();
                while (record != null) {
                    BacktraceData backtraceData = record.getBacktraceData();
                    if (backtraceData == null || backtraceData.report == null) {
                        delete(record);
                    } else {
                        BacktraceApi.send(backtraceData, null);
                        // TODO!!!!
//                        if (result.status == BacktraceResultStatus.Ok) {
//                            delete(record);
//                        } else {
//                            record.close();
//                            backtraceDatabaseContext.incrementBatchRetry();
//                            break;
//                        }
                    }
                    record = backtraceDatabaseContext.first();
                }
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
            BacktraceData backtraceData = record.getBacktraceData();
            this.delete(record);
            if (backtraceData != null) {
                BacktraceApi.send(backtraceData, null);
            }
            record = backtraceDatabaseContext.first();
        }
    }

    public void setApi(IBacktraceApi backtraceApi) {
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
        if (!this._enable || backtraceReport == null) {
            return null;
        }

        boolean validationResult = this.validateDatabaseSize();
        if (!validationResult) {
            return null;
        }

        BacktraceData data = backtraceReport.toBacktraceData(this._applicationContext, attributes);
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
        this.backtraceDatabaseContext.delete(record);
    }

    public int count() {
        return backtraceDatabaseContext.count();
    }

    private void loadReports() {
        Iterable<File> files = backtraceDatabaseFileContext.getRecords();

        for (File file : files) {
            BacktraceDatabaseRecord record = BacktraceDatabaseRecord.readFromFile(file);
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
