package backtraceio.library.services;

import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.UUID;

import backtraceio.library.BacktraceCredentials;
import backtraceio.library.common.BacktraceSerializeHelper;
import backtraceio.library.events.OnAfterSendEventListener;
import backtraceio.library.events.OnServerErrorEventListener;
import backtraceio.library.events.OnServerResponseEventListener;
import backtraceio.library.events.RequestHandler;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.json.BacktraceReport;

/**
 * Backtrace Api class that allows to send a diagnostic data to server
 */
public class BacktraceApi {
    /**
     * URL to server
     */
    private String serverUrl;

    /**
     * Data format
     */
    private final String format = "json";

    /**
     * Event triggered when server respond to diagnostic data
     */
    private OnServerResponseEventListener onServerResponse = null;

    /**
     * Event triggered when server respond with error
     */
    private OnServerErrorEventListener onServerError = null;

    /**
     * Event triggered after sending diagnostic data to server
     */
    private OnAfterSendEventListener afterSend = null;

    /**
     * User custom request method
     */
    private RequestHandler requestHandler = null;

    /**
     * Create a new instance of Backtrace API
     *
     * @param credentials API credentials
     */
    public BacktraceApi(BacktraceCredentials credentials) {
        if (credentials == null) {
            throw new IllegalArgumentException("BacktraceCredentials cannot be null");
        }
        serverUrl = String.format("%spost?format=%s&token=%s", credentials.getEndpointUrl(),
                this.format, credentials.getSubmissionToken());
    }

    public void setOnServerResponse(OnServerResponseEventListener onServerResponse) {
        this.onServerResponse = onServerResponse;
    }

    public void setOnServerError(OnServerErrorEventListener onServerError) {
        this.onServerError = onServerError;
    }

    public void setAfterSend(OnAfterSendEventListener afterSend) {
        this.afterSend = afterSend;
    }

    public void setRequestHandler(RequestHandler requestHandler) {
        this.requestHandler = requestHandler;
    }

    private BacktraceResult send(UUID requestId, String json, ArrayList<String> attachments,
                                 BacktraceReport report) {
        BacktraceResult result;
        try {
            AsyncTask<Void, Void, BacktraceResult> task = sendAsync(requestId, json,
                    attachments, report);
            result = task.get();
        } catch (Exception e) {
            return BacktraceResult.OnError(report, e);
        }
        return result;
    }

    private AsyncTask<Void, Void, BacktraceResult> sendAsync(UUID requestId, String json,
                                                             ArrayList<String> attachments,
                                                             BacktraceReport report) {
        return new BacktraceHttpAsyncTask(serverUrl, requestId, json, attachments, report,
                this.onServerResponse, this.onServerError, this.afterSend).execute();
    }

    /**
     * Sending synchronously a diagnostic report data to Backtrace server API.
     *
     * @param data diagnostic data
     * @return server response
     */
    public BacktraceResult send(BacktraceData data) {
        if (requestHandler != null) {
            return requestHandler.onRequest(data);
        }
        String json = BacktraceSerializeHelper.toJson(data);
        ArrayList<String> attachments = new ArrayList<>(); // TODO: add attachments
        return send(UUID.randomUUID(), json, attachments, data.report);
    }


    /**
     * Sending asynchronously a diagnostic report data to Backtrace server API.
     *
     * @param data diagnostic data
     * @return AsyncTask which returns server response after execution
     */
    public AsyncTask<Void, Void, BacktraceResult> sendAsync(BacktraceData data) {
        String json = BacktraceSerializeHelper.toJson(data);
        ArrayList<String> attachments = new ArrayList<>(); // TODO: add attachments
        return sendAsync(UUID.randomUUID(), json, attachments, data.report);
    }
}
