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
    public UUID Id = UUID.randomUUID();

    /**
     * Check if current record is in use
     */
    public transient boolean Locked = false;

    /**
     * Path to json stored all information about current record
     */
    @SerializedName("RecordName")
    String RecordPath;

    /**
     * Path to a diagnostic data json
     */
    @SerializedName("DataPath")
    String DiagnosticDataPath;

    /**
     * Path to Backtrace Report json
     */
    @SerializedName("ReportPath")
    String ReportPath;

    public long getSize() {
        return Size;
    }

    public void setSize(long size) {
        Size = size;
    }

    /**
     * Total size of record
     */
    @SerializedName("Size")
    long Size;

    /**
     * Stored record
     */
    transient BacktraceData Record;

    /**
     * Path to database directory
     */
    private transient final String _path;

    /**
     * Record writer
     */
    transient IBacktraceDatabaseRecordWriter RecordWriter;


    BacktraceDatabaseRecord() {
        this._path = "";
        this.RecordPath = String.format("%s-record.json", this.Id);
    }

    public BacktraceDatabaseRecord(BacktraceData data, String path) {
        this.Id = UUID.fromString(data.uuid); // TODO: Check
        this.Record = data;
        this._path = path;
        RecordWriter = new BacktraceDatabaseRecordWriter(path);
    }

    /**
     * Get valid BacktraceData from current record
     *
     * @return valid BacktraceData object
     */
    public BacktraceData getBacktraceData() {
        if (this.Record != null) {
            return this.Record;
        }

        if (!this.valid()) {
            return null;
        }

        String jsonData = ""; // TODO:
        String jsonReport = ""; // TODO:

        // deserialize data - if deserialize fails, we receive invalid entry
        try {
            BacktraceData diagnosticData = BacktraceSerializeHelper.fromJson(jsonData,
                    BacktraceData.class); // TODO: Check
            BacktraceReport report = BacktraceSerializeHelper.fromJson(jsonReport,
                    BacktraceReport.class); // TODO: Check
            // add report to diagnostic data
            // we don't store report with diagnostic data in the same json
            // because we have easier way to serialize and deserialize data
            // and no problem/condition with serialization when BacktraceApi want to send
            // diagnostic data to API
            diagnosticData.report = report;
            return diagnosticData;
        } catch (Exception ex) {
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
            this.DiagnosticDataPath = save(Record, String.format("%s-attachment", Id));
            this.ReportPath = save(Record, String.format("%s-report", Id));

            this.RecordPath = new File(this._path, String.format("%s-record.json", this.Id))
                    .getAbsolutePath();

            String json = BacktraceSerializeHelper.toJson(this);
            byte[] file = json.getBytes(StandardCharsets.UTF_8);
            this.Size += file.length;
            RecordWriter.write(this, String.format("%s-record", this.Id));
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
            String json = BacktraceSerializeHelper.toJson(this);
            byte[] file = json.getBytes(StandardCharsets.UTF_8);
            this.Size += file.length;
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
        return FileHelper.isFileExists(this.DiagnosticDataPath) && FileHelper.isFileExists(this
                .ReportPath);
    }

    /**
     * Delete all record files
     */
    public void delete() {
        delete(this.ReportPath);
        delete(this.DiagnosticDataPath);
        delete(this.RecordPath);
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

    /**
     * Read single record from file
     *
     * @param file current file
     * @return saved database record
     */
    public static BacktraceDatabaseRecord readFromFile(File file) {
        try {
            Scanner scanner = new Scanner(file);
            StringBuilder sb = new StringBuilder();

            while (scanner.hasNext()) {
                sb.append(scanner.next());
            }

            scanner.close();

            String json = sb.toString();
            return BacktraceSerializeHelper.fromJson(json, BacktraceDatabaseRecord.class);
        } catch (Exception e) {
            return null;
        }
    }
}