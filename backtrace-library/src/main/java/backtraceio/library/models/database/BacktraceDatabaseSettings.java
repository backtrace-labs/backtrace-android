package backtraceio.library.models.database;

import backtraceio.library.enums.database.RetryBehavior;

/**
 * Backtrace library database settings
 */
public class BacktraceDatabaseSettings
{
    public BacktraceDatabaseSettings(String path)
    {
        DatabasePath = path;
    }

    /**
     * Directory path where reports are stored
     */
    public String DatabasePath;

    /**
     * Maximum number of stored reports in Database. If value is equal to zero, then limit not exists
     */
    public int MaxRecordCount  = 0;

    /**
     * Database size in MB
     */
    private long _maxDatabaseSize = 0;

    /**
     * Maximum database size in MB. If value is equal to zero, then size is unlimited
     */
    public long MaxDatabaseSize;


    public long getMaxDatabaseSize(){
        return _maxDatabaseSize * 1000 *1000;
    }

    public void setMaxDatabaseSize(long value){
        this._maxDatabaseSize = value;
    }

    /**
     * Resend report when http client throw exception
     */
    public boolean AutoSendMode  = false;

    /**
     * Retry behaviour
     */
//    public RetryBehavior RetryBehavior = RetryBehavior.ByInterval; // TODO:

    /**
     * How much seconds library should wait before next retry.
     */
    public int RetryInterval  = 5; // TODO: prevent negative value

    /**
     * Maximum number of retries
     */
    public int RetryLimit  = 3; // TODO: prevent negative value

//    public RetryOrder RetryOrder = RetryOrder.Queue; // TODO:
}
