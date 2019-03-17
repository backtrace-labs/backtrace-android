package backtraceio.library.interfaces;

import java.io.File;

import backtraceio.library.models.database.BacktraceDatabaseRecord;

public interface IBacktraceDatabaseFileContext {

    /**
     * Get all valid physical records stored in database directory
     * @return all existing physical records
     */
    Iterable<File> getRecords();

    /**
     * Get all physical files stored in database directory
     * @return all existing physical files
     */
    Iterable<File> getAll();


    /**
     * Valid all database files consistency
     * @return is a file consistent
     */
    boolean validFileConsistency();


    /**
     * Remove orphaned files existing in database directory
     * @param existingRecords existing entries in BacktraceDatabaseContext
     */
    void removeOrphaned(Iterable<BacktraceDatabaseRecord> existingRecords);

    /**
     * Remove all files from database directory
     */
    void clear();
}

