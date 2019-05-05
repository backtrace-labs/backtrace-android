package backtraceio.library.services;

import backtraceio.library.events.OnServerErrorEventListener;
import backtraceio.library.events.OnServerResponseEventListener;
import backtraceio.library.events.RequestHandler;
import backtraceio.library.models.BacktraceData;

/**
 * Class represents the message that will be sent to BacktraceHandlerThread
 */
public class BacktraceHandlerInput {

    /**
     * A message containing information about the error and the device
     */
    public BacktraceData data;
    /**
     * Event that will be started after receiving a response from the server
     */
    OnServerResponseEventListener serverResponseEventListener;
    /**
     * Event that will be started after receiving an error from the server
     */
    OnServerErrorEventListener serverErrorEventListener;
    /**
     * Event which will be executed instead of default sending report to Backtrace server
     */
    RequestHandler requestHandler;

    /**
     * Initialize new instance of BacktraceHandlerInput
     *
     * @param data                        a message containing information about the error and the device
     * @param serverResponseEventListener event callback that will be started after receiving a response from the server
     * @param serverErrorEventListener    event callback that will be started after receiving an error from the server
     * @param requestHandler              event callback which will be executed instead of default sending report to Backtrace server
     */
    BacktraceHandlerInput(BacktraceData data,
                          OnServerResponseEventListener serverResponseEventListener,
                          OnServerErrorEventListener serverErrorEventListener,
                          RequestHandler requestHandler) {
        this.data = data;
        this.serverResponseEventListener = serverResponseEventListener;
        this.serverErrorEventListener = serverErrorEventListener;
        this.requestHandler = requestHandler;
    }
}
