package backtraceio.library.models;

import backtraceio.gson.annotations.SerializedName;

/**
 * Coroner API response
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

    public String getRxId() {
        return rxId;
    }

    public String getResponse() {
        return response;
    }
}
