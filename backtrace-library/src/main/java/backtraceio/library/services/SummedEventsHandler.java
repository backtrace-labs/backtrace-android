package backtraceio.library.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

import backtraceio.library.interfaces.Api;
import backtraceio.library.models.metrics.EventsPayload;
import backtraceio.library.models.metrics.SummedEvent;
import backtraceio.library.models.metrics.SummedEventsPayload;

public class SummedEventsHandler extends BacktraceEventsHandler<SummedEvent> {

    private final static transient String LOG_TAG = SummedEventsHandler.class.getSimpleName();

    private final static String urlPrefix = "summed-events";

    public SummedEventsHandler(BacktraceMetrics backtraceMetrics,
                               Api api, final BacktraceHandlerThread backtraceHandlerThread) {
        super(backtraceMetrics, api, backtraceHandlerThread, urlPrefix);
    }

    @Override
    protected SummedEventsPayload getEventsPayload() {
        Map<String, String> attributes = backtraceMetrics.createLocalAttributes(null);
        for (SummedEvent event: events) {
            event.addAttributes(attributes);
        }
        SummedEventsPayload payload = new SummedEventsPayload(
                new ConcurrentLinkedDeque<>(events),
                backtraceMetrics.getApplicationName(),
                backtraceMetrics.getApplicationVersion());
        events.clear();
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
