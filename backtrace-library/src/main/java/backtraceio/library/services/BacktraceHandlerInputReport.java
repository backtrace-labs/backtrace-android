package backtraceio.library.services;

import backtraceio.library.events.OnServerErrorEventListener;
import backtraceio.library.events.OnServerResponseEventListener;
import backtraceio.library.events.RequestHandler;
import backtraceio.library.models.BacktraceData;

public class BacktraceHandlerInputReport extends BacktraceHandlerInput {

    /**
     * A message containing information about the error and the device
     */
    public BacktraceData data;
    /**
     * Event that will be executed after receiving a response from the server
     */
    OnServerResponseEventListener serverResponseEventListener;

    /**
     * Event which will be executed instead of default sending report to Backtrace server
     */
    RequestHandler requestHandler;

    /**
     * Initialize new instance of BacktraceHandlerInput
     *
     * @param data                        a message containing information about the error and the device
     * @param serverResponseEventListener event callback that will be executed after receiving a response from the server
     * @param serverErrorEventListener    event callback that will be executed after receiving an error from the server
     * @param requestHandler              event callback which will be executed instead of default sending report to Backtrace server
     */
    BacktraceHandlerInputReport(
            BacktraceData data,
            OnServerResponseEventListener serverResponseEventListener,
            OnServerErrorEventListener serverErrorEventListener,
            RequestHandler requestHandler) {
        super(serverErrorEventListener);
        this.data = data;
        this.serverResponseEventListener = serverResponseEventListener;
        this.requestHandler = requestHandler;
    }
}
