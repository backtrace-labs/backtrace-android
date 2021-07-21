package backtraceio.library.metrics;

import android.content.Context;

import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

import backtraceio.library.common.DeviceAttributesHelper;
import backtraceio.library.interfaces.Api;
import backtraceio.library.logger.BacktraceLogger;
import backtraceio.library.models.json.BacktraceAttributes;
import backtraceio.library.services.BacktraceHandlerThread;

public class SummedEventsHandler extends BacktraceEventsHandler {

    private final static transient String LOG_TAG = SummedEventsHandler.class.getSimpleName();

    private final static String urlPrefix = "summed-events";

    public SummedEventsHandler(Context context, Map<String, Object> customAttributes,
                               final BacktraceHandlerThread backtraceHandlerThread,
                               String universeName, String token,
                               Api api, final long timeIntervalMillis, int timeBetweenRetriesMillis) {
        super(context, customAttributes, backtraceHandlerThread, urlPrefix, universeName, token, api, timeIntervalMillis, timeBetweenRetriesMillis);
    }

    @Override
    public void sendStartupEvent(String eventName) {
        events.addLast(new SummedEvent(eventName));
        send();
        events.clear();
    }

    @Override
    protected EventsPayload getEventsPayload() {
        BacktraceAttributes backtraceAttributes = new BacktraceAttributes(context, null, customAttributes);
        Map<String, Object> attributes = backtraceAttributes.getAllBacktraceAttributes();

        DeviceAttributesHelper deviceAttributesHelper = new DeviceAttributesHelper(context);
        attributes.putAll(deviceAttributesHelper.getDeviceAttributes());

        ConcurrentLinkedDeque<Event> eventsCopy = new ConcurrentLinkedDeque<>();

        for (Event event : events) {
            if (!(event instanceof SummedEvent)) {
                BacktraceLogger.e(LOG_TAG, "Cannot convert stored event to SummedEvent");
                continue;
            }

            ((SummedEvent) event).addAttributes(attributes);
            eventsCopy.addLast(new SummedEvent((SummedEvent) event));
        }
        events.clear();

        SummedEventsPayload payload = new SummedEventsPayload(backtraceAttributes, eventsCopy, numRetries);

        return payload;
    }

    @Override
    protected void onMaximumAttemptsReached(ConcurrentLinkedDeque<Event> events) {
        if (this.events.size() + events.size() < getMaxNumEvents()) {
            for (Event event : events) {
                this.events.addFirst(event);
            }
        }
    }
}
