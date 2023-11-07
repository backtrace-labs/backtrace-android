package backtraceio.library.interfaces;

import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.database.BacktraceDatabaseRecord;

public interface DatabaseContext {

    /**
     * Add new record to Database
     *
     * @param backtraceData Diagnostic data
     * @return current database record
     */
    BacktraceDatabaseRecord add(BacktraceData backtraceData);

    /**
     * Add new data to database
     *
     * @param backtraceDatabaseRecord Database record
     * @return current database record
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
     * @return all existing database records
     */
    Iterable<BacktraceDatabaseRecord> get();

    /**
     * Delete database record by using BacktraceDatabaseRecord
     *
     * @param record Database record
     */
    boolean delete(BacktraceDatabaseRecord record);


    /**
     * Check if any similar record exists
     *
     * @param n Compared record
     * @return is record passed as argument is in the database
     */
    boolean contains(BacktraceDatabaseRecord n);

    /**
     * Check if any similar record exists
     *
     * @return is database empty
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
    boolean removeOldestRecord();
}
