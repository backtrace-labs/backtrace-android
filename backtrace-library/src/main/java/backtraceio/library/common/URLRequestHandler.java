package backtraceio.library.common;

import backtraceio.library.BacktraceCredentials;
import backtraceio.library.events.RequestHandler;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.BacktraceNativeData;
import backtraceio.library.models.BacktraceResult;

public abstract class URLRequestHandler implements RequestHandler {
    public final String jsonURL;
    public final String minidumpURL;

    public URLRequestHandler(BacktraceCredentials credentials) {
        jsonURL = credentials.getSubmissionUrl().toString();
        minidumpURL = credentials.getMinidumpSubmissionUrl().toString();
    }

    @Override
    abstract public BacktraceResult onRequest(BacktraceData data);

    @Override
    abstract public BacktraceResult onNativeRequest(BacktraceNativeData data);
}
