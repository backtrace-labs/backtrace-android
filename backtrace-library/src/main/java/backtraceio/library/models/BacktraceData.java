package backtraceio.library.models;

import android.content.Context;

import com.google.gson.annotations.SerializedName;

import java.util.Map;
import java.util.UUID;

import backtraceio.library.common.DeviceAttributesHelper;
import backtraceio.library.models.json.BacktraceAttributes;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.models.json.ThreadData;
import backtraceio.library.models.json.ThreadInformation;

/// <summary>
/// Serializable Backtrace API data Object
/// </summary>
public class BacktraceData {
    /// <summary>
    /// 16 bytes of randomness in human readable UUID format
    /// server will reject request if uuid is already found
    /// </summary>
    @SerializedName("uuid")
    public UUID uuid;

    /// <summary>
    /// UTC timestamp in seconds
    /// </summary>
    @SerializedName("timestamp")
    public long timestamp;

    /// <summary>
    /// Name of programming language/environment this error comes from.
    /// </summary>
    @SerializedName("lang")
    public final String lang = "java";

    /// <summary>
    /// Version of programming language/environment this error comes from.
    /// </summary>
    @SerializedName("langVersion")
    public String langVersion;

    /// <summary>
    /// Name of the client that is sending this error report.
    /// </summary>
    @SerializedName("agent")
    public final String agent = "backtrace-android";

    /// <summary>
    /// Version of the android library
    /// </summary>
    @SerializedName("agentVersion")
    public String agentVersion;

    /// <summary>
    /// Get built-in attributes
    /// </summary>
    @SerializedName("attributes")
    public Map<String, Object> attributes;

    /// <summary>
    /// Get current host environment variables and application dependencies
    /// </summary>
    //    Annotations annotations;

    /// <summary>
    /// Application thread details
    /// </summary>
    @SerializedName("threads")
    Map<String, ThreadInformation> threadInformations;

    /// <summary>
    /// Get a main thread name
    /// </summary>
    @SerializedName("mainThread")
    public String mainThread;

    /// <summary>
    /// Get a report classifiers. If user send custom message, then variable should be null
    /// </summary>
    @SerializedName("classifier")
    public String[] classifier;

    /// <summary>
    /// Current BacktraceReport
    /// </summary>
    public transient BacktraceReport report;


    public transient Context context;


    public BacktraceData(Context context, BacktraceReport report, Map<String, Object>
            clientAttributes) {
        if (report == null) {
            return;
        }
        this.context = context;
        this.report = report;
        setReportInformation();

        setThreadsInformation();
        setAttributes(clientAttributes);
        new DeviceAttributesHelper(this.context);
    }

    private void setAttributes(Map<String, Object> clientAttributes) {
        BacktraceAttributes backtraceAttributes = new BacktraceAttributes(this.context, this.report,
                clientAttributes);
        this.attributes = backtraceAttributes.attributes;
    }

    private void setReportInformation() {
        uuid = report.uuid;
        timestamp = report.timestamp;
        classifier = report.exceptionTypeReport ? new String[]{report.classifier} : null;
        // TODO:
        langVersion = System.getProperty("java.version");
        agentVersion = "0.0.0";
    }

    private void setThreadsInformation() {
        ThreadData threadData = new ThreadData(report.diagnosticStack);
        mainThread = threadData.getMainThread();
        threadInformations = threadData.threadInformation;
    }
}