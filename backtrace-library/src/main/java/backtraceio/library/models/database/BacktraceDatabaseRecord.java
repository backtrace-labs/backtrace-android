package backtraceio.library.models.database;

import com.google.gson.annotations.SerializedName;

import java.util.UUID;

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
}