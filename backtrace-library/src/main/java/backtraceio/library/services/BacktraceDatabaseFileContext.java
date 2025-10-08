package backtraceio.library.services;

import backtraceio.library.common.FileHelper;
import backtraceio.library.interfaces.DatabaseFileContext;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.models.database.BacktraceDatabaseRecord;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class BacktraceDatabaseFileContext implements DatabaseFileContext {

    private static final transient String LOG_TAG = BacktraceDatabaseFileContext.class.getSimpleName();

    private final String _databasePath;
    private final long _maxDatabaseSize;
    private final int _maxRecordNumber;
    private final File _databaseDirectory;
    private final String recordFilterRegex = ".*-record.json";
    private final String _crashpadDatabasePathPrefix = "crashpad";

    public BacktraceDatabaseFileContext(String databasePath, long maxDatabaseSize, int maxRecordNumber) {
        _databasePath = databasePath;
        _maxDatabaseSize = maxDatabaseSize;
        _maxRecordNumber = maxRecordNumber;
        _databaseDirectory = new File(_databasePath);
    }

    /**
     * Get all physical files stored in database directory
     *
     * @return all existing physical files
     */
    public Iterable<File> getAll() {
        File[] files = this._databaseDirectory.listFiles();
        if (files == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(files);
    }

    /**
     * Get all valid physical records stored in database directory
     *
     * @return all existing physical records
     */
    public Iterable<File> getRecords() {
        BacktraceLogger.d(LOG_TAG, "Getting files from file context");
        final Pattern p = Pattern.compile(this.recordFilterRegex);
        File[] pagesTemplates =
                this._databaseDirectory.listFiles(f -> p.matcher(f.getName()).matches());
        if (pagesTemplates == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(pagesTemplates);
    }

    /**
     * Valid all files consistencies
     *
     * @return is database consistent
     */
    public boolean validFileConsistency() {
        BacktraceLogger.d(LOG_TAG, "Checking the consistency of files in file context");
        Iterable<File> files = this.getAll();

        long size = 0;
        long totalRecordFiles = 0;

        for (File file : files) {
            if (file.getName().matches(this.recordFilterRegex)) {
                totalRecordFiles++;
                if (_maxRecordNumber != 0 && _maxRecordNumber < totalRecordFiles) {
                    BacktraceLogger.w(LOG_TAG, "Total number of records is bigger than allowed");
                    return false;
                }
            }
            size += file.length();
            if (_maxDatabaseSize != 0 && size > _maxDatabaseSize) { // if _maxDatabaseSize == 0, size is unlimited
                BacktraceLogger.w(LOG_TAG, "Database size is bigger than allowed");
                return false;
            }
        }
        return true;
    }

    /**
     * Remove orphaned files existing in database directory
     *
     * @param existingRecords existing entries in BacktraceDatabaseContext
     */
    public void removeOrphaned(Iterable<BacktraceDatabaseRecord> existingRecords) {
        BacktraceLogger.d(LOG_TAG, "Removing orphaned files from file context");
        List<String> recordStringIds = new ArrayList<>();

        for (BacktraceDatabaseRecord record : existingRecords) {
            recordStringIds.add(record.id.toString());
        }

        Iterable<File> files = this.getAll();
        for (File file : files) {
            if (file.isDirectory() && file.getName().endsWith(this._crashpadDatabasePathPrefix)) {
                continue;
            }
            String extension = FileHelper.getFileExtension(file);
            if (!extension.equals("json")) {
                BacktraceLogger.d(LOG_TAG, "Deleting file - it is not a JSON file");
                file.delete();
                continue;
            }

            int fileNameIndex = file.getName().lastIndexOf('-');

            if (fileNameIndex == -1) {
                BacktraceLogger.d(LOG_TAG, "Deleting file - name is incorrect");
                file.delete();
                continue;
            }

            String fileUuid = file.getName().substring(0, fileNameIndex);

            if (!recordStringIds.contains(fileUuid)) {
                BacktraceLogger.d(LOG_TAG, "Deleting file - file id is not in existing collection");
                file.delete();
            }
        }
    }

    /**
     * Remove all files from database directory
     */
    public void clear() {
        BacktraceLogger.d(LOG_TAG, "Removing all files from database file context");
        Iterable<File> files = this.getAll();
        for (File file : files) {
            file.delete();
        }
    }
}
