package backtraceio.library.interfaces;

import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.database.BacktraceDatabaseRecord;

public interface IBacktraceDatabaseContext {

    /**
     * Add new record to Database
     *
     * @param backtraceData Diagnostic data
     * @return
     */
    BacktraceDatabaseRecord add(BacktraceData backtraceData);

    /**
     * Add new data to database
     *
     * @param backtraceDatabaseRecord Database record
     * @return
     */
    BacktraceDatabaseRecord add(BacktraceDatabaseRecord backtraceDatabaseRecord);

    /**
     * Get first record or null
     *
     * @return First existing record in database store
     */
    BacktraceDatabaseRecord first();

    /**
     * Get last record or null
     *
     * @return Last existing record in database store
     */
    BacktraceDatabaseRecord last();

    /**
     * Get all records stored in Database
     *
     * @return
     */
    Iterable<BacktraceDatabaseRecord> get();

    /**
     * Delete database record by using BacktraceDatabaseRecord
     *
     * @param record Database record
     */
    void delete(BacktraceDatabaseRecord record);


    /**
     * Check if any similar record exists
     *
     * @param n Compared record
     * @return
     */
    boolean contains(BacktraceDatabaseRecord n);

    /**
     * Check if any similar record exists
     *
     * @return
     */
    boolean isEmpty();

    /**
     * Get total count of records
     *
     * @return Total number of records
     */
    int count();

    /**
     * Clear database
     */
    void clear();

    /**
     * Increment record time for all records
     */
    void incrementBatchRetry();

    /**
     * Get database size
     *
     * @return Database size
     */
    long getDatabaseSize();

    /**
     * Remove last record in database.
     *
     * @return If algorithm can remove last record, method return true. Otherwise false
     */
    boolean removeLastRecord();
}
