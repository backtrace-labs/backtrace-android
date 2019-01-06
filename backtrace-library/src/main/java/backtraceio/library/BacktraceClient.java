package backtraceio.library;

import android.content.Context;
import android.os.AsyncTask;

import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceResult;
import backtraceio.library.base.BacktraceBase;
import backtraceio.library.models.json.BacktraceReport;

public class BacktraceClient extends BacktraceBase {
    private BacktraceCredentials backtraceCredentials;

    public BacktraceClient(Context context, BacktraceCredentials credentials)
    {
        super(context, credentials);
    }

    public BacktraceResult send(String message)
    {
        return super.send(message);
    }

    public BacktraceResult send(Exception exception)
    {
        return super.send(exception);
    }

    public BacktraceResult send(BacktraceReport report)
    {
        return super.send(report);
    }

    public AsyncTask<Void, Void, BacktraceResult> sendAsync(String message)
    {
        return super.sendAsync(message);
    }

    public AsyncTask<Void, Void, BacktraceResult> sendAsync(Exception exception)
    {
        return super.sendAsync(exception);
    }

    public AsyncTask<Void, Void, BacktraceResult> sendAsync(BacktraceReport report)
    {
        return super.sendAsync(report);
    }
}
