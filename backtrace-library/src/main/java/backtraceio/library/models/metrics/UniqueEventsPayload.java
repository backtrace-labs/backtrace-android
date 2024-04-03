package backtraceio.library.models.metrics;

import java.util.concurrent.ConcurrentLinkedDeque;

import backtraceio.library.common.serializers.SerializedName;

public final class UniqueEventsPayload extends EventsPayload<UniqueEvent> {
    @SerializedName("unique_events")
    private final ConcurrentLinkedDeque<UniqueEvent> uniqueEvents;

    public UniqueEventsPayload(ConcurrentLinkedDeque<UniqueEvent> events, String application, String appVersion) {
        super(application, appVersion);
        this.uniqueEvents = events;
    }

    @Override
    public ConcurrentLinkedDeque<UniqueEvent> getEvents() {
        return this.uniqueEvents;
    }
}
