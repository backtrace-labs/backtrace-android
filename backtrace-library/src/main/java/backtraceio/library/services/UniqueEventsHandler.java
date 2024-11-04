package backtraceio.library.services;

import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

import backtraceio.library.common.BacktraceTimeHelper;
import backtraceio.library.interfaces.Api;
import backtraceio.library.models.metrics.EventsPayload;
import backtraceio.library.models.metrics.UniqueEvent;
import backtraceio.library.models.metrics.UniqueEventsPayload;

public class UniqueEventsHandler extends BacktraceEventsHandler<UniqueEvent> {

    private final static transient String LOG_TAG = UniqueEventsHandler.class.getSimpleName();

    private final static String urlPrefix = "unique-events";

    public UniqueEventsHandler(BacktraceMetrics backtraceMetrics,
                               Api api, final BacktraceHandlerThread backtraceHandlerThread) {
        super(backtraceMetrics, api, backtraceHandlerThread, urlPrefix);
    }

    @Override
    protected UniqueEventsPayload getEventsPayload() {
        Map<String, String> attributes = backtraceMetrics.createLocalAttributes(null);

        for (UniqueEvent event : events) {
            event.update(BacktraceTimeHelper.getTimestampSeconds(), attributes);
        }

        return new UniqueEventsPayload(events, backtraceMetrics.getApplicationName(), backtraceMetrics.getApplicationVersion());
    }

    @Override
    protected void sendEvents(ConcurrentLinkedDeque<UniqueEvent> events) {
        UniqueEventsPayload payload = getEventsPayload();
        api.sendEventsPayload(payload);
    }

    @Override
    protected void sendEventsPayload(EventsPayload<UniqueEvent> payload) {
        api.sendEventsPayload((UniqueEventsPayload) payload);
    }
}
