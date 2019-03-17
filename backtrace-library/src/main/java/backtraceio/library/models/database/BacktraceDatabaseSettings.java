package backtraceio.library.models.database;

import backtraceio.library.enums.database.DeduplicationStrategy;
import backtraceio.library.enums.database.RetryBehavior;
import backtraceio.library.enums.database.RetryOrder;

/**
 * Backtrace library database settings
 */
public class BacktraceDatabaseSettings {
    public BacktraceDatabaseSettings(String path) {
        databasePath = path;
    }

    /**
     * Directory path where reports are stored
     */
    public String databasePath;

    /**
     * Maximum number of stored reports in Database. If value is equal to zero, then limit not exists
     */
    public int maxRecordCount = 0;

    /**
     * Database size in MB
     */
    private long _maxDatabaseSize = 0;

    /**
     * Maximum database size in MB. If value is equal to zero, then size is unlimited
     */
    public long MaxDatabaseSize;


    public long getMaxDatabaseSize() {
        return _maxDatabaseSize * 1000 * 1000;
    }

    public void setMaxDatabaseSize(long value) {
        this._maxDatabaseSize = value;
    }

    /**
     * Resend report when http client throw exception
     */
    public boolean autoSendMode = false;

    /**
     * Retry behaviour
     */
    public RetryBehavior retryBehavior = RetryBehavior.ByInterval;

    /**
     * How much seconds library should wait before next retry.
     */
    public int retryInterval = 5; // TODO: prevent negative value

    /**
     * Maximum number of retries
     */
    public int retryLimit = 3; // TODO: prevent negative value


    public RetryOrder retryOrder = RetryOrder.Queue;
}
