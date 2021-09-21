package backtraceio.library.metrics;

import backtraceio.library.events.OnServerErrorEventListener;
import backtraceio.library.services.BacktraceHandlerInput;

/**
 * Class represents Backtrace events that will be sent to BacktraceHandlerThread
 */
public class BacktraceHandlerInputEvents extends BacktraceHandlerInput {

    /**
     * A message containing information about the error and the device
     */
    public EventsPayload payload;
    /**
     * Event that will be executed after receiving a response from the server
     */
    public EventsOnServerResponseEventListener eventsOnServerResponseEventListener;

    /**
     * Event which will be executed instead of default sending report to Backtrace server
     */
    public EventsRequestHandler eventsRequestHandler;

    /**
     * Initialize new instance of BacktraceHandlerInput
     *
     * @param payload                             a message containing information about the error and the device
     * @param eventsOnServerResponseEventListener event callback that will be executed after receiving a response from the server
     * @param serverErrorEventListener            event callback that will be executed after receiving an error from the server
     * @param eventsRequestHandler                event callback which will be executed instead of default sending report to Backtrace server
     */
    public BacktraceHandlerInputEvents(EventsPayload payload,
                                       EventsOnServerResponseEventListener eventsOnServerResponseEventListener,
                                       OnServerErrorEventListener serverErrorEventListener,
                                       EventsRequestHandler eventsRequestHandler) {
        super(serverErrorEventListener);
        this.payload = payload;
        this.eventsOnServerResponseEventListener = eventsOnServerResponseEventListener;
        this.eventsRequestHandler = eventsRequestHandler;
    }
}
