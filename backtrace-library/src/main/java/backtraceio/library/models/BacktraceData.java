package backtraceio.library.models;

import android.content.Context;
import backtraceio.gson.annotations.SerializedName;
import backtraceio.library.BacktraceClient;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.models.json.Annotations;
import backtraceio.library.models.json.BacktraceAttributes;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.models.json.SourceCode;
import backtraceio.library.models.json.SourceCodeData;
import backtraceio.library.models.json.ThreadData;
import backtraceio.library.models.json.ThreadInformation;
import java.util.List;
import java.util.Map;

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
    public transient BacktraceReport report; // TODO: verify if we need it

    /**
     * Application thread details
     */
    @SerializedName("threads")
    Map<String, ThreadInformation> threadInformationMap;

    /**
     * Create new instance of BacktraceData
     * @deprecated
     * This method is no longer way of creating new BacktraceData instance and will be removed soon
     */
    @Deprecated
    public BacktraceData(Context context, BacktraceReport report, Map<String, Object> clientAttributes) {
        BacktraceData obj = new Builder(report)
                .setAttributes(context, clientAttributes)
                .setSymbolication("")
                .build();

        this.uuid = obj.uuid;
        this.symbolication = obj.symbolication;
        this.timestamp = obj.timestamp;
        this.langVersion = obj.langVersion;
        this.agentVersion = obj.agentVersion;
        this.attributes = obj.attributes;
        this.mainThread = obj.mainThread;
        this.report = obj.report;
        this.classifiers = obj.classifiers;
        this.annotations = obj.annotations;
        this.sourceCode = obj.sourceCode;
        this.threadInformationMap = obj.threadInformationMap;
    }

    public BacktraceData(
            String uuid,
            String symbolication,
            long timestamp,
            String langVersion,
            String agentVersion,
            Map<String, String> attributes,
            String mainThread,
            String[] classifiers,
            BacktraceReport report,
            Map<String, Object> annotations,
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

    /**
     * Get paths to report attachments
     *
     * @deprecated
     * Please use {@link #getAttachmentPaths()} instead.
     *
     * @return paths to attachments
     */
    @Deprecated
    public List<String> getAttachments() {
        return this.getAttachmentPaths();
    }

    public Map<String, ThreadInformation> getThreadInformationMap() {
        return threadInformationMap;
    }

    public String getUuid() {
        return uuid;
    }

    public String getLang() {
        return lang;
    }

    public String getAgent() {
        return agent;
    }

    public String getSymbolication() {
        return symbolication;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getLangVersion() {
        return langVersion;
    }

    public String getAgentVersion() {
        return agentVersion;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public String getMainThread() {
        return mainThread;
    }

    public String[] getClassifiers() {
        return classifiers;
    }

    public Map<String, Object> getAnnotations() {
        return annotations;
    }

    public Map<String, SourceCode> getSourceCode() {
        return sourceCode;
    }

    public BacktraceReport getReport() {
        return report;
    }

    public static class Builder {
        private final BacktraceReport report;

        private String symbolication = "";

        private String uuid;

        private long timestamp;

        private String[] classifiers;

        private String langVersion;

        private String agentVersion;

        private Map<String, Object> annotations;
        private Map<String, SourceCode> sourceCode;
        private Map<String, ThreadInformation> threadInformationMap;
        private Map<String, String> attributes;
        private String mainThread;

        public Builder(BacktraceReport report) {
            this.report = report;

            this.setDefaultReportInformation(this.report);
            this.setDefaultThreadsInformation();
        }

        public BacktraceData build() {
            return new BacktraceData(
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
                    this.threadInformationMap);
        }

        public Builder setSymbolication(String symbolication) {
            this.symbolication = symbolication;
            return this;
        }

        /**
         * Set report information such as report identifier (UUID), timestamp, classifier
         */
        private Builder setDefaultReportInformation(BacktraceReport report) {
            this.uuid = report.uuid.toString();
            this.timestamp = report.timestamp;
            this.classifiers = report.exceptionTypeReport ? new String[] {report.classifier} : null;
            this.langVersion = System.getProperty("java.version");
            this.agentVersion = BacktraceClient.version;
            return this;
        }

        /**
         * Set information about all threads
         */
        private Builder setDefaultThreadsInformation() {
            BacktraceLogger.d(LOG_TAG, "Setting threads information");

            ThreadData threadData = new ThreadData(report.diagnosticStack);
            SourceCodeData sourceCodeData = new SourceCodeData(report.diagnosticStack);

            this.mainThread = threadData.getMainThread();
            this.threadInformationMap = threadData.threadInformation;
            this.sourceCode = sourceCodeData.data.isEmpty() ? null : sourceCodeData.data;
            return this;
        }

        public Builder setAttributes(Context context, Map<String, Object> clientAttributes) {
            BacktraceLogger.d(LOG_TAG, "Setting attributes");
            BacktraceAttributes backtraceAttributes = new BacktraceAttributes(context, this.report, clientAttributes);
            this.attributes = backtraceAttributes.attributes;

            setAnnotations(backtraceAttributes.getComplexAttributes());
            return this;
        }

        private Builder setAnnotations(Map<String, Object> complexAttributes) {
            BacktraceLogger.d(LOG_TAG, "Setting annotations");
            Object exceptionMessage = null;

            if (this.attributes != null && this.attributes.containsKey("error.message")) {
                exceptionMessage = this.attributes.get("error.message");
            }
            this.annotations = Annotations.getAnnotations(exceptionMessage, complexAttributes);
            return this;
        }
    }
}
