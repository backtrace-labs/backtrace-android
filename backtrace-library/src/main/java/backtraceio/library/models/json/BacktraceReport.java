package backtraceio.library.models.json;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BacktraceReport {

    /// <summary>
    /// Fingerprint
    /// </summary>
    public String Fingerprint;

    /// <summary>
    /// Factor
    /// </summary>
    public String Factor;

    /// <summary>
    /// 16 bytes of randomness in human readable UUID format
    /// server will reject request if uuid is already found
    /// </summary>s
    public UUID Uuid = UUID.randomUUID();

    /// <summary>
    /// UTC timestamp in seconds
    /// </summary>
    public long Timestamp = System.currentTimeMillis() / 1000;

    /// <summary>
    /// Get information aboout report type. If value is true the BacktraceReport has an error
    // information
    /// </summary>

    public Boolean ExceptionTypeReport = false;

    /// <summary>
    /// Get a report classification
    /// </summary>
    public String Classifier = "";

    /// <summary>
    /// Get an report attributes
    /// </summary>
    public Map<String, Object> Attributes;

    /// <summary>
    /// Get a custom client message
    /// </summary>
    public String Message;

    /// <summary>
    /// Get a report exception
    /// </summary>
    public Exception Exception;

    /// <summary>
    /// Get all paths to attachments
    /// </summary>
    public List<String> AttachmentPaths;

    public BacktraceReport(
            String message
    ) {
        this((Exception) null, null, null);
        Message = message;
    }

    public BacktraceReport(
            String message,
            Map<String, Object> attributes,
            List<String> attachmentPaths
    ) {
        this((Exception) null, attributes, attachmentPaths);
        Message = message;
    }


    public BacktraceReport(
            Exception exception) {
        this(exception, null, null);
        //classifier = ExceptionTypeReport ? exception.GetType().Name : String.Empty;
    }

    public BacktraceReport(
            Exception exception,
            Map<String, Object> attributes,
            List<String> attachmentPaths) {
        Attributes = attributes == null ? new HashMap<String, Object>() {
        } : attributes;
        AttachmentPaths = attachmentPaths == null ? new ArrayList<String>() : attachmentPaths;
        Exception = exception;
        ExceptionTypeReport = exception != null;
        Classifier = ExceptionTypeReport ? exception.getClass().getCanonicalName() : "";
    }

    public static Map<String, Object> concatAttributes(
            BacktraceReport report, Map<String, Object> attributes) {
        Map<String, Object> reportAttributes = report.Attributes != null ? report.Attributes :
                new HashMap<String, Object>();
        if (attributes == null) {
            return reportAttributes;
        }
        reportAttributes.putAll(attributes);
        return reportAttributes;
    }
}