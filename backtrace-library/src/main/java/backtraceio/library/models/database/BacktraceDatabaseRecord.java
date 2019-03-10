package backtraceio.library.models.database;

import android.util.Log;

import com.google.gson.annotations.SerializedName;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.UUID;

import backtraceio.library.common.BacktraceSerializeHelper;
import backtraceio.library.common.FileHelper;
import backtraceio.library.interfaces.IBacktraceDatabaseRecordWriter;
import backtraceio.library.models.BacktraceData;

public class BacktraceDatabaseRecord {
    /**
     * Id
     */
    @SerializedName("Id")
    public UUID Id = UUID.randomUUID();

    /**
     * Check if current record is in use
     */
    transient boolean Locked = false;

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
    private transient final String _path = "";

    /**
     * Record writer
     */
    transient IBacktraceDatabaseRecordWriter RecordWriter;


    public BacktraceData getBacktraceData(){
        if(this.Record != null)
        {
            return this.Record;
        }

        if(!this.valid())
        {
            return null;
        }

        String jsonData = ""; // TODO:
        String jsonReport = ""; // TODO:

        try {
            BacktraceData diagnosticData = BacktraceSerializeHelper.
        }
        catch (Exception ex)
        {
            return null;
        }
    }
    /**
     * Save data to internal app storage
     * @return is saving successful
     */
    public boolean save(){
        try {
            this.DiagnosticDataPath = save(Record, String.format("%s-attachment", Id));
            this.ReportPath = save(Record, String.format("%s-report", Id));

            this.RecordPath = Paths.get("", "").toString();

            String json = BacktraceSerializeHelper.toJson(this);
            byte[] file = json.getBytes(StandardCharsets.UTF_8);
            this.Size += file.length;
            RecordWriter.write(this, String.format("%s-record", this.Id));
            return true;
        }
        catch (IOException io)
        {
            Log.e("Backtrace.IO", "Received IOException while saving data to database. ");
            Log.d("Backtrace.IO", String.format("Message %s", io.getMessage()));
            return false;
        }
        catch (Exception ex)
        {
            Log.e("Backtrace.IO", "Received IOException while saving data to database. ");
            Log.d("Backtrace.IO", String.format("Message %s", ex.getMessage()));
            return false;
        }
    }

    /**
     * Save single file from database record
     * @param data single json file
     * @param prefix file prefix
     * @return path to file
     */
    private String save(Object data, String prefix){
        if(data == null)
        {
            return "";
        }
        String json = BacktraceSerializeHelper.toJson(this);
        byte[] file = json.getBytes(StandardCharsets.UTF_8);
        this.Size += file.length;
        return RecordWriter.write(file, prefix);
    }

    /**
     * Check if all necessary files declared on record exists
     * @return is record valid
     */
    public boolean valid(){
        return FileHelper.isFileExists(this.DiagnosticDataPath) && FileHelper.isFileExists(this.ReportPath);
    }

    /**
     * Delete all record files
     */
    public void delete(){
        delete(this.ReportPath);
        delete(this.DiagnosticDataPath);
        delete(this.RecordPath);
    }

    /**
     * Delete single file on database record
     * @param path path to file
     */
    private void delete(String path){
        try{
            if(FileHelper.isFileExists(path)){
                new File(path).delete();
            }
        }
        catch (Exception ex){
            Log.e("Backtrace.IO", String.format("Cannot delete file: %s. Message: %s", path, ex.getMessage()));
        }
    }

    public static BacktraceDatabaseRecord readFromFile(File file){
        throw new UnsupportedOperationException();
    }
}