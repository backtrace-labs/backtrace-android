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
        Map<String, Object> attributes = backtraceMetrics.createLocalAttributes(null);
        String application = attributes.get("application").toString();
        String appVersion = attributes.get("application.version").toString();

        for (UniqueEvent event : events) {
            event.update(BacktraceTimeHelper.getTimestampSeconds(), attributes);
        }

        UniqueEventsPayload payload = new UniqueEventsPayload(events, application, appVersion);
        return payload;
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
