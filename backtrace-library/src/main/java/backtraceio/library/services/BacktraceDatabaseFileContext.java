package backtraceio.library.services;

import java.io.File;

import backtraceio.library.interfaces.IBacktraceDatabaseFileContext;
import backtraceio.library.models.database.BacktraceDatabaseRecord;

public class BacktraceDatabaseFileContext implements IBacktraceDatabaseFileContext {
    public BacktraceDatabaseFileContext(String databasePath, long maxDatabaseSize, int maxRecordNumber)
    {
//        _databasePath = databasePath;
//        _maxDatabaseSize = maxDatabaseSize;
//        _maxRecordNumber = maxRecordNumber;
//        _databaseDirectoryInfo = new DirectoryInfo(_databasePath);
    }

    public Iterable<File> GetRecords() {
        return null;
    }

    public Iterable<File> GetAll() {
        return null;
    }

    public boolean validFileConsistency() {
        return false;
    }

    public void RemoveOrphaned(Iterable<BacktraceDatabaseRecord> existingRecords) {

    }

    public void clear() {

    }
}
