package backtraceio.library.services;

import java.io.File;

import backtraceio.library.interfaces.IBacktraceDatabaseFileContext;
import backtraceio.library.models.database.BacktraceDatabaseRecord;

public class BacktraceDatabaseFileContext implements IBacktraceDatabaseFileContext {

    private final String _databasePath;
    private final long _maxDatabaseSize;
    private final int _maxRecordNumber;

    public BacktraceDatabaseFileContext(String databasePath, long maxDatabaseSize, int maxRecordNumber)
    {

        _databasePath = databasePath;
        _maxDatabaseSize = maxDatabaseSize;
        _maxRecordNumber = maxRecordNumber;
//        _databaseDirectoryInfo = new DirectoryInfo(_databasePath); // TODO:
    }

    public Iterable<File> GetRecords() {
        throw new UnsupportedOperationException();
    }

    public Iterable<File> GetAll() {
        throw new UnsupportedOperationException();
    }

    public boolean validFileConsistency() {
        throw new UnsupportedOperationException();

    }

    public void RemoveOrphaned(Iterable<BacktraceDatabaseRecord> existingRecords) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }
}