package backtraceio.library;

import java.util.Map;

import backtraceio.library.common.FileHelper;
import backtraceio.library.interfaces.IBacktraceApi;
import backtraceio.library.interfaces.IBacktraceDatabase;
import backtraceio.library.interfaces.IBacktraceDatabaseContext;
import backtraceio.library.interfaces.IBacktraceDatabaseFileContext;
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
        if(databaseSettings == null || (databaseSettings.DatabasePath != null && !databaseSettings.DatabasePath.isEmpty())
        {
            return;
        }

        if(!FileHelper.isFileExists(databaseSettings.DatabasePath))
        {
            throw new IllegalArgumentException("Database path does not exists");
        }

        DatabaseSettings = databaseSettings;
        BacktraceDatabaseContext = new BacktraceDatabaseContext(DatabasePath, DatabaseSettings.RetryLimit, DatabaseSettings.RetryOrder);
        BacktraceDatabaseFileContext = new BacktraceDatabaseFileContext(DatabasePath, DatabaseSettings.MaxDatabaseSize, DatabaseSettings.MaxRecordCount);
    }

    public void start() {
        if(DatabaseSettings == null){
            return;
        }
    }

    public void flush() {

    }

    public void setApi(IBacktraceApi backtraceApi) {

    }

    public void clear() {

    }

    public boolean validConsistency() {
        return false;
    }

    public BacktraceDatabaseRecord add(BacktraceReport backtraceReport, Map<String, Object> attributes) {
        return null;
    }

    public Iterable<BacktraceDatabaseRecord> get() {
        return null;
    }


    public void delete(BacktraceDatabaseRecord record) {

    }

    public BacktraceDatabaseSettings getSettings() {
        return null;
    }

    public long getDatabaseSize() {
        return 0;
    }
}
