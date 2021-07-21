package backtraceio.library.metrics;

import com.google.gson.annotations.SerializedName;

import java.util.concurrent.ConcurrentLinkedDeque;

import backtraceio.library.models.json.BacktraceAttributes;

public final class UniqueEventsPayload extends EventsPayload {
    @SerializedName("unique_events")
    private ConcurrentLinkedDeque<Event> uniqueEvents;

    protected UniqueEventsPayload(BacktraceAttributes backtraceAttributes, ConcurrentLinkedDeque<Event> events, int droppedEvents) {
       super(backtraceAttributes, droppedEvents);
       this.uniqueEvents = events;
    }

    public ConcurrentLinkedDeque<Event> getEvents() {
        return this.uniqueEvents;
    }
}
