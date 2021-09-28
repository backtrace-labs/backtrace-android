package backtraceio.library.metrics;

import android.content.Context;

import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

import backtraceio.library.interfaces.Api;
import backtraceio.library.models.BacktraceMetricsSettings;
import backtraceio.library.services.BacktraceHandlerThread;

public class SummedEventsHandler extends BacktraceEventsHandler<SummedEvent> {

    private final static transient String LOG_TAG = SummedEventsHandler.class.getSimpleName();

    private final static String urlPrefix = "summed-events";

    public SummedEventsHandler(Context context, Map<String, Object> customAttributes,
                               Api api, final BacktraceHandlerThread backtraceHandlerThread, BacktraceMetricsSettings settings) {
        super(context, customAttributes, api, backtraceHandlerThread, urlPrefix, settings);
    }

    @Override
    public void sendStartupEvent(String eventName) {
        events.addLast(new SummedEvent(eventName));
        send();
        events.clear();
    }

    @Override
    protected SummedEventsPayload getEventsPayload() {
        Map<String, Object> attributes = getAttributes();

        ConcurrentLinkedDeque<SummedEvent> eventsCopy = new ConcurrentLinkedDeque<>();

        for (SummedEvent event : events) {
            event.addAttributes(attributes);
            eventsCopy.addLast(new SummedEvent((SummedEvent) event));
        }
        events.clear();

        SummedEventsPayload payload = new SummedEventsPayload(eventsCopy, application, appVersion);

        return payload;
    }

    @Override
    protected void sendEvents(ConcurrentLinkedDeque<SummedEvent> events) {
        SummedEventsPayload payload = getEventsPayload();
        api.sendEventsPayload(payload);
    }

    @Override
    protected void sendEventsPayload(EventsPayload<SummedEvent> payload) {
        api.sendEventsPayload((SummedEventsPayload) payload);
    }

    @Override
    protected void onMaximumAttemptsReached(ConcurrentLinkedDeque<SummedEvent> events) {
        if (this.events.size() + events.size() < getMaximumNumberOfEvents()) {
            for (SummedEvent event : events) {
                this.events.addFirst(event);
            }
        }
    }
}
