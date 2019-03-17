package backtraceio.library.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
    private Map<Integer, List<BacktraceDatabaseRecord>> BatchRetry = new HashMap<>();

    /**
     * Total database size on hard drive
     */
    private long TotalSize = 0;

    /**
     * Total records in BacktraceDatabase
     */
    private int TotalRecords = 0;

    /**
     * Path to database directory
     */
    private final String _path;

    /**
     * Maximum number of retries
     */
    private final int _retryNumber;

    /**
     * Record order
     */
    private RetryOrder retryOrder;


    /**
     * Initialize new instance of Backtrace Database Context
     *
     * @param settings database settings
     */
    public BacktraceDatabaseContext(BacktraceDatabaseSettings settings) {
        this(settings.databasePath, settings.retryLimit, settings.retryOrder);
    }

    /**
     * Initialize new instance of Backtrace Database Context
     *
     * @param path        path to database directory
     * @param retryNumber total number of retries
     * @param retryOrder  record order
     */
    private BacktraceDatabaseContext(String path, int retryNumber, RetryOrder retryOrder) {
        this._path = path;
        this._retryNumber = retryNumber;
        this.retryOrder = retryOrder;
        SetupBatch();
    }

    /**
     * Setup cache
     */
    private void SetupBatch() {
        if (this._retryNumber == 0) {
            throw new IllegalArgumentException("Retry number must be greater than 0!");
        }

        for (int i = 0; i < _retryNumber; i++) {
            this.BatchRetry.put(i, new ArrayList<BacktraceDatabaseRecord>());
        }
    }

    /**
     * Add new record to database
     *
     * @param backtraceData diagnostic data that should be stored in database
     * @return new instance of DatabaseRecord
     * @throws NullPointerException if backtraceData is null
     */
    public BacktraceDatabaseRecord add(BacktraceData backtraceData) throws NullPointerException {
        if (backtraceData == null) {
            throw new NullPointerException("BacktraceData");
        }

        BacktraceDatabaseRecord record = new BacktraceDatabaseRecord(backtraceData, this._path);
        record.save();

        return add(record);
    }

    /**
     * Add existing record to database
     *
     * @param backtraceDatabaseRecord database record
     * @return database record
     */
    public BacktraceDatabaseRecord add(BacktraceDatabaseRecord backtraceDatabaseRecord) {
        if (backtraceDatabaseRecord == null) {
            throw new NullPointerException("BacktraceDatabaseRecord");
        }
        backtraceDatabaseRecord.Locked = true;
        this.TotalSize += backtraceDatabaseRecord.getSize();
        this.BatchRetry.get(0).add(backtraceDatabaseRecord); // TODO: null
        this.TotalRecords++;
        return backtraceDatabaseRecord;
    }

    /**
     * Get first existing database record. Method returns record based on order in database
     *
     * @return first Backtrace database record
     */
    public BacktraceDatabaseRecord first() {
        return retryOrder == RetryOrder.Queue
                ? getFirstRecord()
                : getLastRecord();
    }

    /**
     * Get last existing database record. Method returns record based on order in database
     *
     * @return last Backtrace database record
     */
    public BacktraceDatabaseRecord last() {
        return this.retryOrder == RetryOrder.Stack ? getLastRecord() : getFirstRecord();
    }


    /**
     * Get all database records
     *
     * @return all existing database records
     */
    public Iterable<BacktraceDatabaseRecord> get() {
        List<BacktraceDatabaseRecord> allRecords = new ArrayList<>();
        for (Map.Entry<Integer, List<BacktraceDatabaseRecord>> entry : BatchRetry.entrySet()) {
            allRecords.addAll(entry.getValue());
        }
        return allRecords;
    }

    /**
     * Delete existing record from database
     *
     * @param record Database record to delete
     */
    public void delete(BacktraceDatabaseRecord record) {
        if (record == null) {
            return;
        }

        // TODO: Check is it works
        for (int key : BatchRetry.keySet()) {
            for (BacktraceDatabaseRecord databaseRecord : BatchRetry.get(key)) {
                if (databaseRecord == null || record.Id != databaseRecord.Id) {
                    continue;
                }

                databaseRecord.delete();
                BatchRetry.get(key).remove(databaseRecord);
                this.TotalRecords--;
                this.TotalSize -= databaseRecord.getSize();
                return;
            }
        }
    }

    /**
     * Check if the record passed as parameter exists
     *
     * @param record database record
     * @return is record passed as argument is in the database
     */
    public boolean contains(BacktraceDatabaseRecord record) {
        for (Map.Entry<Integer, List<BacktraceDatabaseRecord>> entry : this.BatchRetry.entrySet()) {
            List<BacktraceDatabaseRecord> records = entry.getValue();

            for (BacktraceDatabaseRecord databaseRecord : records) {
                if (databaseRecord != null && databaseRecord.Id == record.Id) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if database is empty
     *
     * @return is database empty
     */
    public boolean isEmpty() {
        return TotalRecords != 0;
    }

    /**
     * Get total number of records in database
     *
     * @return number of records in database
     */
    public int count() {
        return TotalRecords;
    }

    /**
     * Delete all records from database
     */
    public void clear() {
        for (Map.Entry<Integer, List<BacktraceDatabaseRecord>> entry : this.BatchRetry.entrySet()) {
            List<BacktraceDatabaseRecord> records = entry.getValue();

            for (BacktraceDatabaseRecord databaseRecord : records) {
                databaseRecord.delete();
            }
        }

        this.TotalRecords = 0;
        this.TotalSize = 0;

        for (Map.Entry<Integer, List<BacktraceDatabaseRecord>> entry : this.BatchRetry.entrySet()) {
            entry.getValue().clear();
        }
    }

    /**
     * Increment retry time for current record
     */
    public void incrementBatchRetry() {
        removeMaxRetries();
        incrementBatches();
    }

    /**
     * Get database size
     *
     * @return database size
     */
    public long getDatabaseSize() {
        return this.TotalSize;
    }

    public boolean removeLastRecord() {
        BacktraceDatabaseRecord record = this.last();
        if (record == null) {
            return false;
        }

        record.delete();
        this.TotalSize--;
        this.TotalSize -= record.getSize();
        return true;
    }

    /**
     * Increment each batch
     */
    private void incrementBatches() {
        for (int i = this._retryNumber - 2; i >= 0; i--) {
            List<BacktraceDatabaseRecord> temp = this.BatchRetry.get(i);
            BatchRetry.put(i, new ArrayList<BacktraceDatabaseRecord>());
            BatchRetry.put(i + 1, temp);
        }
    }

    /**
     * Remove last batch
     */
    private void removeMaxRetries() {
        List<BacktraceDatabaseRecord> currentBatch = this.BatchRetry.get(_retryNumber - 1);

        for (BacktraceDatabaseRecord record : currentBatch) {
            if (!record.valid()) {
                continue;
            }
            record.delete();
            this.TotalRecords--;
            TotalSize -= record.getSize();
        }
    }


    /**
     * Get first record in in-cache BacktraceDatabase
     *
     * @return first database record
     */
    private BacktraceDatabaseRecord getFirstRecord() {
        return getRecordFromCache(false);
    }

    /**
     * Get last record in in-cache BacktraceDatabase
     *
     * @return last database record
     */
    private BacktraceDatabaseRecord getLastRecord() {
        return getRecordFromCache(true);
    }


    /**
     * Get record in in-cache BacktraceDatabase
     *
     * @param reverse reverse the order of records
     * @return first unlocked record
     */
    private BacktraceDatabaseRecord getRecordFromCache(boolean reverse) {
        for (int i = _retryNumber - 1; i >= 0; i--) {
            List<BacktraceDatabaseRecord> reverseRecords = BatchRetry.get(i);
            if (reverse) {
                Collections.reverse(reverseRecords);
            }
            for (BacktraceDatabaseRecord record : reverseRecords) {
                if (!record.Locked) {
                    record.Locked = true;
                    return record;
                }
            }
        }
        return null;
    }

}
