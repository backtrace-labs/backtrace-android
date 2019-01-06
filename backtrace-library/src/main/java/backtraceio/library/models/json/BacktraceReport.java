package backtraceio.library.models.json;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import backtraceio.library.models.BacktraceStackFrame;
import backtraceio.library.models.BacktraceStackTrace;

/**
 * Captured application error
 */
public class BacktraceReport {

    /**
     * 16 bytes of randomness in human readable UUID format
     * server will reject request if uuid is already found
     */
    public UUID uuid = UUID.randomUUID();

    /**
     * UTC timestamp in seconds
     */
    public long timestamp = System.currentTimeMillis() / 1000;

    /**
     * Get information about report type. If value is true the BacktraceReport has an error
     */
    public Boolean exceptionTypeReport = false;

    /**
     * Get a report classification
     */
    public String classifier = "";

    /**
     * Get an report attributes
     */
    public Map<String, Object> attributes;

    /**
     * Get a custom client message
     */
    public String message;

    /**
     * Get a report exception
     */
    public Exception exception;

    /**
     * Get all paths to attachments
     */
    public List<String> attachmentPaths;

    /**
     * Current report exception stack
     */
    public ArrayList<BacktraceStackFrame> diagnosticStack;

    /**
     * Create new instance of Backtrace report to sending a report with custom client message
     *
     * @param message custom client message
     */
    public BacktraceReport(
            String message
    ) {
        this((Exception) null, null, null);
        this.message = message;
    }

    /**
     * Create new instance of Backtrace report to sending a report
     * with custom client message, attributes and attachments
     *
     * @param message         custom client message
     * @param attributes      additional information about application state
     * @param attachmentPaths path to all report attachments
     */
    public BacktraceReport(
            String message,
            Map<String, Object> attributes,
            List<String> attachmentPaths
    ) {
        this((Exception) null, attributes, attachmentPaths);
        this.message = message;
    }

    /**
     * Create new instance of Backtrace report to sending a report
     * with application exception
     *
     * @param exception current exception
     */
    public BacktraceReport(
            Exception exception) {
        this(exception, null, null);
    }

    /**
     * Create new instance of Backtrace report to sending a report
     * with application exception, attributes and attachments
     *
     * @param exception       current exception
     * @param attributes      additional information about application state
     * @param attachmentPaths path to all report attachments
     */
    public BacktraceReport(
            Exception exception,
            Map<String, Object> attributes,
            List<String> attachmentPaths) {

        this.attributes = attributes == null ? new HashMap<String, Object>() {
        } : attributes;
        this.attachmentPaths = attachmentPaths == null ? new ArrayList<String>() : attachmentPaths;
        this.exception = exception;
        this.exceptionTypeReport = exception != null;
        this.diagnosticStack = new BacktraceStackTrace(exception).getStackFrames();

        if (this.exceptionTypeReport && exception != null) {
            this.classifier = exception.getClass().getCanonicalName();
        }
    }

    /**
     * Concat two dictionaries with attributes
     *
     * @param report     current report
     * @param attributes attributes to concatenate
     * @return concatenated map of attributes from report and from passed attributes
     */
    public static Map<String, Object> concatAttributes(
            BacktraceReport report, Map<String, Object> attributes) {
        Map<String, Object> reportAttributes = report.attributes != null ? report.attributes :
                new HashMap<String, Object>();
        if (attributes == null) {
            return reportAttributes;
        }
        reportAttributes.putAll(attributes);
        return reportAttributes;
    }
}