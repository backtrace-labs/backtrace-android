package backtraceio.library.models.database;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import backtraceio.library.common.BacktraceSerializeHelper;
import backtraceio.library.common.BacktraceStringHelper;
import backtraceio.library.common.FileHelper;
import backtraceio.library.common.serializers.SerializedName;
import backtraceio.library.interfaces.DatabaseRecordWriter;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.json.BacktraceReport;

public class BacktraceDatabaseRecord {

    private static transient final String LOG_TAG = BacktraceDatabaseRecord.class.getSimpleName();

    /**
     * Path to database directory
     */
    private final transient String path;

    /**
     * Id
     */
    @SerializedName("Id")
    public UUID id;

    /**
     * Check if current record is in use
     */
    public transient boolean locked = false;

    /**
     * record writer
     */
    private final transient DatabaseRecordWriter recordWriter;


    /**
     * Path to json stored all information about current record
     */
    @SerializedName("RecordName")
    private String recordPath;

    /**
     * Path to a diagnostic data json
     */
    @SerializedName("DataPath")
    private String diagnosticDataPath;

    /**
     * Path to Backtrace Report json
     */
    @SerializedName("ReportPath")
    private String reportPath;

    /**
     * Total size of record
     */
    @SerializedName("Size")
    private long size;

    /**
     * Stored record
     */
    private transient BacktraceData record;

    public BacktraceDatabaseRecord(BacktraceData data, String path) {
        this.id = UUID.fromString(data.getUuid());
        this.record = data;
        this.path = path;
        this.recordWriter = new BacktraceDatabaseRecordWriter(path);
    }

    public BacktraceDatabaseRecord(String id,
                                   String path,
                                   String recordPath,
                                   String diagnosticDataPath,
                                   String reportPath,
                                   long size) {
        this.id = UUID.fromString(id);
        this.recordPath = recordPath;
        this.diagnosticDataPath = diagnosticDataPath;
        this.reportPath = reportPath;
        this.path = path;
        this.size = size;
        this.recordWriter = new BacktraceDatabaseRecordWriter(path);

        this.record = getBacktraceData();
    }

    /**
     * Read single record from file
     *
     * @param file current file
     * @return saved database record
     */
    public static BacktraceDatabaseRecord readFromFile(File file) {
        BacktraceLogger.d(LOG_TAG, "Reading JSON from passed file");
        String json = FileHelper.readFile(file);
        if (BacktraceStringHelper.isNullOrEmpty(json)) {
            BacktraceLogger.w(LOG_TAG, "JSON from passed file is null or empty");
            return null;
        }
        return BacktraceSerializeHelper.fromJson(json, BacktraceDatabaseRecord.class);
    }

    public String getRecordPath() {
        return recordPath;
    }

    public String getDiagnosticDataPath() {
        return diagnosticDataPath;
    }

    public String getReportPath() {
        return reportPath;
    }

    public long getSize() {
        return size;
    }

    /**
     * Get BacktraceData object related to db record
     * @deprecated The {@code context} parameter is no longer used and this method will be removed in future versions.
     * Please use {@link #getBacktraceData()} instead.
     *
     * <p>The {@code context} parameter has no effect on the behavior of this method.</p>
     *
     * @param context The unused context parameter.
     * @return The BacktraceData object related to db record
     */
    @Deprecated
    public BacktraceData getBacktraceData(Context context) {
        return getBacktraceData();
    }
    /**
     * Get valid BacktraceData from current record
     * @return valid BacktraceData object
     */
    public BacktraceData getBacktraceData() {
        if (this.record != null) {
            return this.record;
        }

        if (!this.valid()) {
            BacktraceLogger.w(LOG_TAG, "Database record is invalid");
            return null;
        }

        String jsonData = FileHelper.readFile(new File(this.diagnosticDataPath));
        String jsonReport = FileHelper.readFile(new File(this.reportPath));

        // deserialize data - if deserialize fails, we receive invalid entry
        try {
            BacktraceLogger.d(LOG_TAG, "Deserialization diagnostic data");
            BacktraceData diagnosticData = BacktraceSerializeHelper.fromJson(jsonData,
                    BacktraceData.class);
            // add report to diagnostic data
            // we don't store report with diagnostic data in the same json
            // because we have easier way to serialize and deserialize data
            // and no problem/condition with serialization when BacktraceApi want to send
            // diagnostic data to API
            diagnosticData.report = BacktraceSerializeHelper.fromJson(jsonReport,
                    BacktraceReport.class);
            return diagnosticData;
        } catch (Exception ex) {
            BacktraceLogger.e(LOG_TAG, "Exception occurs on deserialization of diagnostic data", ex);
            return null;
        }
    }

    /**
     * Save data to internal app storage
     *
     * @return is saving successful
     */
    public boolean save() {
        try {
            BacktraceLogger.d(LOG_TAG, "Trying saving data to internal app storage");
            this.diagnosticDataPath = save(record, String.format("%s-attachment", id));
            this.reportPath = save(record.getReport(), String.format("%s-report", id));

            this.recordPath = new File(this.path,
                    String.format("%s-record.json", this.id)).getAbsolutePath();

            String json = BacktraceSerializeHelper.toJson(this);
            byte[] file = json.getBytes(StandardCharsets.UTF_8);
            this.size += file.length;
            recordWriter.write(this, String.format("%s-record", this.id));
            BacktraceLogger.d(LOG_TAG, "Saving data to internal app storage successful");
            return true;
        } catch (Exception ex) {
            BacktraceLogger.e(LOG_TAG, "Received IOException while saving data to database", ex);
            return false;
        }
    }

    /**
     * Save single file from database record
     *
     * @param data   single json file
     * @param prefix file prefix
     * @return path to file
     */
    private String save(Object data, String prefix) {
        try {
            if (data == null) {
                BacktraceLogger.w(LOG_TAG, "Passed data parameter is null");
                return "";
            }
            String json = BacktraceSerializeHelper.toJson(data);
            byte[] file = json.getBytes(StandardCharsets.UTF_8);
            this.size += file.length;
            return recordWriter.write(file, prefix);
        } catch (Exception ex) {
            BacktraceLogger.e(LOG_TAG, "Received IOException while saving data to database", ex);
            return ""; // TODO: consider a better solution
        }
    }

    /**
     * Check if all necessary files declared on record exists
     *
     * @return is record valid
     */
    public boolean valid() {
        return FileHelper.isFileExists(this.diagnosticDataPath) &&
                FileHelper.isFileExists(this.reportPath);
    }

    /**
     * Delete all record files
     */
    public void delete() {
        BacktraceLogger.d(LOG_TAG, "Trying delete files from database");
        delete(this.reportPath);
        delete(this.diagnosticDataPath);
        delete(this.recordPath);
    }

    /**
     * Delete single file on database record
     *
     * @param path path to file
     */
    private void delete(String path) {
        try {
            if (FileHelper.isFileExists(path)) {
                BacktraceLogger.d(LOG_TAG, "Passed path exist, trying delete file on database record");
                new File(path).delete();
            }
        } catch (Exception ex) {
            BacktraceLogger.e(LOG_TAG, String.format("Cannot delete file: %s", path), ex);
        }
    }

    public boolean close() {
        BacktraceLogger.d(LOG_TAG, "Trying unlock database record");
        try {
            this.locked = false;
            this.record = null;
            BacktraceLogger.d(LOG_TAG, "Record unlocked");
            return true;
        } catch (Exception e) {
            BacktraceLogger.e(LOG_TAG, "Can not unlock record");
        }
        return false;
    }
}