package backtraceio.library.services;

import backtraceio.library.events.OnServerErrorEventListener;

/**
 * Class represents a Backtrace report that will be sent to BacktraceHandlerThread
 */
public abstract class BacktraceHandlerInput {

    /**
     * Event that will be executed after receiving an error from the server
     */
    public OnServerErrorEventListener serverErrorEventListener;

    /**
     * Initialize new instance of BacktraceHandlerInput
     *
     * @param serverErrorEventListener    event callback that will be executed after receiving an error from the server
     */
    protected BacktraceHandlerInput(OnServerErrorEventListener serverErrorEventListener) {
        this.serverErrorEventListener = serverErrorEventListener;
    }
}
