package backtraceio.library.models.base;

import android.content.Context;
import android.os.AsyncTask;

import java.util.function.Function;

import backtraceio.library.BacktraceCredentials;
import backtraceio.library.events.OnAfterSendEventListener;
import backtraceio.library.events.OnBeforeSendEventListener;
import backtraceio.library.events.OnServerErrorEventListener;
import backtraceio.library.events.OnServerResponseEventListener;
import backtraceio.library.interfaces.IBacktraceClient;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.models.json.BacktraceReport;
import backtraceio.library.services.BacktraceApi;

public class BacktraceBase implements IBacktraceClient {

    protected BacktraceApi backtraceApi;
    protected BacktraceCredentials credentials;
    protected Context context;
    private OnBeforeSendEventListener beforeSendEventListener = null;
    private OnAfterSendEventListener afterSendEventListener = null; // TODO: handle

    public BacktraceBase(Context context, BacktraceCredentials credentials)
    {
        this.context = context;
        this.credentials = credentials;
        this.backtraceApi = new BacktraceApi(credentials);
    }

    public void setOnBeforeSendEventListener(OnBeforeSendEventListener eventListener){
        this.beforeSendEventListener = eventListener;
    }

    public void setOnAfterSendEventListener(OnAfterSendEventListener eventListener){
        this.afterSendEventListener = eventListener;
    }

    public void setOnServerResponseEventListner(OnServerResponseEventListener eventListner){
        this.backtraceApi.setOnServerResponse(eventListner);
    }

    public void setOnServerErrorEventListner(OnServerErrorEventListener eventListner){
        this.backtraceApi.setOnServerError(eventListner);
    }

    public BacktraceResult send(String message)
    {
        BacktraceReport backtraceReport = new BacktraceReport(message);
        return this.send(backtraceReport);
    }

    public BacktraceResult send(Exception e)
    {
        BacktraceReport backtraceReport = new BacktraceReport(e);
        return this.send(backtraceReport);
    }

    public BacktraceResult send(BacktraceReport report)
    {
        BacktraceData backtraceData = new BacktraceData(this.context, report, null);
        if(this.beforeSendEventListener != null)
        {
            backtraceData = this.beforeSendEventListener.onEvent(backtraceData);
        }
        return this.backtraceApi.send(backtraceData);
    }

    public AsyncTask<Void, Void, BacktraceResult> sendAsync(String message)
    {
        BacktraceReport backtraceReport = new BacktraceReport(message);
        return this.sendAsync(backtraceReport);
    }

    public AsyncTask<Void, Void, BacktraceResult> sendAsync(Exception e)
    {
        BacktraceReport backtraceReport = new BacktraceReport(e);
        return this.sendAsync(backtraceReport);
    }

    public AsyncTask<Void, Void, BacktraceResult> sendAsync(BacktraceReport report)
    {
        BacktraceData backtraceData = new BacktraceData(this.context, report, null);
        if(this.beforeSendEventListener != null)
        {
            backtraceData = this.beforeSendEventListener.onEvent(backtraceData);
        }
        return this.backtraceApi.sendAsync(backtraceData);
    }
}
