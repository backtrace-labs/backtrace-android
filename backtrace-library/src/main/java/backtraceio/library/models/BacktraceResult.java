package backtraceio.library.models;

import com.google.gson.annotations.SerializedName;

import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.models.types.BacktraceResultStatus;

public class BacktraceResult {

    public String getRxId() {
        return rxId;
    }

    public void setRxId(String rxId) {
        this.rxId = rxId;
    }

    @SerializedName("_rxid")
    public String rxId;

    public String message;
    public BacktraceResultStatus status  = BacktraceResultStatus.Ok;

    public void setBacktraceReport(BacktraceReport backtraceReport) {
        this.backtraceReport = backtraceReport;
    }

    private BacktraceReport backtraceReport;

    public BacktraceResult(){

    }

    public BacktraceResult(BacktraceReport report, String message, BacktraceResultStatus status){
        this.backtraceReport = report;
        this.message = message;
        this.status = status;
    }

    public static BacktraceResult OnError(BacktraceReport report, Exception exception)
    {
        return new BacktraceResult(
            report, exception.getMessage(),
            BacktraceResultStatus.ServerError);
    }
}
