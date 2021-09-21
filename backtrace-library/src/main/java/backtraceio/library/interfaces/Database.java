package backtraceio.library.interfaces;

import java.util.Map;

import backtraceio.library.BacktraceCredentials;
import backtraceio.library.base.BacktraceBase;
import backtraceio.library.enums.UnwindingMode;
import backtraceio.library.models.database.BacktraceDatabaseRecord;
import backtraceio.library.models.database.BacktraceDatabaseSettings;
import backtraceio.library.models.json.BacktraceReport;

public interface Database {
    /**
     * Start all database tasks - data storage, timers, file loading
     */
    void start();

    /**
     * Send all reports stored in BacktraceDatabase and clean database
     */
    void flush();

    /**
     * @param backtraceApi
     */
    void setApi(Api backtraceApi);

    /**
     * Remove all existing reports in BacktraceDatabase
     */
    void clear();

    /**
     * Check all database consistency requirements
     *
     * @return is database has valid consistency requirements
     */
    boolean validConsistency();

    /**
     * Add new report to Database
     *
     * @param backtraceReport
     * @param attributes
     * @return
     */
    BacktraceDatabaseRecord add(BacktraceReport backtraceReport, Map<String, Object> attributes);

    /**
     * Add new report to Database
     *
     * @param backtraceReport
     * @param attributes
     * @param isProguardEnabled
     * @return
     */
    BacktraceDatabaseRecord add(BacktraceReport backtraceReport, Map<String, Object> attributes, boolean isProguardEnabled);

    /**
     * @return
     */
    Iterable<BacktraceDatabaseRecord> get();

    /**
     * @param record
     */
    void delete(BacktraceDatabaseRecord record);

    /**
     * Get database settings
     *
     * @return
     */
    BacktraceDatabaseSettings getSettings();

    /**
     * Get database size
     *
     * @return
     */
    long getDatabaseSize();

    /**
     * Setup database NDK integration
     *
     * @param client      Backtrace client
     * @param credentials Backtrace credentials
     */
    Boolean setupNativeIntegration(BacktraceBase client, BacktraceCredentials credentials);

    /**
     * Setup native crash handler
     *
     * @param client                    Backtrace client
     * @param credentials               Backtrace credentials
     * @param enableClientSideUnwinding Enable client side unwinding
     */
    Boolean setupNativeIntegration(BacktraceBase client, BacktraceCredentials credentials,
                                   boolean enableClientSideUnwinding);

    /**
     * Setup native crash handler
     *
     * @param client                    Backtrace client
     * @param credentials               Backtrace credentials
     * @param enableClientSideUnwinding Enable client side unwinding
     * @param unwindingMode             Unwinding mode to use for client side unwinding
     */
    Boolean setupNativeIntegration(BacktraceBase client, BacktraceCredentials credentials,
                                   boolean enableClientSideUnwinding, UnwindingMode unwindingMode);

    /**
     * Get the breadcrumbs implementation
     *
     * @return the breadcrumbs implementation for this Database, if any
     */
    Breadcrumbs getBreadcrumbs();
}
