package backtraceio.library;

import java.util.Map;

import backtraceio.library.interfaces.IBacktraceApi;
import backtraceio.library.interfaces.IBacktraceDatabase;
import backtraceio.library.models.database.BacktraceDatabaseRecord;
import backtraceio.library.models.database.BacktraceDatabaseSettings;
import backtraceio.library.models.json.BacktraceReport;

/**
 * Backtrace Database
 */
public class BacktraceDatabase implements IBacktraceDatabase {

    public void start() {

    }

    public void flush() {

    }

    public void setApi(IBacktraceApi backtraceApi) {

    }

    public void clear() {

    }

    public boolean validConsistency() {
        return false;
    }

    public BacktraceDatabaseRecord add(BacktraceReport backtraceReport, Map<String, Object> attributes) {
        return null;
    }

    public Iterable<BacktraceDatabaseRecord> get() {
        return null;
    }


    public void delete(BacktraceDatabaseRecord record) {

    }

    public BacktraceDatabaseSettings getSettings() {
        return null;
    }

    public long getDatabaseSize() {
        return 0;
    }
}
