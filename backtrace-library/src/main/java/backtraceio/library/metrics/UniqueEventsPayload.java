package backtraceio.library.metrics;

import com.google.gson.annotations.SerializedName;

import java.util.concurrent.ConcurrentLinkedDeque;

import backtraceio.library.models.json.BacktraceAttributes;

public final class UniqueEventsPayload extends EventsPayload<UniqueEvent> {
    @SerializedName("unique_events")
    private final ConcurrentLinkedDeque<UniqueEvent> uniqueEvents;

    protected UniqueEventsPayload(ConcurrentLinkedDeque<UniqueEvent> events, String application, String appVersion, int droppedEvents) {
        super(application, appVersion, droppedEvents);
        this.uniqueEvents = events;
    }

    @Override
    public ConcurrentLinkedDeque<UniqueEvent> getEvents() {
        return this.uniqueEvents;
    }
}
