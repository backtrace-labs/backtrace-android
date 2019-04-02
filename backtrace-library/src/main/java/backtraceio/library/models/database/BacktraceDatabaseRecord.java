package backtraceio.library.models.database;

import android.util.Log;

import com.google.gson.annotations.SerializedName;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.UUID;

import backtraceio.library.common.BacktraceSerializeHelper;
import backtraceio.library.common.FileHelper;
import backtraceio.library.interfaces.IBacktraceDatabaseRecordWriter;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.json.BacktraceReport;

public class BacktraceDatabaseRecord {
    /**
     * Id
     */
    @SerializedName("Id")
    public UUID id = UUID.randomUUID();

    /**
     * Check if current record is in use
     */
    public transient boolean Locked = false;

    /**
     * Path to json stored all information about current record
     */
    @SerializedName("RecordName")
    private String recordPath;

    public String getRecordPath() {
        return recordPath;
    }

    /**
     * Path to a diagnostic data json
     */
    @SerializedName("DataPath")
    private String diagnosticDataPath;

    public String getDiagnosticDataPath() {
        return diagnosticDataPath;
    }

    /**
     * Path to Backtrace Report json
     */
    @SerializedName("ReportPath")
    private String reportPath;

    public String getReportPath() {
        return reportPath;
    }

    /**
     * Total size of record
     */
    @SerializedName("Size")
    private long size;

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    /**
     * Stored record
     */
    private transient BacktraceData record;

    /**
     * Path to database directory
     */
    private transient final String _path;

    /**
     * record writer
     */
    transient IBacktraceDatabaseRecordWriter RecordWriter;


    BacktraceDatabaseRecord() {
        this._path = "";
        this.recordPath = String.format("%s-record.json", this.id);
        this.diagnosticDataPath = String.format("%s-attachment", this.id);
        this.recordPath = String.format("%s-record.json", this.id);
    }

    public BacktraceDatabaseRecord(BacktraceData data, String path) {
        this.id = UUID.fromString(data.uuid);
        this.record = data;
        this._path = path;
        RecordWriter = new BacktraceDatabaseRecordWriter(path);
    }

    /**
     * Get valid BacktraceData from current record
     *
     * @return valid BacktraceData object
     */
    public BacktraceData getBacktraceData() {
        if (this.record != null) {
            return this.record;
        }

        if (!this.valid()) {
            return null;
        }

        String jsonData = readJsonString(new File(this.diagnosticDataPath));
        String jsonReport = readJsonString(new File(this.reportPath));

        // deserialize data - if deserialize fails, we receive invalid entry
        try {
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
            Log.e("Backtrace.IO", ex.getMessage());
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
            this.diagnosticDataPath = save(record, String.format("%s-attachment", id));
            this.reportPath = save(record.report, String.format("%s-report", id));

            this.recordPath = new File(this._path,
                    String.format("%s-record.json", this.id)).getAbsolutePath();

            String json = BacktraceSerializeHelper.toJson(this);
            byte[] file = json.getBytes(StandardCharsets.UTF_8);
            this.size += file.length;
            RecordWriter.write(this, String.format("%s-record", this.id));
            return true;
        } catch (Exception ex) {
            Log.e("Backtrace.IO", "Received IOException while saving data to database. ");
            Log.d("Backtrace.IO", String.format("Message %s", ex.getMessage()));
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
                return "";
            }
            String json = BacktraceSerializeHelper.toJson(data);
            byte[] file = json.getBytes(StandardCharsets.UTF_8);
            this.size += file.length;
            return RecordWriter.write(file, prefix);
        }
        catch (Exception ex) {
            Log.e("Backtrace.IO", "Received IOException while saving data to database. ");
            Log.d("Backtrace.IO", String.format("Message %s", ex.getMessage()));
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
                new File(path).delete();
            }
        } catch (Exception ex) {
            Log.e("Backtrace.IO", String.format("Cannot delete file: %s. Message: %s", path, ex
                    .getMessage()));
        }
    }

    public boolean close(){
        try {
            this.Locked = false;
            this.record = null;
            return true;
        }
        catch (Exception e)
        {
            Log.e("Backtrace.IO", "Can not unlock record");
        }
        return false;
    }

    private static String readJsonString(File file){
        try {
            Scanner scanner = new Scanner(file);
            StringBuilder sb = new StringBuilder();

            while (scanner.hasNext()) {
                sb.append(scanner.nextLine());
            }

            scanner.close();

            return sb.toString();
        }
        catch (Exception e){
            Log.e("Backtrace.IO", e.getMessage());
            return null;
        }
    }

    /**
     * Read single record from file
     *
     * @param file current file
     * @return saved database record
     */
    public static BacktraceDatabaseRecord readFromFile(File file) {
        String json = BacktraceDatabaseRecord.readJsonString(file);
        if(json == null || json.equals("")){
            return null;
        }
        return BacktraceSerializeHelper.fromJson(json, BacktraceDatabaseRecord.class);
    }
}