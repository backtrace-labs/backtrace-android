package backtraceio.library.models;

import android.content.Context;

import java.util.List;
import java.util.Map;

import backtraceio.library.BacktraceClient;
import com.google.gson.annotations.SerializedName;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.models.json.Annotations;
import backtraceio.library.models.json.BacktraceAttributes;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.models.json.SourceCode;
import backtraceio.library.models.json.SourceCodeData;
import backtraceio.library.models.json.ThreadData;
import backtraceio.library.models.json.ThreadInformation;


// TODO: replace direct access with getters

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
    public transient BacktraceReport report; // Think if we need it

    /**
     * Application thread details
     */
    @SerializedName("threads")
    Map<String, ThreadInformation> threadInformationMap;


    public BacktraceData() {

    }

    public BacktraceData(String uuid, String symbolication, long timestamp, String langVersion,
                         String agentVersion, Map<String, String> attributes, String mainThread,
                         String[] classifiers, BacktraceReport report, Map<String, Object> annotations,
                         Map<String, SourceCode> sourceCode,
                         Map<String, ThreadInformation> threadInformationMap) {
        this.uuid = uuid;
        this.symbolication = symbolication;
        this.timestamp = timestamp;
        this.langVersion = langVersion;
        this.agentVersion = agentVersion;
        this.attributes = attributes;
        this.mainThread = mainThread;
        this.report = report;
        this.classifiers = classifiers;
        this.annotations = annotations;
        this.sourceCode = sourceCode;
        this.threadInformationMap = threadInformationMap;
    }

    /**
     * Get absolute paths to report attachments
     *
     * @return paths to attachments
     */
    public List<String> getAttachmentPaths() {
        return report.attachmentPaths;
    }

    public Map<String, ThreadInformation> getThreadInformationMap() {
        return threadInformationMap;
    }

    public static class Builder {
        final BacktraceReport report;

        final String symbolication;

        String uuid;

        long timestamp;

        String[] classifiers;

        String langVersion;

        String agentVersion;

        Map<String, Object> annotations;
        Map<String, SourceCode> sourceCode;
        Map<String, ThreadInformation> threadInformationMap;
        Map<String, String> attributes;
        String mainThread;

        public Builder(Context context, BacktraceReport report, Map<String, Object>
                clientAttributes) {
            this(context, report, "", clientAttributes);
        }
        public Builder(Context context, BacktraceReport report, String symbolication, Map<String, Object>
                clientAttributes) {
            this.report = report;
            this.symbolication = symbolication;

            this.setDefaultReportInformation(this.report);
            this.setDefaultThreadsInformation();
            this.setAttributes(context, clientAttributes);
        }

        public BacktraceData build() {
            BacktraceData backtraceData = new BacktraceData(
                    this.uuid,
                    this.symbolication,
                    this.timestamp,
                    this.langVersion,
                    this.agentVersion,
                    this.attributes,
                    this.mainThread,
                    this.classifiers,
                    this.report,
                    this.annotations,
                    this.sourceCode,
                    this.threadInformationMap
            );

            return backtraceData;
        }

        /**
         * Set report information such as report identifier (UUID), timestamp, classifier
         */
        private void setDefaultReportInformation(BacktraceReport report) {
            this.uuid = report.uuid.toString();
            this.timestamp = report.timestamp;
            this.classifiers = report.exceptionTypeReport ? new String[]{report.classifier} : null;
            this.langVersion = System.getProperty("java.version");
            this.agentVersion = BacktraceClient.version;
        }

        /**
        * Set information about all threads
        */
        private void setDefaultThreadsInformation() {
            BacktraceLogger.d(LOG_TAG, "Setting threads information");

            ThreadData threadData = new ThreadData(report.diagnosticStack);
            SourceCodeData sourceCodeData = new SourceCodeData(report.diagnosticStack);

            this.mainThread = threadData.getMainThread();
            this.threadInformationMap = threadData.threadInformation;
            this.sourceCode = sourceCodeData.data.isEmpty() ? null : sourceCodeData.data;
        }

        public void setAttributes(Context context, Map<String, Object> clientAttributes) {
            BacktraceLogger.d(LOG_TAG, "Setting attributes");
            BacktraceAttributes backtraceAttributes = new BacktraceAttributes(
                    context,
                    this.report,
                    clientAttributes);
            this.attributes = backtraceAttributes.attributes;

            setAnnotations(backtraceAttributes.getComplexAttributes());
        }

        private void setAnnotations(Map<String, Object> complexAttributes) {
            BacktraceLogger.d(LOG_TAG, "Setting annotations");
            Object exceptionMessage = null;

            if (this.attributes != null &&
                    this.attributes.containsKey("error.message")) {
                exceptionMessage = this.attributes.get("error.message");
            }
            this.annotations = Annotations.getAnnotations(exceptionMessage, complexAttributes);
        }
    }
}