package backtraceio.library.models;

import android.content.Context;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

import backtraceio.library.BacktraceClient;
import backtraceio.library.common.FileHelper;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.models.json.Annotations;
import backtraceio.library.models.json.BacktraceAttributes;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.models.json.SourceCode;
import backtraceio.library.models.json.SourceCodeData;
import backtraceio.library.models.json.ThreadData;
import backtraceio.library.models.json.ThreadInformation;

/**
 * Serializable Backtrace API data object
 */
public class BacktraceData {

    private static final transient String LOG_TAG = BacktraceData.class.getSimpleName();
    /**
     * Name of programming language/environment this error comes from.
     */
    @SerializedName("lang")
    public final String lang = "java";

    /**
     * Name of the client that is sending this error report.
     */
    @SerializedName("agent")
    public final String agent = "backtrace-android";

    /**
     * If sending a Proguard obfuscated callstack, we need
     * to set this field to "proguard" so the backend knows
     */
    @SerializedName("symbolication")
    public String symbolication;

    /**
     * 16 bytes of randomness in human readable UUID format
     * server will reject request if uuid is already found
     */
    @SerializedName("uuid")
    public String uuid;

    /**
     * UTC timestamp in seconds
     */
    @SerializedName("timestamp")
    public long timestamp;

    /**
     * Version of programming language/environment this error comes from.
     */
    @SerializedName("langVersion")
    public String langVersion;

    /**
     * Version of the android library
     */
    @SerializedName("agentVersion")
    public String agentVersion;

    /**
     * Get built-in attributes
     */
    @SerializedName("attributes")
    public Map<String, String> attributes;

    /**
     * Get a main thread name
     */
    @SerializedName("mainThread")
    public String mainThread;

    /**
     * Get a report classifiers. If user send custom message, then variable should be null
     */
    @SerializedName("classifiers")
    public String[] classifiers;

    /**
     * Current host environment variables
     */
    @SerializedName("annotations")
    public Map<String, Object> annotations;

    @SerializedName("sourceCode")
    public Map<String, SourceCode> sourceCode;

    /**
     * Current BacktraceReport
     */
    public transient BacktraceReport report;

    /**
     * Current application context
     */
    public transient Context context;

    /**
     * Application thread details
     */
    @SerializedName("threads")
    Map<String, ThreadInformation> threadInformationMap;

    /**
     * Create instance of report data
     *
     * @param context          current application context
     * @param report           current report
     * @param clientAttributes attributes which should be added to BacktraceData object
     */
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
    }

    /**
     * Get absolute paths to report attachments
     *
     * @return paths to attachments
     */
    public List<String> getAttachments() {
        return FileHelper.filterOutFiles(this.context, report.attachmentPaths);
    }


    public BacktraceReport getReport() {
        return report;
    }

    /***
     * Set annotations object
     * @param complexAttributes
     */
    private void setAnnotations(Map<String, Object> complexAttributes) {
        BacktraceLogger.d(LOG_TAG, "Setting annotations");
        Object exceptionMessage = null;

        if (this.attributes != null &&
                this.attributes.containsKey("error.message")) {
            exceptionMessage = this.attributes.get("error.message");
        }
        this.annotations = Annotations.getAnnotations(exceptionMessage, complexAttributes);
    }

    /**
     * Set attributes and add complex attributes to annotations
     *
     * @param clientAttributes
     */
    private void setAttributes(Map<String, Object> clientAttributes) {
        BacktraceLogger.d(LOG_TAG, "Setting attributes");
        BacktraceAttributes backtraceAttributes = new BacktraceAttributes(this.context, this.report,
                clientAttributes);
        this.attributes = backtraceAttributes.attributes;

        setAnnotations(backtraceAttributes.getComplexAttributes());
    }

    /**
     * Set report information such as report identifier (UUID), timestamp, classifier
     */
    private void setReportInformation() {
        BacktraceLogger.d(LOG_TAG, "Setting report information");
        uuid = report.uuid.toString();
        timestamp = report.timestamp;
        classifiers = report.exceptionTypeReport ? new String[]{report.classifier} : null;
        langVersion = System.getProperty("java.version"); //TODO: Fix problem with read Java version
        agentVersion = BacktraceClient.version;
    }

    /**
     * Set information about all threads
     */
    private void setThreadsInformation() {
        BacktraceLogger.d(LOG_TAG, "Setting threads information");
        ThreadData threadData = new ThreadData(report.diagnosticStack);
        this.mainThread = threadData.getMainThread();
        this.threadInformationMap = threadData.threadInformation;
        SourceCodeData sourceCodeData = new SourceCodeData(report.diagnosticStack);
        this.sourceCode = sourceCodeData.data.isEmpty() ? null : sourceCodeData.data;
    }
}