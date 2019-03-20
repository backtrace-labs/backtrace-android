package backtraceio.library;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Date;

import backtraceio.library.common.FileHelper;
import backtraceio.library.enums.database.RetryBehavior;
import backtraceio.library.interfaces.IBacktraceApi;
import backtraceio.library.interfaces.IBacktraceDatabase;
import backtraceio.library.interfaces.IBacktraceDatabaseContext;
import backtraceio.library.interfaces.IBacktraceDatabaseFileContext;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.database.BacktraceDatabaseRecord;
import backtraceio.library.models.database.BacktraceDatabaseSettings;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.models.types.BacktraceResultStatus;
import backtraceio.library.services.BacktraceDatabaseContext;
import backtraceio.library.services.BacktraceDatabaseFileContext;

/**
 * Backtrace Database
 */
public class BacktraceDatabase implements IBacktraceDatabase {

    private IBacktraceApi BacktraceApi;

    private Context _applicationContext;

    private IBacktraceDatabaseContext BacktraceDatabaseContext;

    private IBacktraceDatabaseFileContext BacktraceDatabaseFileContext;

    private BacktraceDatabaseSettings DatabaseSettings;

    private String getDatabasePath(){
        return DatabaseSettings.databasePath;
    };

    private boolean _timerBackgroundWork = false;


    private boolean _enable = false;


    private Timer _timer;

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
        if (databaseSettings == null || databaseSettings.databasePath.isEmpty() || context == null) {
            return;
        }

        if (!FileHelper.isFileExists(databaseSettings.databasePath)) {
            throw new IllegalArgumentException("Database path does not exists");
        }

        this._applicationContext = context;
        this.DatabaseSettings = databaseSettings;
        this.BacktraceDatabaseContext = new BacktraceDatabaseContext(this._applicationContext, databaseSettings);
        this.BacktraceDatabaseFileContext = new BacktraceDatabaseFileContext(this.getDatabasePath(),
                this.DatabaseSettings.MaxDatabaseSize, this.DatabaseSettings.maxRecordCount);
    }

    public void start() {
        if (DatabaseSettings == null) {
            return;
        }

        if (BacktraceDatabaseContext != null && !BacktraceDatabaseContext.isEmpty()) {
            this._enable = true;
            return;
        }

        this.loadReports(); // load reports from internal storage

        this.removeOrphaned();

        if (DatabaseSettings.retryBehavior == RetryBehavior.ByInterval || DatabaseSettings
                .autoSendMode) {
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
        return DatabaseSettings;
    }

    private void setupTimer() {
        this._timer = new Timer();
        this._timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Log.d("TIMER", new Date(System.currentTimeMillis()).toString());
                if(BacktraceDatabaseContext == null || BacktraceDatabaseContext.isEmpty() || _timerBackgroundWork){
                    return;
                }
                _timerBackgroundWork = true;
                _timer.cancel();
                _timer.purge();

                BacktraceDatabaseRecord record = BacktraceDatabaseContext.first();
                while(record != null)
                {
                    BacktraceData backtraceData = record.getBacktraceData();
                    if(backtraceData == null || backtraceData.report ==null){
                        delete(record);
                    }
                    else
                    {
                        BacktraceResult result = BacktraceApi.send(backtraceData);
                        if(result.status == BacktraceResultStatus.Ok)
                        {
                            delete(record);
                        }
                        else
                        {
                            BacktraceDatabaseContext.incrementBatchRetry();
                            break;
                        }
                    }
                    record = BacktraceDatabaseContext.first();
                }
                _timerBackgroundWork = false;
                setupTimer();
            }
        },  new Date(), DatabaseSettings.retryInterval * 1000);
    }

    public void flush() {
        if (this.BacktraceApi == null) {
            throw new IllegalArgumentException("BacktraceApi is required if you want to use Flush" +
                    " method");
        }

        BacktraceDatabaseRecord record = BacktraceDatabaseContext.first();
        while (record != null) {
            BacktraceData backtraceData = record.getBacktraceData();
            this.delete(record);
            record = BacktraceDatabaseContext.first();
            if (backtraceData != null) {
                BacktraceApi.send(backtraceData);
            }
        }
    }


    public void setApi(IBacktraceApi backtraceApi) {
        this.BacktraceApi = backtraceApi;
    }

    public void clear() {
        if (BacktraceDatabaseContext != null) {
            BacktraceDatabaseContext.clear();
        }
        if (BacktraceDatabaseFileContext != null) {
            BacktraceDatabaseFileContext.clear();
        }
    }

    private void removeOrphaned() {
        Iterable<BacktraceDatabaseRecord> records = BacktraceDatabaseContext.get();
        BacktraceDatabaseFileContext.removeOrphaned(records);
    }

    public boolean validConsistency() {
        return false;
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
        return BacktraceDatabaseContext.add(data);
    }

    public Iterable<BacktraceDatabaseRecord> get() {
        if (BacktraceDatabaseContext == null) {
            return null;
        }

        return BacktraceDatabaseContext.get();
    }

    public void delete(BacktraceDatabaseRecord record) {
        if (this.BacktraceDatabaseContext == null) {
            return;
        }
        this.BacktraceDatabaseContext.delete(record);
    }

    int count() {
        return BacktraceDatabaseContext.count();
    }

    private void loadReports() {
        Iterable<File> files = BacktraceDatabaseFileContext.getRecords();

        for (File file : files) {
            BacktraceDatabaseRecord record = BacktraceDatabaseRecord.readFromFile(file);
            if (!record.valid()) {
                record.delete();
                continue;
            }
            BacktraceDatabaseContext.add(record);
            validateDatabaseSize();

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
        if (BacktraceDatabaseContext.count() + 1 > DatabaseSettings.maxRecordCount &&
                DatabaseSettings.maxRecordCount != 0) {
            if (!BacktraceDatabaseContext.removeLastRecord()) {
                return false;
            }
        }

        if (DatabaseSettings.getMaxDatabaseSize() != 0 && BacktraceDatabaseContext
                .getDatabaseSize() > DatabaseSettings.MaxDatabaseSize) {
            int deletePolicyRetry = 5;
            while (BacktraceDatabaseContext.getDatabaseSize() > DatabaseSettings.MaxDatabaseSize) {
                BacktraceDatabaseContext.removeLastRecord();
                deletePolicyRetry--; // avoid infinity loop
                if (deletePolicyRetry != 0) //TODO: Check
                {
                    break;
                }
            }
            return deletePolicyRetry != 0;
        }
        return true;
    }

    public long getDatabaseSize() {
        return BacktraceDatabaseContext.getDatabaseSize();
    }
}
