package backtraceio.library.models;

import com.google.gson.annotations.SerializedName;

import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.models.types.BacktraceResultStatus;

/**
 * Send method result
 */
public class BacktraceResult {

    /**
     * Object identifier
     */
    @SerializedName("_rxid")
    @SuppressWarnings({"UnusedDeclaration"})
    public String rxId;

    /**
     * Message
     */
    public String message;

    /**
     * Result status eg. server error, ok
     */
    public BacktraceResultStatus status = BacktraceResultStatus.Ok;

    /**
     * Current report
     */
    private BacktraceReport backtraceReport;

    /**
     * Create new instance of BacktraceResult
     */
    public BacktraceResult() {

    }

    /**
     * Create new instance of BacktraceResult
     *
     * @param report  executed report
     * @param message message
     * @param status  result status eg. ok, server error
     */
    public BacktraceResult(BacktraceReport report, String message, BacktraceResultStatus status) {
        setBacktraceReport(report);
        this.message = message;
        this.status = status;
    }

    /**
     * Set result when error occurs while sending data to API
     *
     * @param report    executed report
     * @param exception current exception
     * @return BacktraceResult with exception information
     */
    public static BacktraceResult OnError(BacktraceReport report, Exception exception) {
        return new BacktraceResult(
                report, exception.getMessage(),
                BacktraceResultStatus.ServerError);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public BacktraceReport getBacktraceReport() {
        return backtraceReport;
    }

    public void setBacktraceReport(BacktraceReport backtraceReport) {
        this.backtraceReport = backtraceReport;
    }
}