package backtraceio.library.services;

import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.UUID;

import backtraceio.library.BacktraceCredentials;
import backtraceio.library.common.BacktraceSerializeHelper;
import backtraceio.library.events.OnServerErrorEventListener;
import backtraceio.library.events.OnServerResponseEventListener;
import backtraceio.library.events.RequestHandler;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.json.BacktraceReport;

public class BacktraceApi {
    private String serverUrl;
    private String format = "json";
    private OnServerResponseEventListener onServerResponse = null;
    private OnServerErrorEventListener onServerError = null;
    private RequestHandler requestHandler = null;

    public BacktraceApi(BacktraceCredentials credentials) {
        if (credentials == null) {
            throw new IllegalArgumentException(String.format("%s cannot be null",
                    credentials.getClass().getName()));
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

    public void setRequestHandler(RequestHandler requestHandler) {
        this.requestHandler = requestHandler;
    }

    private BacktraceResult send(UUID requestId, String json, ArrayList<String> attachments,
                                 BacktraceReport report) {
        BacktraceResult result;
        try {
            AsyncTask<Void, Void, BacktraceResult> task = sendAsync(requestId, json,
                    attachments, report);
            result = task.execute().get();
        } catch (Exception e) {
            return BacktraceResult.OnError(report, e);
        }
        return result;
    }

    private AsyncTask<Void, Void, BacktraceResult> sendAsync(UUID requestId, String json,
                                                               ArrayList<String> attachments,
                                                               BacktraceReport report) {
        return new BacktraceHttpAsyncTask(serverUrl, requestId, json, attachments, report,
                this.onServerResponse, this.onServerError).execute();
    }

    public BacktraceResult send(BacktraceData data) {
        if (requestHandler != null) {
            return requestHandler.onRequest(data);
        }
        String json = BacktraceSerializeHelper.toJson(data);
        //TODO: add attachments:
        ArrayList<String> attachments = new ArrayList<>();
        return send(UUID.randomUUID(), json, attachments, data.report);
    }

    public AsyncTask<Void, Void, BacktraceResult> sendAsync(BacktraceData data) {
        String json = BacktraceSerializeHelper.toJson(data);
        //TODO: add attachments:
        ArrayList<String> attachments = new ArrayList<>();
        return sendAsync(UUID.randomUUID(), json, attachments, data.report);
    }
}
