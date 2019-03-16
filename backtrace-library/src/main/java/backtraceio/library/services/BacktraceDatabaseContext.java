package backtraceio.library.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import backtraceio.library.enums.database.RetryOrder;
import backtraceio.library.interfaces.IBacktraceDatabaseContext;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.database.BacktraceDatabaseRecord;
import backtraceio.library.models.database.BacktraceDatabaseSettings;

public class BacktraceDatabaseContext implements IBacktraceDatabaseContext {
    /**
     * Database cache
     */
    Map<Integer, List<BacktraceDatabaseRecord>> BatchRetry = new HashMap<>();

    /**
     * Total database size on hard drive
     */
    long TotalSize = 0;

    /**
     * Total records in BacktraceDatabase
     */
    int TotalRecords = 0;

    /**
     * Path to database directory
     */
    private String _path; // TODO: READONLY

    /**
     * Maximum number of retries
     */
    private int _retryNumber; // TODO: READONLY

    /**
     * Record order
     */
    RetryOrder _RetryOrder;



    public BacktraceDatabaseContext(BacktraceDatabaseSettings settings)
    {
        this(settings.databasePath, settings.retryLimit, settings.retryOrder);
    }

    /**
     * Initialize new instance of Backtrace Database Context
     * @param path path to database directory
     * @param retryNumber total number of retries
     * @param retryOrder record order
     */
    public BacktraceDatabaseContext(String path, int retryNumber, RetryOrder retryOrder){
        this._path = path;
        this._retryNumber = retryNumber;
        this._RetryOrder = retryOrder;
        SetupBatch();
    }

    /**
     * Setup cache
     */
    private void SetupBatch(){
        if(this._retryNumber == 0){
            throw new IllegalArgumentException("Retry number must be greater than 0!");
        }

        for (int i = 0; i < _retryNumber; i++)
        {
            this.BatchRetry.put(i, new ArrayList<BacktraceDatabaseRecord>());
        }
    }

    public BacktraceDatabaseRecord add(BacktraceData backtraceData) throws NullPointerException{
        if (backtraceData == null) {
            throw new NullPointerException("BacktraceData");
        }

        BacktraceDatabaseRecord record = new BacktraceDatabaseRecord(backtraceData, this._path);
        record.save();

        return add(record);
    }

    public BacktraceDatabaseRecord add(BacktraceDatabaseRecord backtraceDatabaseRecord) {
        if (backtraceDatabaseRecord == null) {
            throw new NullPointerException("BacktraceDatabaseRecord");
        }

        this.TotalSize += backtraceDatabaseRecord.getSize();
        this.BatchRetry.get(0).add(backtraceDatabaseRecord); // TODO: null
        this.TotalRecords++;
        return backtraceDatabaseRecord;
    }

    public BacktraceDatabaseRecord first() {
        return null;
    }

    public BacktraceDatabaseRecord last() {
        return null;
    }

    public Iterable<BacktraceDatabaseRecord> get() {
        return null;
    }

    public void delete(BacktraceDatabaseRecord record) {

    }

    public boolean contains(BacktraceDatabaseRecord n) {
        return false;
    }

    public boolean isEmpty() {
        return false;
    }

    public int count() {
        return TotalRecords;
    }

    public void clear() {

    }

    @Override
    public void incrementBatchRetry() {

    }

    @Override
    public long getDatabaseSize() {
        return this.TotalSize;
    }

    @Override
    public boolean removeLastRecord() {
        return false;
    }
}
