package backtraceio.library.services;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import backtraceio.library.enums.database.RetryOrder;
import backtraceio.library.interfaces.DatabaseContext;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.database.BacktraceDatabaseRecord;
import backtraceio.library.models.database.BacktraceDatabaseSettings;

public class BacktraceDatabaseContext implements DatabaseContext {

    private static final transient String LOG_TAG = BacktraceDatabaseContext.class.getSimpleName();

    /**
     * Path to database directory
     */
    private final String _path;

    /**
     * Maximum number of retries
     */
    private final int _retryNumber;

    /**
     * Database cache
     */
    private final Map<Integer, Queue<BacktraceDatabaseRecord>> batchRetry = new ConcurrentHashMap<>();

    /**
     * Total database size on hard drive
     */
    private final AtomicLong totalSize = new AtomicLong(0);

    /**
     * Total records in BacktraceDatabase
     */
    private final AtomicInteger totalRecords = new AtomicInteger(0);

    /**
     * Record order
     */
    private final RetryOrder retryOrder;

    /**
     * @deprecated This constructor will be removed in future versions.
     * The {@code context} parameter is no longer used.
     * Please use the constructor without the {@code context} parameter.
     *
     * <p>Use {@link #BacktraceDatabaseContext(BacktraceDatabaseSettings)} instead.</p>
     *
     * @param context The unused Android context parameter.
     * @param settings The database settings.
     */
    @Deprecated
    public BacktraceDatabaseContext(Context context, BacktraceDatabaseSettings settings) {
        this(settings);
    }

    /**
     * Initialize new instance of Backtrace Database Context
     *
     * @param settings database settings
     */
    public BacktraceDatabaseContext(BacktraceDatabaseSettings settings) {
        this(settings.getDatabasePath(), settings.getRetryLimit(), settings.getRetryOrder());
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
        setupBatch();
    }

