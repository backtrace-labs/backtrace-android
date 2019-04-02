package backtraceio.library.services;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import backtraceio.library.common.FileHelper;
import backtraceio.library.interfaces.IBacktraceDatabaseFileContext;
import backtraceio.library.models.database.BacktraceDatabaseRecord;

public class BacktraceDatabaseFileContext implements IBacktraceDatabaseFileContext {

    private final String _databasePath;
    private final long _maxDatabaseSize;
    private final int _maxRecordNumber;
    private final File _databaseDirectory;
    private final String recordFilterRegex = ".*-record.json";

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
        return Arrays.asList(this._databaseDirectory.listFiles());
    }

    /**
     * Get all valid physical records stored in database directory
     *
     * @return all existing physical records
     */
    public Iterable<File> getRecords() {
        final Pattern p = Pattern.compile(this.recordFilterRegex);
        File[] pagesTemplates = this._databaseDirectory.listFiles(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return p.matcher(f.getName()).matches();
            }
        });
        return Arrays.asList(pagesTemplates);
    }

    /**
     * Valid all files consistencies
     *
     * @return is database consistent
     */
    public boolean validFileConsistency() {
        Iterable<File> files = this.getAll();

        long size = 0;
        long totalRecordFiles = 0;

        for (File file : files) {
            if (file.getName().matches(this.recordFilterRegex))
            {
                totalRecordFiles++;
                if (_maxRecordNumber < totalRecordFiles) {
                    return false;
                }
            }
            size += file.length();
            if (_maxDatabaseSize != 0 && size > _maxDatabaseSize) { // if _maxDatabaseSize == 0, size is unlimited
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
        List<String> recordStringIds = new ArrayList<>();

        for (BacktraceDatabaseRecord record : existingRecords) {
            recordStringIds.add(record.id.toString());
        }

        Iterable<File> files = this.getAll();
        for (File file : files) {
            String extension = FileHelper.getFileExtension(file);
            if (!extension.equals("json")) {
                file.delete();
                continue;
            }

            int fileNameIndex = file.getName().lastIndexOf('-');

            if (fileNameIndex == -1) {
                file.delete();
                continue;
            }

            String fileUuid = file.getName().substring(0, fileNameIndex);

            if (!recordStringIds.contains(fileUuid)) {
                file.delete();
            }
        }
    }

    /**
     * Remove all files from database directory
     */
    public void clear() {
        Iterable<File> files = this.getAll();
        for (File file : files) {
            file.delete();
        }
    }
}