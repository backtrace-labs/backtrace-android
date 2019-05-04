package backtraceio.library.services;

import java.util.List;

import backtraceio.library.events.OnServerErrorEventListener;
import backtraceio.library.events.OnServerResponseEventListener;
import backtraceio.library.events.RequestHandler;
import backtraceio.library.models.BacktraceData;
import backtraceio.library.models.json.BacktraceReport;

public class BacktraceHandlerInput {
    public BacktraceData data;
    public OnServerResponseEventListener serverResponseEventListener;
    public OnServerErrorEventListener serverErrorEventListener;
    public RequestHandler requestHandler;

    public BacktraceHandlerInput(BacktraceData data,
                                 OnServerResponseEventListener serverResponseEventListener,
                                 OnServerErrorEventListener serverErrorEventListener,
                                 RequestHandler requestHandler) {
        this.data = data;
        this.serverResponseEventListener = serverResponseEventListener;
        this.serverErrorEventListener = serverErrorEventListener;
        this.requestHandler = requestHandler;
    }
}
