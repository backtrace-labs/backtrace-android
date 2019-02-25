package backtraceio.library.models;

import android.content.Context;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

import backtraceio.library.BuildConfig;
import backtraceio.library.common.DeviceAttributesHelper;
import backtraceio.library.common.FileHelper;
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
     * Name of programming language/environment this error comes from.
     */
    @SerializedName("lang")
    public final String lang = "java";

    /**
     * Version of programming language/environment this error comes from.
     */
    @SerializedName("langVersion")
    public String langVersion;

    /**
     * Name of the client that is sending this error report.
     */
    @SerializedName("agent")
    public final String agent = "backtrace-android";

    /**
     * Version of the android library
     */
    @SerializedName("agentVersion")
    public String agentVersion;

    /**
     * Get built-in attributes
     */
    @SerializedName("attributes")
    public Map<String, Object> attributes;

    /**
     * Application thread details
     */
    @SerializedName("threads")
    Map<String, ThreadInformation> threadInformationMap;

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
    public Annotations annotations;


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
        this.annotations = new Annotations();

        setReportInformation();

        setThreadsInformation();
        setAttributes(clientAttributes);
    }

    /**
     * Get absolute paths to report attachments
     * @return paths to attachments
     */
    public List<String> getAttachments() {
        return FileHelper.filterOutFiles(this.context, report.attachmentPaths);
    }

    /**
     * Set attributes and add complex attributes to annotations
     * @param clientAttributes
     */
    private void setAttributes(Map<String, Object> clientAttributes) {
        BacktraceAttributes backtraceAttributes = new BacktraceAttributes(this.context, this.report,
                clientAttributes);
        this.attributes = backtraceAttributes.attributes;

        DeviceAttributesHelper deviceAttributesHelper = new DeviceAttributesHelper(this.context);
        this.attributes.putAll(deviceAttributesHelper.getDeviceAttributes());

        if(this.annotations != null)
        {
            this.annotations.addComplexAttributes(backtraceAttributes.getComplexAttributes());
        }
    }

    /**
     * Set report information such as report identifier (UUID), timestamp, classifier
     */
    private void setReportInformation() {
        uuid = report.uuid.toString();
        timestamp = report.timestamp;
        classifiers = report.exceptionTypeReport ? new String[]{report.classifier} : null;
        langVersion = System.getProperty("java.version"); //TODO: Fix problem with read Java version
        agentVersion = BuildConfig.VERSION_NAME;
    }

    /**
     * Set information about all threads
     */
    private void setThreadsInformation() {
        ThreadData threadData = new ThreadData(report.diagnosticStack);
        this.mainThread = threadData.getMainThread();
        this.threadInformationMap = threadData.threadInformation;
        SourceCodeData sourceCodeData = new SourceCodeData(report.diagnosticStack);
        this.sourceCode = sourceCodeData.data.isEmpty() ? null : sourceCodeData.data;
    }
}