    /**
     * Setup cache
     */
    private void setupBatch() {
        if (this._retryNumber <= 0) {
            throw new IllegalArgumentException("Retry number must be greater than 0!");
        }

        for (int i = 0; i < _retryNumber; i++) {
            this.batchRetry.put(i, new ConcurrentLinkedQueue<>());
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
        BacktraceLogger.d(LOG_TAG, "Adding new record to database context");
        if (backtraceData == null) {
            BacktraceLogger.e(LOG_TAG, "BacktraceData is null");
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
        BacktraceLogger.d(LOG_TAG, "Adding new record to database context");
        if (backtraceDatabaseRecord == null) {
            BacktraceLogger.e(LOG_TAG, "Backtrace database record is null");
            throw new NullPointerException("BacktraceDatabaseRecord");
        }
        backtraceDatabaseRecord.locked = true;
        this.totalSize.addAndGet(backtraceDatabaseRecord.getSize());
        this.addToFirstBatch(backtraceDatabaseRecord);
        this.totalRecords.incrementAndGet();
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
        return this.retryOrder == RetryOrder.Queue ? getLastRecord() : getFirstRecord();
    }


    /**
     * Get all database records
     *
     * @return all existing database records
     */
    public Iterable<BacktraceDatabaseRecord> get() {
        List<BacktraceDatabaseRecord> allRecords = new ArrayList<>();
        for (Map.Entry<Integer, Queue<BacktraceDatabaseRecord>> entry : batchRetry.entrySet()) {
            allRecords.addAll(entry.getValue());
        }
        return allRecords;
    }

    /**
     * Get database size
     *
     * @return database size
     */
    public long getDatabaseSize() {
        return this.totalSize.get();
    }


    /**
     * Delete existing record from database
     *
     * @param record Database record to delete
     */
    public boolean delete(BacktraceDatabaseRecord record) {
        if (record == null) {
            return false;
        }

        for (int key : batchRetry.keySet()) {
            Queue<BacktraceDatabaseRecord> records = batchRetry.get(key);

            if (records == null) {
                continue;
            }

            Iterator<BacktraceDatabaseRecord> iterator = records.iterator();
            while (iterator.hasNext()) {
                BacktraceDatabaseRecord databaseRecord = iterator.next();
                if (databaseRecord == null || !record.id.equals(databaseRecord.id)) {
                    continue;
                }

                databaseRecord.delete();
                try {
                    iterator.remove();
                    this.totalRecords.decrementAndGet();
                    this.totalSize.addAndGet(-databaseRecord.getSize());
                    return true;
                } catch (Exception e) {
                    BacktraceLogger.d(LOG_TAG, "Exception on removing record "
                            + databaseRecord.id.toString() + "from db context: " + e.getMessage());
                }
            }
        }
        return false;
    }

    /**
     * Check if the record passed as parameter exists
     *
     * @param record database record
     * @return is record passed as argument is in the database
     */
    public boolean contains(BacktraceDatabaseRecord record) {
        if (record == null) {
            throw new NullPointerException("BacktraceDatabaseRecord");
        }
        for (Map.Entry<Integer, Queue<BacktraceDatabaseRecord>> entry : this.batchRetry.entrySet()) {
            Queue<BacktraceDatabaseRecord> records = entry.getValue();

            for (BacktraceDatabaseRecord databaseRecord : records) {
                if (databaseRecord != null && databaseRecord.id == record.id) {
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
        return totalRecords.get() == 0;
    }

    /**
     * Get total number of records in database
     *
     * @return number of records in database
     */
    public int count() {
        return totalRecords.get();
    }

    /**
     * Delete all records from database
     */
    public void clear() {
        BacktraceLogger.d(LOG_TAG, "Deleting all records from database context");
        for (Map.Entry<Integer, Queue<BacktraceDatabaseRecord>> entry : this.batchRetry.entrySet()) {
            Queue<BacktraceDatabaseRecord> records = entry.getValue();

            for (BacktraceDatabaseRecord databaseRecord : records) {
                databaseRecord.delete();
            }
        }

        this.totalRecords.set(0);
        this.totalSize.set(0);

        for (Map.Entry<Integer, Queue<BacktraceDatabaseRecord>> entry : this.batchRetry.entrySet()) {
            entry.getValue().clear();
        }
    }

        /**
     * Delete the oldest file
     *
     * @return is deletion was successful
     */
    public boolean removeOldestRecord() {
        BacktraceLogger.d(LOG_TAG, "Removing oldest record from database context");
        BacktraceDatabaseRecord record = this.first();

        if (record == null) {
            BacktraceLogger.w(LOG_TAG, "Oldest record in database is null");
            return false;
        }
        return delete(record);
    }

    /**
     * Increment each batch
     */
    private void incrementBatches() {
        for (int i = this._retryNumber - 2; i >= 0; i--) {
            Queue<BacktraceDatabaseRecord> currentBatch = this.batchRetry.get(i);
            batchRetry.put(i, new ConcurrentLinkedQueue<>());
            batchRetry.put(i + 1, currentBatch);
        }
    }

    /**
     * Remove last batch
     */
    private void removeMaxRetries() {
        Queue<BacktraceDatabaseRecord> currentBatch = this.batchRetry.get(_retryNumber - 1);

        for (BacktraceDatabaseRecord record : currentBatch) {
            if (!record.valid()) {
                continue;
            }
            record.delete();
            this.totalRecords.decrementAndGet();
            totalSize.addAndGet(-record.getSize());
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
            Queue<BacktraceDatabaseRecord> batchRecords = batchRetry.get(i);

            if (batchRecords == null) {
                continue;
            }

            Iterable<BacktraceDatabaseRecord> records = batchRecords;
            if (reverse) {
                List<BacktraceDatabaseRecord> recordsList = new ArrayList<>(batchRecords);
                Collections.reverse(recordsList);
                records = recordsList;
            }

            for (BacktraceDatabaseRecord record : records) {
                if (record != null && !record.locked) {
                    record.locked = true;
                    return record;
                }
            }
        }
        return null;
    }

    private void addToFirstBatch(BacktraceDatabaseRecord backtraceDatabaseRecord) {
        final int firstBatch = 0;

        if (this.batchRetry.isEmpty()) {
            this.batchRetry.put(firstBatch, new ConcurrentLinkedQueue<>());
        }

        Queue<BacktraceDatabaseRecord> batch = this.batchRetry.get(firstBatch);

        if (batch == null) {
            batch = new ConcurrentLinkedQueue<>();
            this.batchRetry.put(firstBatch, batch);
        }

        batch.add(backtraceDatabaseRecord);
    }
}
