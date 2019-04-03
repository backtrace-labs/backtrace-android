package backtraceio.library.models.database;

import backtraceio.library.enums.database.RetryBehavior;
import backtraceio.library.enums.database.RetryOrder;

/**
 * Backtrace library database settings
 */
public class BacktraceDatabaseSettings {
    public BacktraceDatabaseSettings(String path) {
        this(path, RetryOrder.Queue);
    }

    public BacktraceDatabaseSettings(String path, RetryOrder retryOrder) {
        this.databasePath = path;
        this.retryOrder = retryOrder;
    }

    /**
     * Directory path where reports are stored
     */
    private String databasePath;

    /**
     * Maximum number of stored reports in Database. If value is equal to zero, then limit not exists
     */
    private int maxRecordCount = 0;

    /**
     * Database size in MB
     */
    private long _maxDatabaseSize = 0;

    /**
     * Maximum database size in MB. If value is equal to zero, then size is unlimited
     */

    public long getMaxDatabaseSize() {
        return _maxDatabaseSize * 1000 * 1000;
    }


    public void setMaxDatabaseSize(long value) {
        this._maxDatabaseSize = value;
    }

    /**
     * Resend report when http client throw exception
     */
    private boolean autoSendMode = false;

    /**
     * Retry behaviour
     */
    private RetryBehavior retryBehavior = RetryBehavior.ByInterval;

    /**
     * How much seconds library should wait before next retry.
     */
    private int retryInterval = 5;

    /**
     * Maximum number of retries
     */
    private int retryLimit = 3;


    private RetryOrder retryOrder = RetryOrder.Queue;

    public String getDatabasePath() {
        return databasePath;
    }

    public void setDatabasePath(String databasePath) {
        this.databasePath = databasePath;
    }

    public int getMaxRecordCount() {
        return maxRecordCount;
    }

    public void setMaxRecordCount(int maxRecordCount) {
        this.maxRecordCount = maxRecordCount;
    }

    public long get_maxDatabaseSize() {
        return _maxDatabaseSize;
    }

    public void set_maxDatabaseSize(long _maxDatabaseSize) {
        this._maxDatabaseSize = _maxDatabaseSize;
    }

    public boolean isAutoSendMode() {
        return autoSendMode;
    }

    public void setAutoSendMode(boolean autoSendMode) {
        this.autoSendMode = autoSendMode;
    }

    public RetryBehavior getRetryBehavior() {
        return retryBehavior;
    }

    public void setRetryBehavior(RetryBehavior retryBehavior) {
        this.retryBehavior = retryBehavior;
    }

    public int getRetryInterval() {
        return retryInterval;
    }

    public void setRetryInterval(int retryInterval) {
        if(retryInterval <= 0)
        {
            throw new IllegalArgumentException("Retry interval value must be grader than zero");
        }
        this.retryInterval = retryInterval;
    }

    public int getRetryLimit() {
        return retryLimit;
    }

    public void setRetryLimit(int retryLimit) {
        if(retryInterval <= 0)
        {
            throw new IllegalArgumentException("Retry limit value must be grader than zero");
        }
        this.retryLimit = retryLimit;
    }

    public RetryOrder getRetryOrder() {
        return retryOrder;
    }

    public void setRetryOrder(RetryOrder retryOrder) {
        this.retryOrder = retryOrder;
    }


}
