package backtraceio.library.models.metrics;

import backtraceio.gson.annotations.SerializedName;
import java.util.concurrent.ConcurrentLinkedDeque;

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
