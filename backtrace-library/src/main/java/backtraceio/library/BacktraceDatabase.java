package backtraceio.library;

import java.io.File;
import java.util.Map;

import backtraceio.library.common.FileHelper;
import backtraceio.library.enums.database.RetryBehavior;
import backtraceio.library.interfaces.IBacktraceApi;
import backtraceio.library.interfaces.IBacktraceDatabase;
import backtraceio.library.interfaces.IBacktraceDatabaseContext;
import backtraceio.library.interfaces.IBacktraceDatabaseFileContext;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.database.BacktraceDatabaseRecord;
import backtraceio.library.models.database.BacktraceDatabaseSettings;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.services.BacktraceDatabaseContext;
import backtraceio.library.services.BacktraceDatabaseFileContext;

/**
 * Backtrace Database
 */
public class BacktraceDatabase implements IBacktraceDatabase {

    public IBacktraceApi BacktraceApi;


    IBacktraceDatabaseContext BacktraceDatabaseContext;

    IBacktraceDatabaseFileContext BacktraceDatabaseFileContext;

    private BacktraceDatabaseSettings DatabaseSettings;

    private String DatabasePath;

    private boolean _timerBackgroundWork = false;


    private boolean _enable = false;

    //TODO: times

    /**
     * Create disabled instance of BacktraceDatabase
     */
    public BacktraceDatabase(){}

    /**
     * Create new Backtrace database instance
     * @param path Path to database directory
     */
    public BacktraceDatabase(String path)
    {
        this(new BacktraceDatabaseSettings(path));
    }

    /**
     * Create Backtrace database instance
     * @param databaseSettings Backtrace database settings
     */
    public BacktraceDatabase(BacktraceDatabaseSettings databaseSettings)
    {
        if(databaseSettings == null || (databaseSettings.databasePath != null && !databaseSettings.databasePath.isEmpty()))
        {
            return;
        }

        if(!FileHelper.isFileExists(databaseSettings.databasePath))
        {
            throw new IllegalArgumentException("Database path does not exists");
        }

        DatabaseSettings = databaseSettings;
        BacktraceDatabaseContext = new BacktraceDatabaseContext(databaseSettings);
        BacktraceDatabaseFileContext = new BacktraceDatabaseFileContext(DatabasePath, DatabaseSettings.MaxDatabaseSize, DatabaseSettings.maxRecordCount);
    }

    public void start() {
        if(DatabaseSettings == null){
            return;
        }

//        TODO:
//        if(BacktraceDatabaseContext == null || BacktraceDatabaseContext.isEmpty())
//        {
//
//        }

        this.loadReports();

        this.removeOrphanded();
        
        if(DatabaseSettings.retryBehavior == RetryBehavior.ByInterval || DatabaseSettings.autoSendMode)
        {
            setupTimer();
        }

        this._enable = true;
    }

    /**
     * Get settings
     * @return current database settings
     */
    public BacktraceDatabaseSettings getSettings(){
        return DatabaseSettings;
    }


    public void setupTimer(){
        throw new UnsupportedOperationException();
    }

    public void flush() {
        if(this.BacktraceApi == null)
        {
            throw new IllegalArgumentException("BacktraceApi is required if you want to use Flush method");
        }

        BacktraceDatabaseRecord record = BacktraceDatabaseContext.first();
        while (record != null)
        {
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

    private void removeOrphanded(){
        Iterable<BacktraceDatabaseRecord> records = BacktraceDatabaseContext.get();
        BacktraceDatabaseFileContext.removeOrphaned(records);
    }

    public boolean validConsistency() {
        return false;
    }

    public BacktraceDatabaseRecord add(BacktraceReport backtraceReport, Map<String, Object> attributes) {
        if(!this._enable || backtraceReport == null)
        {
            return null;
        }

        boolean validationResult = ValidateDatabaseSize();
        if(!validationResult)
        {
            return null;
        }

        BacktraceData data = backtraceReport.toBacktraceData(null, attributes); // TODO: change null with application context!
        return BacktraceDatabaseContext.add(data);
    }

    public Iterable<BacktraceDatabaseRecord> get() {
        throw new UnsupportedOperationException();
    } // TODO:


    public void delete(BacktraceDatabaseRecord record) {
        if(this.BacktraceDatabaseContext == null)
        {
            return;
        }
        this.BacktraceDatabaseContext.delete(record);
    }

    int count(){
        return BacktraceDatabaseContext.count();
    }

    private void loadReports()
    {
        Iterable<File> files = BacktraceDatabaseFileContext.getRecords();

        for(File file : files)
        {
            BacktraceDatabaseRecord record = BacktraceDatabaseRecord.readFromFile(file);
            if(!record.valid())
            {
                record.delete();
                continue;
            }
            BacktraceDatabaseContext.add(record);
            validateDatabaseSize();

        }
    }

    private boolean validateDatabaseSize(){
        throw new UnsupportedOperationException();
    }

    public long getDatabaseSize() {
        return 0;
    }

    public boolean ValidateDatabaseSize()
    {
        return false;
    }
}
