package backtraceio.library.models;

import backtraceio.library.common.serializers.SerializedName;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.models.types.BacktraceResultStatus;

/**
 * Send method result
 */
public class BacktraceApiResult {

    /**
     * Object identifier
     */
    @SerializedName("_rxid")
    public String rxId;

    /**
     * Result status eg. server error, ok
     */
    @SerializedName("response")
    public String response;

    public BacktraceApiResult(String rxId, String response) {
        this.rxId = rxId;
        this.response = response;
    }
}