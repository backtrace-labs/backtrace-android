package backtraceio.library.models.json;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import backtraceio.library.models.BacktraceStackFrame;
import backtraceio.library.models.BacktraceStackTrace;

public class BacktraceReport {


    /// <summary>
    /// 16 bytes of randomness in human readable UUID format
    /// server will reject request if uuid is already found
    /// </summary>s
    public UUID uuid = UUID.randomUUID();

    /// <summary>
    /// UTC timestamp in seconds
    /// </summary>
    public long timestamp = System.currentTimeMillis() / 1000;

    /// <summary>
    /// Get information aboout report type. If value is true the BacktraceReport has an error
    // information
    /// </summary>

    public Boolean exceptionTypeReport = false;

    /// <summary>
    /// Get a report classification
    /// </summary>
    public String classifier = "";

    /// <summary>
    /// Get an report attributes
    /// </summary>
    public Map<String, Object> attributes;

    /// <summary>
    /// Get a custom client message
    /// </summary>
    public String message;

    /// <summary>
    /// Get a report exception
    /// </summary>
    public Exception exception;

    /// <summary>
    /// Get all paths to attachments
    /// </summary>
    public List<String> attachmentPaths;

    /// <summary>
    /// Current report exception stack
    /// </summary>
    @SerializedName("diagnosticStack")
    public ArrayList<BacktraceStackFrame> diagnosticStack;

    public BacktraceReport(
            String message
    ) {
        this((Exception) null, null, null);
        this.message = message;
    }

    public BacktraceReport(
            String message,
            Map<String, Object> attributes,
            List<String> attachmentPaths
    ) {
        this((Exception) null, attributes, attachmentPaths);
        this.message = message;
    }


    public BacktraceReport(
            Exception exception) {
        this(exception, null, null);
        //classifier = exceptionTypeReport ? exception.GetType().Name : String.Empty;
    }

    public BacktraceReport(
            Exception exception,
            Map<String, Object> attributes,
            List<String> attachmentPaths) {
        this.attributes = attributes == null ? new HashMap<String, Object>() {
        } : attributes;
        this.attachmentPaths = attachmentPaths == null ? new ArrayList<String>() : attachmentPaths;
        this.exception = exception;
        exceptionTypeReport = exception != null;
        this.diagnosticStack = new BacktraceStackTrace(exception).getStackFrames();
        classifier = exceptionTypeReport ? exception.getClass().getCanonicalName() : "";
    }

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