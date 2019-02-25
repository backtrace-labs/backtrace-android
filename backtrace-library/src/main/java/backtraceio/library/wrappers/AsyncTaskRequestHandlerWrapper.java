package backtraceio.library.wrappers;

import android.os.AsyncTask;

import backtraceio.library.events.RequestHandler;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceResult;

public class AsyncTaskRequestHandlerWrapper extends AsyncTask<Void, Void, BacktraceResult> {

    /**
     * User custom request method
     */
    private final RequestHandler requestHandler;

    private final BacktraceData data;

    public AsyncTaskRequestHandlerWrapper(RequestHandler requestHandler, BacktraceData data)
    {
        this.requestHandler = requestHandler;
        this.data = data;
    }

    protected BacktraceResult doInBackground(Void... params) {
        return this.requestHandler.onRequest(data);
    }
}