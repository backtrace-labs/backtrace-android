package backtraceio.library.interfaces;

import java.util.Map;

import backtraceio.library.models.database.BacktraceDatabaseRecord;
import backtraceio.library.models.database.BacktraceDatabaseSettings;
import backtraceio.library.models.json.BacktraceReport;

public interface IBacktraceDatabase {
    /**
     * Start all database tasks - data storage, timers, file loading
     */
    void start();

    /**
     * Send all reports stored in BacktraceDatabase and clean database
     */
    void flush();

    /**
     *
     * @param backtraceApi
     */
    void setApi(IBacktraceApi backtraceApi);

    /**
     * Remove all existing reports in BacktraceDatabase
     */
    void clear();

    /**
     * Check all database consistency requirements
     * @return is database has valid consistency requirements
     */
    boolean validConsistency();

    /**
     * Add new report to Database
     * @param backtraceReport
     * @param attributes
     * @return
     */
    BacktraceDatabaseRecord add(BacktraceReport backtraceReport, Map<String, Object> attributes);

    /**
     *
     * @return
     */
    Iterable<BacktraceDatabaseRecord> get();

    /**
     *
     * @param record
     */
    void delete(BacktraceDatabaseRecord record);

    /**
     * Get database settings
     * @return
     */
    BacktraceDatabaseSettings getSettings();

    /**
     * Get database size
     * @return
     */
    long getDatabaseSize();
}
