package backtraceio.library.models.json;

import android.content.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import backtraceio.library.common.BacktraceTimeHelper;
import backtraceio.library.common.CollectionUtils;
import backtraceio.library.models.BacktraceAttributeConsts;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceStackFrame;
import backtraceio.library.models.BacktraceStackTrace;

/**
 * Captured application error
 */
public class BacktraceReport {

    /**
     * Reference to the original exception. This field is internally used by the library.
     */
    private final transient Throwable originalException;
    /**
     * 16 bytes of randomness in human readable UUID format
     * server will reject request if uuid is already found
     */
    public UUID uuid = UUID.randomUUID();
    /**
     * UTC timestamp in seconds
     */
    public long timestamp = BacktraceTimeHelper.getTimestampSeconds();
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
    public Map<String, Object> attributes = new HashMap<>();
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
    public List<String> attachmentPaths = new ArrayList<>();
    /**
     * Current report exception stack
     */
    public List<BacktraceStackFrame> diagnosticStack;

    /**
     * Create new instance of Backtrace report to send a report with custom client message
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
     * Create new instance of Backtrace report to send a report
     * with custom client message and attributes
     *
     * @param message    custom client message
     * @param attributes additional information about application state
     */
    public BacktraceReport(
            String message,
            Map<String, Object> attributes
    ) {
        this((Exception) null, attributes, null);
        this.message = message;
    }

    /**
     * Create new instance of Backtrace report to send a report
     * with custom client message, attributes and attachments
     *
     * @param message         custom client message
     * @param attachmentPaths path to all report attachments
     */
    public BacktraceReport(
            String message,
            List<String> attachmentPaths
    ) {
        this(message, null, attachmentPaths);
    }


    /**
     * Create new instance of Backtrace report to send a report
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
     * Create new instance of Backtrace report to send a report
     * with application exception
     *
     * @param exception current exception
     */
    public BacktraceReport(
            Throwable exception) {
        this(exception, null, null);
    }

    /**
     * Create new instance of Backtrace report to send a report
     * with application exception and attributes
     *
     * @param exception  current exception
     * @param attributes additional information about application state
     */
    public BacktraceReport(
            Throwable exception,
            Map<String, Object> attributes) {
        this(exception, attributes, null);
    }

    /**
     * Create new instance of Backtrace report to send a report
     * with application exception, attributes and attachments
     *
     * @param exception       current exception
     * @param attachmentPaths path to all report attachments
     */
    public BacktraceReport(
            Exception exception,
            List<String> attachmentPaths) {
        this(exception, null, attachmentPaths);
    }

    /**
     * Create new instance of Backtrace report to send a report
     * with application exception, attributes and attachments
     *
     * @param throwable       current throwable
     * @param attributes      additional information about application state
     * @param attachmentPaths path to all report attachments
     */
    public BacktraceReport(Throwable throwable, Map<String, Object> attributes, List<String> attachmentPaths) {
        this.attributes = CollectionUtils.copyMap(attributes);
        this.attachmentPaths = CollectionUtils.copyList(attachmentPaths);
        this.originalException = throwable;

        if (throwable != null) {
            this.exception = this.prepareException(throwable);
            this.classifier = this.getExceptionClassifier(throwable);
            this.exceptionTypeReport = true;
        }
        this.diagnosticStack = new BacktraceStackTrace(exception).getStackFrames();

        this.setDefaultErrorTypeAttribute();
    }

    public BacktraceReport(UUID uuid, long timestamp,
                           boolean exceptionTypeReport, String classifier,
                           Map<String, Object> attributes,
                           String message, Exception exception,
                           List<String> attachmentPaths,
                           List<BacktraceStackFrame> diagnosticStack) {
        this.uuid = uuid;
        this.timestamp = timestamp;
        this.exceptionTypeReport = exceptionTypeReport;
        this.classifier = classifier;
        this.attributes = attributes;
        this.message = message;
        this.originalException = exception;
        this.exception = exception;
        this.attachmentPaths = attachmentPaths;
        this.diagnosticStack = diagnosticStack;
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
                new HashMap<>();
        if (attributes == null) {
            return reportAttributes;
        }
        reportAttributes.putAll(attributes);
        return reportAttributes;
    }

    /**
     * Returns report exception - original exception if possible or serializable exception if
     * the report was loaded from JSON.
     *
     * @return throwable object
     */
    public Throwable getException() {
        return this.originalException != null ? this.originalException : this.exception;
    }

    private String getExceptionClassifier(Throwable exception) {
        return exception.getClass().getCanonicalName();
    }

    /**
     * To avoid serialization issues with custom exceptions, our goal is to always
     * prepare exception in a way potential serialization won't break it
     *
     * @param exception captured client-side exception
     */
    private Exception prepareException(Throwable exception) {
        if (exception == null) {
            return null;
        }
        Exception reportException = new Exception(exception.getMessage());
        reportException.setStackTrace(exception.getStackTrace());

        return reportException;
    }

    /**
     * Sets error.type attribute depends on the type of the report
     */
    private void setDefaultErrorTypeAttribute() {
        if (attributes.containsKey(BacktraceAttributeConsts.ErrorType)) {
            // error type already set
            return;
        }

        attributes.put(
                BacktraceAttributeConsts.ErrorType,
                this.exceptionTypeReport
                        ? BacktraceAttributeConsts.HandledExceptionAttributeType
                        : BacktraceAttributeConsts.MessageAttributeType);
    }

    public BacktraceData toBacktraceData(Context context, Map<String, Object> clientAttributes) {
        return toBacktraceData(context, clientAttributes, false);
    }

    public BacktraceData toBacktraceData(Context context, Map<String, Object> clientAttributes, boolean isProguardEnabled) {
        final String symbolication = isProguardEnabled ? "proguard" : null;
        return new BacktraceData.Builder(this).setAttributes(context, clientAttributes).setSymbolication(symbolication).build();
    }
}
